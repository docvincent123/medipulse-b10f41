# QureMED Cloud — MedTime

Backend for MedTime family sharing over HTTPS.

## API

- `GET /health`
- `POST /v1/shares` — creates a private family share.
- `PUT /v1/shares/:shareId` — owner updates medicines and intake history using a Bearer owner token.
- `GET /v1/shares/:shareId` — relative reads a snapshot using a Bearer viewer token.
- `DELETE /v1/shares/:shareId` — owner revokes a share.

Only SHA-256 hashes of owner/viewer tokens are stored in PostgreSQL.

## Render

Create a Render Blueprint from this repository using the root `render.yaml`.

For testing, Render free web services are acceptable. Render's free PostgreSQL database currently expires after 30 days, so use a persistent database before real users rely on it.

## Privacy

This backend stores medication schedules and intake history. Use HTTPS, strong access tokens, a persistent database with backups, a privacy policy, access revocation, and appropriate legal/compliance review before production use with health data.
