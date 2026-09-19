package com.quremed.medtime.ui

import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.CloudSync
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.quremed.medtime.cloud.CloudClient
import com.quremed.medtime.data.AppStore
import com.quremed.medtime.data.RelativeConnection
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FamilyCloudScreen(
    store: AppStore,
    refreshKey: Int,
    modifier: Modifier = Modifier,
    onRefresh: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val profile = remember(refreshKey) { store.profile() }
    val medicines = remember(refreshKey) { store.medications() }
    val logs = remember(refreshKey) { store.logs() }
    val relatives = remember(refreshKey) { store.relatives() }
    val ownerShare = remember(refreshKey) { store.cloudOwnerShare() }
    var serverInput by remember(refreshKey) { mutableStateOf(store.cloudServerUrl()) }
    var message by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    val onlineInvite = remember(ownerShare, profile) {
        if (ownerShare != null && profile != null) {
            CloudClient.buildInvite(ownerShare, profile.name)
        } else {
            null
        }
    }
    val onlineQr = remember(onlineInvite) { onlineInvite?.let(::familyQrBitmap) }

    fun refreshRelative(relative: RelativeConnection) {
        if (relative.cloudShareId == null) return
        scope.launch {
            busy = true
            CloudClient.fetchRelative(relative)
                .onSuccess {
                    store.addRelative(it)
                    message = "Дані " + it.ownerName + " оновлено"
                    onRefresh()
                }
                .onFailure {
                    message = "Не вдалося оновити: " + (it.message ?: "помилка мережі")
                }
            busy = false
        }
    }

    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val raw = result.contents
        if (raw == null) {
            message = "Сканування скасовано"
            return@rememberLauncherForActivityResult
        }

        val cloudRelative = CloudClient.parseInvite(raw)
        if (cloudRelative != null) {
            store.addRelative(cloudRelative)
            onRefresh()
            refreshRelative(cloudRelative)
        } else {
            val local = store.importFamilyInvite(raw)
            message = if (local != null) {
                "Підключено локально: " + local.ownerName
            } else {
                "Це не QR-код MedTime"
            }
            onRefresh()
        }
    }

    LaunchedEffect(refreshKey) {
        relatives.filter { it.cloudShareId != null }.forEach { relative ->
            CloudClient.fetchRelative(relative).onSuccess {
                store.addRelative(it)
            }
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Родина", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text(
                "Доступ до графіка ліків та архіву через інтернет",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF102733),
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Column(Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Cloud, null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("QureMED Cloud", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    }

                    Text(
                        "Вкажіть HTTPS-адресу вашого MedTime Cloud на Render.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    OutlinedTextField(
                        value = serverInput,
                        onValueChange = { serverInput = it },
                        label = { Text("Адреса сервера") },
                        placeholder = { Text("https://medtime-quremed-cloud.onrender.com") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )

                    if (ownerShare == null) {
                        Button(
                            onClick = {
                                val currentProfile = profile ?: return@Button
                                store.setCloudServerUrl(serverInput)
                                scope.launch {
                                    busy = true
                                    CloudClient.healthCheck(serverInput)
                                        .onSuccess {
                                            CloudClient.createShare(
                                                serverUrl = serverInput,
                                                profile = currentProfile,
                                                medicines = medicines,
                                                logs = logs
                                            ).onSuccess { share ->
                                                store.saveCloudOwnerShare(share)
                                                message = "Онлайн-доступ створено. Покажіть QR родичу."
                                                onRefresh()
                                            }.onFailure {
                                                message = "Не вдалося створити доступ: " + (it.message ?: "помилка")
                                            }
                                        }
                                        .onFailure {
                                            message = "Сервер недоступний: " + (it.message ?: "помилка")
                                        }
                                    busy = false
                                }
                            },
                            enabled = !busy && serverInput.startsWith("https://"),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Icon(Icons.Rounded.CloudSync, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Створити онлайн-доступ")
                        }
                    } else {
                        Text(
                            "Онлайн-доступ активний",
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 12.dp)
                        )
                        Text(
                            ownerShare.serverUrl,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        Button(
                            onClick = {
                                val currentProfile = profile ?: return@Button
                                scope.launch {
                                    busy = true
                                    CloudClient.updateOwnerShare(
                                        share = ownerShare,
                                        profile = currentProfile,
                                        medicines = store.medications(),
                                        logs = store.logs()
                                    ).onSuccess {
                                        message = "QureMED Cloud синхронізовано"
                                    }.onFailure {
                                        message = "Синхронізація не вдалася: " + (it.message ?: "помилка")
                                    }
                                    busy = false
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp)
                        ) {
                            Icon(Icons.Rounded.CloudSync, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Синхронізувати зараз")
                        }
                    }
                }
            }
        }

        if (onlineQr != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(26.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10202A),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Rounded.QrCode2, null, tint = MaterialTheme.colorScheme.primary)
                        Text(
                            "QR для родича",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Text(
                            "Після сканування родич може дивитися актуальні дані з будь-якого інтернету.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 6.dp)
                        )
                        Image(
                            bitmap = onlineQr.asImageBitmap(),
                            contentDescription = "MedTime Cloud QR",
                            modifier = Modifier
                                .padding(14.dp)
                                .size(230.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                }
            }
        }

        item {
            Button(
                onClick = {
                    scanner.launch(
                        ScanOptions()
                            .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                            .setPrompt("Відскануйте QR MedTime")
                            .setBeepEnabled(false)
                            .setOrientationLocked(false)
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.QrCodeScanner, null)
                Spacer(Modifier.width(8.dp))
                Text("Підключити родича через QR")
            }
        }

        message?.let {
            item {
                Text(
                    it,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 13.sp
                )
            }
        }

        item {
            Text("Підключені профілі", fontSize = 21.sp, fontWeight = FontWeight.Bold)
        }

        if (relatives.isEmpty()) {
            item {
                Text(
                    "Поки немає підключених профілів.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        items(relatives, key = { it.id }) { relative ->
            RelativeCloudCard(
                relative = relative,
                busy = busy,
                onRefresh = { refreshRelative(relative) }
            )
        }

        item {
            Text(
                "Власник контролює доступ через приватний QR-токен. Не публікуйте цей QR у відкритому доступі.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun RelativeCloudCard(
    relative: RelativeConnection,
    busy: Boolean,
    onRefresh: () -> Unit
) {
    val formatter = DateTimeFormatter.ofPattern("dd.MM HH:mm")
    val zone = ZoneId.systemDefault()

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF10202A),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.tertiary)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(relative.ownerName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                    Text(
                        if (relative.cloudShareId != null) "Онлайн QureMED Cloud" else "Локальний QR",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp
                    )
                }
                if (relative.cloudShareId != null) {
                    OutlinedButton(onClick = onRefresh, enabled = !busy) {
                        Icon(Icons.Rounded.Refresh, null)
                        Spacer(Modifier.width(4.dp))
                        Text("Оновити")
                    }
                }
            }

            if (relative.lastSyncAt != null) {
                Text(
                    "Оновлено: " + Instant.ofEpochMilli(relative.lastSyncAt)
                        .atZone(zone)
                        .format(formatter),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                "Ліки",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            if (relative.medicinesSnapshot.isEmpty()) {
                Text("Немає даних", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                relative.medicinesSnapshot.take(8).forEach { medication ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Rounded.Medication,
                            null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            medication.name + " • " + medication.time,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            "зал. " + medication.remaining,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            Text(
                "Останні прийоми",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 14.dp, bottom = 4.dp)
            )
            if (relative.logsSnapshot.isEmpty()) {
                Text("Архів ще порожній", color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                relative.logsSnapshot.take(8).forEach { log ->
                    val status = when (log.status) {
                        "TAKEN" -> "прийнято"
                        "MISSED" -> "не прийнято"
                        "SNOOZED" -> "відкладено"
                        else -> log.status.lowercase()
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            Icons.Rounded.CheckCircle,
                            null,
                            tint = if (log.status == "MISSED") {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(log.medicationName + " — " + status, fontSize = 13.sp)
                            if (!log.reason.isNullOrBlank()) {
                                Text(
                                    "Причина: " + log.reason,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun familyQrBitmap(content: String): Bitmap {
    val size = 900
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) {
            for (y in 0 until size) {
                setPixel(
                    x,
                    y,
                    if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
                )
            }
        }
    }
}
