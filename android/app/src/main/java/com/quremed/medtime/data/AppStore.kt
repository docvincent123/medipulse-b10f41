package com.quremed.medtime.data

import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.UUID

@Serializable
data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class Medication(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val dose: String,
    val time: String,
    val remaining: Int,
    val amountPerDose: Int = 1,
    val lowStockThreshold: Int = 5,
    val enabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)

@Serializable
data class IntakeLog(
    val id: String = UUID.randomUUID().toString(),
    val medicationId: String,
    val medicationName: String,
    val scheduledAt: Long,
    val recordedAt: Long = System.currentTimeMillis(),
    val status: String,
    val reason: String? = null
)

@Serializable
data class RelativeConnection(
    val id: String = UUID.randomUUID().toString(),
    val ownerId: String,
    val ownerName: String,
    val connectedAt: Long = System.currentTimeMillis(),
    val medicinesSnapshot: List<Medication> = emptyList()
)

@Serializable
data class PendingReminder(
    val medicationId: String,
    val dueAt: Long
)

@Serializable
data class FamilyInvite(
    val ownerId: String,
    val ownerName: String,
    val token: String = UUID.randomUUID().toString(),
    val medicines: List<Medication>,
    val createdAt: Long = System.currentTimeMillis()
)

class AppStore(context: Context) {
    private val prefs = context.getSharedPreferences("medtime_store", Context.MODE_PRIVATE)
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun profile(): UserProfile? = decode(KEY_PROFILE)

    fun saveProfile(profile: UserProfile) {
        encode(KEY_PROFILE, profile)
    }

    fun medications(): List<Medication> = decode<List<Medication>>(KEY_MEDICATIONS).orEmpty()

    fun saveMedications(items: List<Medication>) {
        encode(KEY_MEDICATIONS, items)
    }

    fun addMedication(item: Medication) {
        saveMedications(medications() + item)
    }

    fun updateMedication(item: Medication) {
        saveMedications(medications().map { if (it.id == item.id) item else it })
    }

    fun deleteMedication(id: String) {
        saveMedications(medications().filterNot { it.id == id })
    }

    fun medication(id: String): Medication? = medications().firstOrNull { it.id == id }

    fun logs(): List<IntakeLog> = decode<List<IntakeLog>>(KEY_LOGS).orEmpty()

    fun addLog(log: IntakeLog) {
        encode(KEY_LOGS, (listOf(log) + logs()).take(500))
    }

    fun markTaken(medicationId: String, dueAt: Long) {
        val medication = medication(medicationId) ?: return
        updateMedication(
            medication.copy(
                remaining = (medication.remaining - medication.amountPerDose).coerceAtLeast(0)
            )
        )
        addLog(
            IntakeLog(
                medicationId = medication.id,
                medicationName = medication.name,
                scheduledAt = dueAt,
                status = "TAKEN"
            )
        )
        clearPendingReminder()
    }

    fun markSnoozed(medicationId: String, dueAt: Long) {
        val medication = medication(medicationId) ?: return
        addLog(
            IntakeLog(
                medicationId = medication.id,
                medicationName = medication.name,
                scheduledAt = dueAt,
                status = "SNOOZED"
            )
        )
        clearPendingReminder()
    }

    fun markMissed(medicationId: String, dueAt: Long, reason: String) {
        val medication = medication(medicationId) ?: return
        addLog(
            IntakeLog(
                medicationId = medication.id,
                medicationName = medication.name,
                scheduledAt = dueAt,
                status = "MISSED",
                reason = reason.trim().ifBlank { "Причину не вказано" }
            )
        )
        clearPendingReminder()
    }

    fun pendingReminder(): PendingReminder? = decode(KEY_PENDING)

    fun setPendingReminder(pending: PendingReminder) {
        encode(KEY_PENDING, pending)
    }

    fun clearPendingReminder() {
        prefs.edit().remove(KEY_PENDING).apply()
    }

    fun relatives(): List<RelativeConnection> = decode<List<RelativeConnection>>(KEY_RELATIVES).orEmpty()

    fun addRelative(relative: RelativeConnection) {
        val updated = relatives().filterNot { it.ownerId == relative.ownerId } + relative
        encode(KEY_RELATIVES, updated)
    }

    fun customSoundUri(): Uri? = prefs.getString(KEY_CUSTOM_SOUND, null)?.let(Uri::parse)

    fun setCustomSoundUri(uri: Uri?) {
        prefs.edit().apply {
            if (uri == null) remove(KEY_CUSTOM_SOUND) else putString(KEY_CUSTOM_SOUND, uri.toString())
        }.apply()
    }

    fun createFamilyInvite(): String? {
        val profile = profile() ?: return null
        val invite = FamilyInvite(
            ownerId = profile.id,
            ownerName = profile.name,
            medicines = medications()
        )
        val payload = json.encodeToString(invite)
        val encoded = Base64.encodeToString(
            payload.toByteArray(Charsets.UTF_8),
            Base64.URL_SAFE or Base64.NO_WRAP
        )
        return "medtime://family?payload=$encoded"
    }

    fun importFamilyInvite(raw: String): RelativeConnection? = runCatching {
        val uri = Uri.parse(raw)
        require(uri.scheme == "medtime" && uri.host == "family")
        val encoded = requireNotNull(uri.getQueryParameter("payload"))
        val decoded = String(
            Base64.decode(encoded, Base64.URL_SAFE or Base64.NO_WRAP),
            Charsets.UTF_8
        )
        val invite = json.decodeFromString<FamilyInvite>(decoded)
        RelativeConnection(
            ownerId = invite.ownerId,
            ownerName = invite.ownerName,
            medicinesSnapshot = invite.medicines
        ).also(::addRelative)
    }.getOrNull()

    private inline fun <reified T> decode(key: String): T? = runCatching {
        prefs.getString(key, null)?.let { json.decodeFromString<T>(it) }
    }.getOrNull()

    private inline fun <reified T> encode(key: String, value: T) {
        prefs.edit().putString(key, json.encodeToString(value)).apply()
    }

    companion object {
        private const val KEY_PROFILE = "profile"
        private const val KEY_MEDICATIONS = "medications"
        private const val KEY_LOGS = "logs"
        private const val KEY_RELATIVES = "relatives"
        private const val KEY_PENDING = "pending"
        private const val KEY_CUSTOM_SOUND = "custom_sound_uri"
    }
}
