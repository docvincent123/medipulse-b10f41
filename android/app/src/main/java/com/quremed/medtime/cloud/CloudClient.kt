package com.quremed.medtime.cloud

import android.net.Uri
import com.quremed.medtime.data.CloudOwnerShare
import com.quremed.medtime.data.IntakeLog
import com.quremed.medtime.data.Medication
import com.quremed.medtime.data.RelativeConnection
import com.quremed.medtime.data.UserProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID

@Serializable
private data class CreateShareRequest(
    val ownerId: String,
    val ownerName: String,
    val medicines: List<Medication>,
    val logs: List<IntakeLog>
)

@Serializable
private data class UpdateShareRequest(
    val ownerName: String,
    val medicines: List<Medication>,
    val logs: List<IntakeLog>
)

@Serializable
private data class ShareCredentials(
    val shareId: String,
    val ownerToken: String,
    val viewerToken: String
)

@Serializable
private data class CloudSnapshot(
    val shareId: String,
    val ownerId: String,
    val ownerName: String,
    val medicines: List<Medication> = emptyList(),
    val logs: List<IntakeLog> = emptyList(),
    val updatedAt: Long
)

object CloudClient {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    suspend fun createShare(
        serverUrl: String,
        profile: UserProfile,
        medicines: List<Medication>,
        logs: List<IntakeLog>
    ): Result<CloudOwnerShare> = withContext(Dispatchers.IO) {
        runCatching {
            val base = normalizeServer(serverUrl)
            require(base.startsWith("https://")) {
                "Для віддаленого доступу потрібен HTTPS-сервер"
            }

            val body = json.encodeToString(
                CreateShareRequest(
                    ownerId = profile.id,
                    ownerName = profile.name,
                    medicines = medicines,
                    logs = logs
                )
            )
            val response = request(
                method = "POST",
                url = "$base/v1/shares",
                body = body
            )
            val credentials = json.decodeFromString<ShareCredentials>(response)
            CloudOwnerShare(
                serverUrl = base,
                shareId = credentials.shareId,
                ownerToken = credentials.ownerToken,
                viewerToken = credentials.viewerToken
            )
        }
    }

    suspend fun updateOwnerShare(
        share: CloudOwnerShare,
        profile: UserProfile,
        medicines: List<Medication>,
        logs: List<IntakeLog>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val body = json.encodeToString(
                UpdateShareRequest(
                    ownerName = profile.name,
                    medicines = medicines,
                    logs = logs
                )
            )
            request(
                method = "PUT",
                url = "${normalizeServer(share.serverUrl)}/v1/shares/${share.shareId}",
                body = body,
                bearerToken = share.ownerToken
            )
            Unit
        }
    }

    suspend fun fetchRelative(relative: RelativeConnection): Result<RelativeConnection> =
        withContext(Dispatchers.IO) {
            runCatching {
                val server = requireNotNull(relative.cloudServer)
                val shareId = requireNotNull(relative.cloudShareId)
                val token = requireNotNull(relative.viewerToken)
                val url = "${normalizeServer(server)}/v1/shares/$shareId"

                val snapshot = json.decodeFromString<CloudSnapshot>(
                    request(method = "GET", url = url, bearerToken = token)
                )
                relative.copy(
                    ownerId = snapshot.ownerId,
                    ownerName = snapshot.ownerName,
                    medicinesSnapshot = snapshot.medicines,
                    logsSnapshot = snapshot.logs,
                    lastSyncAt = snapshot.updatedAt
                )
            }
        }

    fun buildInvite(share: CloudOwnerShare, ownerName: String): String =
        Uri.Builder()
            .scheme("medtime")
            .authority("cloud")
            .appendQueryParameter("server", share.serverUrl)
            .appendQueryParameter("share", share.shareId)
            .appendQueryParameter("token", share.viewerToken)
            .appendQueryParameter("name", ownerName)
            .build()
            .toString()

    fun parseInvite(raw: String): RelativeConnection? = runCatching {
        val uri = Uri.parse(raw)
        require(uri.scheme == "medtime" && uri.host == "cloud")
        val server = requireNotNull(uri.getQueryParameter("server"))
        val shareId = requireNotNull(uri.getQueryParameter("share"))
        val token = requireNotNull(uri.getQueryParameter("token"))
        val name = uri.getQueryParameter("name").orEmpty().ifBlank { "Родич MedTime" }

        RelativeConnection(
            id = UUID.randomUUID().toString(),
            ownerId = shareId,
            ownerName = name,
            cloudServer = normalizeServer(server),
            cloudShareId = shareId,
            viewerToken = token
        )
    }.getOrNull()

    suspend fun healthCheck(serverUrl: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = normalizeServer(serverUrl)
            require(base.startsWith("https://")) {
                "Для віддаленого доступу потрібен HTTPS-сервер"
            }
            request("GET", "$base/health")
        }
    }

    private fun normalizeServer(value: String): String = value.trim().trimEnd('/')

    private fun request(
        method: String,
        url: String,
        body: String? = null,
        bearerToken: String? = null
    ): String {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.requestMethod = method
            connection.connectTimeout = 10_000
            connection.readTimeout = 15_000
            connection.setRequestProperty("Accept", "application/json")
            if (bearerToken != null) {
                connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            }
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body) }
            }

            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                error("QureMED Cloud: HTTP $status ${response.take(180)}")
            }
            response
        } finally {
            connection.disconnect()
        }
    }
}
