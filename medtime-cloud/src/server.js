import crypto from "node:crypto";
import express from "express";
import cors from "cors";
import helmet from "helmet";
import rateLimit from "express-rate-limit";
import pg from "pg";
import "dotenv/config";

const { Pool } = pg;
const app = express();
const port = Number(process.env.PORT || 10000);

if (!process.env.DATABASE_URL) {
  throw new Error("DATABASE_URL is required");
}

const databaseUrl = process.env.DATABASE_URL;
const useSsl =
  process.env.PGSSL === "require" ||
  databaseUrl.includes("sslmode=require");

const pool = new Pool({
  connectionString: databaseUrl,
  ssl: useSsl ? { rejectUnauthorized: false } : false,
  max: 10
});

app.disable("x-powered-by");
app.use(helmet());
app.use(cors({ origin: false }));
app.use(express.json({ limit: "512kb" }));
app.use(rateLimit({
  windowMs: 60_000,
  limit: 120,
  standardHeaders: "draft-7",
  legacyHeaders: false
}));

const hash = (value) =>
  crypto.createHash("sha256").update(value, "utf8").digest("hex");

const token = () => crypto.randomBytes(32).toString("base64url");

const bearer = (req) => {
  const header = req.get("authorization") || "";
  return header.startsWith("Bearer ") ? header.slice(7).trim() : "";
};

async function initDb() {
  await pool.query(
    "CREATE TABLE IF NOT EXISTS medtime_shares (" +
    "share_id UUID PRIMARY KEY," +
    "owner_id TEXT NOT NULL," +
    "owner_name TEXT NOT NULL," +
    "owner_token_hash TEXT NOT NULL," +
    "viewer_token_hash TEXT NOT NULL," +
    "medicines JSONB NOT NULL DEFAULT '[]'::jsonb," +
    "logs JSONB NOT NULL DEFAULT '[]'::jsonb," +
    "created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()," +
    "updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()" +
    ");"
  );
  await pool.query(
    "CREATE INDEX IF NOT EXISTS medtime_shares_owner_id_idx ON medtime_shares(owner_id);"
  );
}

app.get("/health", async (_req, res) => {
  const db = await pool.query("SELECT 1 AS ok");
  res.json({
    ok: db.rows[0]?.ok === 1,
    service: "QureMED Cloud",
    product: "MedTime",
    version: "1.1.0"
  });
});

app.post("/v1/shares", async (req, res) => {
  const ownerId = String(req.body?.ownerId || "").trim();
  const ownerName = String(req.body?.ownerName || "").trim();
  const medicines = Array.isArray(req.body?.medicines) ? req.body.medicines : [];
  const logs = Array.isArray(req.body?.logs) ? req.body.logs : [];

  if (!ownerId || !ownerName) {
    return res.status(400).json({ error: "ownerId and ownerName are required" });
  }

  const shareId = crypto.randomUUID();
  const ownerToken = token();
  const viewerToken = token();

  await pool.query(
    "INSERT INTO medtime_shares " +
      "(share_id, owner_id, owner_name, owner_token_hash, viewer_token_hash, medicines, logs) " +
      "VALUES ($1,$2,$3,$4,$5,$6::jsonb,$7::jsonb)",
    [
      shareId,
      ownerId,
      ownerName.slice(0, 120),
      hash(ownerToken),
      hash(viewerToken),
      JSON.stringify(medicines),
      JSON.stringify(logs.slice(0, 1000))
    ]
  );

  res.status(201).json({ shareId, ownerToken, viewerToken });
});

app.put("/v1/shares/:shareId", async (req, res) => {
  const auth = bearer(req);
  if (!auth) return res.status(401).json({ error: "Bearer token required" });

  const existing = await pool.query(
    "SELECT owner_token_hash FROM medtime_shares WHERE share_id = $1",
    [req.params.shareId]
  );
  if (!existing.rowCount) return res.status(404).json({ error: "Share not found" });

  const supplied = Buffer.from(hash(auth));
  const expected = Buffer.from(existing.rows[0].owner_token_hash);
  if (supplied.length !== expected.length || !crypto.timingSafeEqual(supplied, expected)) {
    return res.status(403).json({ error: "Invalid owner token" });
  }

  const ownerName = String(req.body?.ownerName || "").trim();
  const medicines = Array.isArray(req.body?.medicines) ? req.body.medicines : [];
  const logs = Array.isArray(req.body?.logs) ? req.body.logs : [];

  await pool.query(
    "UPDATE medtime_shares " +
      "SET owner_name = COALESCE(NULLIF($2,''), owner_name), " +
      "medicines = $3::jsonb, logs = $4::jsonb, updated_at = NOW() " +
      "WHERE share_id = $1",
    [
      req.params.shareId,
      ownerName.slice(0, 120),
      JSON.stringify(medicines),
      JSON.stringify(logs.slice(0, 1000))
    ]
  );

  res.json({ ok: true });
});

app.get("/v1/shares/:shareId", async (req, res) => {
  const auth = bearer(req);
  if (!auth) return res.status(401).json({ error: "Bearer token required" });

  const result = await pool.query(
    "SELECT share_id, owner_id, owner_name, viewer_token_hash, medicines, logs, updated_at " +
      "FROM medtime_shares WHERE share_id = $1",
    [req.params.shareId]
  );
  if (!result.rowCount) return res.status(404).json({ error: "Share not found" });

  const row = result.rows[0];
  const supplied = Buffer.from(hash(auth));
  const expected = Buffer.from(row.viewer_token_hash);
  if (supplied.length !== expected.length || !crypto.timingSafeEqual(supplied, expected)) {
    return res.status(403).json({ error: "Invalid viewer token" });
  }

  res.json({
    shareId: row.share_id,
    ownerId: row.owner_id,
    ownerName: row.owner_name,
    medicines: row.medicines || [],
    logs: row.logs || [],
    updatedAt: new Date(row.updated_at).getTime()
  });
});

app.delete("/v1/shares/:shareId", async (req, res) => {
  const auth = bearer(req);
  if (!auth) return res.status(401).json({ error: "Bearer token required" });

  const result = await pool.query(
    "DELETE FROM medtime_shares WHERE share_id=$1 AND owner_token_hash=$2",
    [req.params.shareId, hash(auth)]
  );
  if (!result.rowCount) return res.status(403).json({ error: "Invalid owner token or share" });
  res.status(204).end();
});

app.use((err, _req, res, _next) => {
  console.error(err?.message || err);
  res.status(500).json({ error: "Internal server error" });
});

await initDb();

app.listen(port, "0.0.0.0", () => {
  console.log("QureMED Cloud listening on port " + port);
});
