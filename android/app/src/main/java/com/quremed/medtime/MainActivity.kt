package com.quremed.medtime

import android.Manifest
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.FamilyRestroom
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Medication
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Snooze
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import com.quremed.medtime.cloud.CloudClient
import com.quremed.medtime.data.AppStore
import com.quremed.medtime.data.IntakeLog
import com.quremed.medtime.data.Medication
import com.quremed.medtime.data.PendingReminder
import com.quremed.medtime.data.RelativeConnection
import com.quremed.medtime.data.UserProfile
import com.quremed.medtime.reminder.ReminderScheduler
import com.quremed.medtime.reminder.ReminderSoundService
import com.quremed.medtime.ui.ArchiveScreen
import com.quremed.medtime.ui.FamilyCloudScreen
import com.quremed.medtime.ui.MedTimeTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var store: AppStore
    private val refresh = mutableIntStateOf(0)
    private val pending = mutableStateOf<PendingReminder?>(null)
    private val askReason = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        store = AppStore(this)
        readIntent(intent)
        setContent {
            MedTimeTheme {
                MedTimeApp(
                    store = store,
                    refreshKey = refresh.intValue,
                    pendingReminder = pending.value,
                    forceReason = askReason.value,
                    refresh = { refresh.intValue++ },
                    closeReminder = {
                        pending.value = null
                        askReason.value = false
                        refresh.intValue++
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        pending.value = store.pendingReminder()
        refresh.intValue++
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        readIntent(intent)
    }

    private fun readIntent(intent: Intent?) {
        val id = intent?.getStringExtra(ReminderScheduler.EXTRA_MEDICATION_ID)
        pending.value = store.pendingReminder() ?: id?.let { PendingReminder(it, System.currentTimeMillis()) }
        askReason.value = intent?.getBooleanExtra(EXTRA_ASK_MISSED_REASON, false) == true
    }

    companion object {
        const val EXTRA_ASK_MISSED_REASON = "ask_missed_reason"
    }
}

private enum class Tab(val title: String, val icon: ImageVector) {
    TODAY("Сьогодні", Icons.Rounded.Home),
    MEDS("Ліки", Icons.Rounded.Medication),
    FAMILY("Родина", Icons.Rounded.FamilyRestroom),
    SETTINGS("Налаштування", Icons.Rounded.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MedTimeApp(
    store: AppStore,
    refreshKey: Int,
    pendingReminder: PendingReminder?,
    forceReason: Boolean,
    refresh: () -> Unit,
    closeReminder: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = remember(refreshKey) { store.profile() }
    val medicines = remember(refreshKey) { store.medications() }
    val logs = remember(refreshKey) { store.logs() }
    val relatives = remember(refreshKey) { store.relatives() }
    var selectedTab by remember { mutableStateOf(Tab.TODAY) }
    var addMedicine by remember { mutableStateOf(false) }
    var showArchive by remember { mutableStateOf(false) }
    val cloudScope = rememberCoroutineScope()

    fun syncCloudOwner() {
        val share = store.cloudOwnerShare() ?: return
        val currentProfile = store.profile() ?: return
        cloudScope.launch {
            CloudClient.updateOwnerShare(
                share = share,
                profile = currentProfile,
                medicines = store.medications(),
                logs = store.logs()
            )
        }
    }

    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (profile == null) {
        RegistrationPage {
            store.saveProfile(UserProfile(name = it))
            refresh()
        }
        return
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF061018), Color(0xFF0B1721), Color(0xFF061018))
                )
            )
    ) {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        actionIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    title = {
                        Column {
                            Text("MedTime", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold)
                            Text("by QureMED Industries", color = MaterialTheme.colorScheme.primary, fontSize = 11.sp)
                        }
                    },
                    actions = {
                        Box(
                            Modifier
                                .padding(end = 16.dp)
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(profile.name.take(1).uppercase(), fontWeight = FontWeight.Bold)
                        }
                    }
                )
            },
            bottomBar = {
                NavigationBar(containerColor = Color(0xF20A141D)) {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            selected = selectedTab == tab && !showArchive,
                            onClick = {
                                selectedTab = tab
                                showArchive = false
                            },
                            icon = { Icon(tab.icon, tab.title) },
                            label = { Text(tab.title) },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = MaterialTheme.colorScheme.primary,
                                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                                indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        )
                    }
                }
            },
            floatingActionButton = {
                if (selectedTab == Tab.MEDS) {
                    FloatingActionButton(onClick = { addMedicine = true }) {
                        Icon(Icons.Rounded.Add, "Додати ліки")
                    }
                }
            }
        ) { innerPadding ->
            if (showArchive) {
                ArchiveScreen(
                    logs = logs,
                    modifier = Modifier.padding(innerPadding),
                    onBack = { showArchive = false }
                )
            } else {
                when (selectedTab) {
                    Tab.TODAY -> TodayPage(
                        profile = profile,
                        medicines = medicines,
                        logs = logs,
                        modifier = Modifier.padding(innerPadding),
                        add = { addMedicine = true },
                        openArchive = { showArchive = true }
                    )
                    Tab.MEDS -> MedicinesPage(medicines, Modifier.padding(innerPadding)) { medicine ->
                        ReminderScheduler.cancel(context, medicine.id)
                        store.deleteMedication(medicine.id)
                        syncCloudOwner()
                        refresh()
                    }
                    Tab.FAMILY -> FamilyCloudScreen(
                        store = store,
                        refreshKey = refreshKey,
                        modifier = Modifier.padding(innerPadding),
                        onRefresh = refresh
                    )
                    Tab.SETTINGS -> SettingsPage(store, refreshKey, Modifier.padding(innerPadding), refresh)
                }
            }
        }
    }

    if (addMedicine) {
        AddMedicineDialog(
            dismiss = { addMedicine = false },
            save = { medicine ->
                store.addMedication(medicine)
                ReminderScheduler.scheduleDaily(context, medicine)
                syncCloudOwner()
                addMedicine = false
                refresh()
            }
        )
    }

    pendingReminder?.let { reminder ->
        medicines.firstOrNull { it.id == reminder.medicationId }?.let { medicine ->
            ReminderDialog(
                medicine = medicine,
                forceReason = forceReason,
                taken = {
                    store.markTaken(medicine.id, reminder.dueAt)
                    ReminderScheduler.scheduleNextDay(context, medicine)
                    ReminderSoundService.stop(context)
                    syncCloudOwner()
                    closeReminder()
                },
                snooze = {
                    store.markSnoozed(medicine.id, reminder.dueAt)
                    ReminderScheduler.scheduleAt(context, medicine.id, System.currentTimeMillis() + 10 * 60 * 1000L)
                    ReminderSoundService.stop(context)
                    syncCloudOwner()
                    closeReminder()
                },
                missed = { reason ->
                    store.markMissed(medicine.id, reminder.dueAt, reason)
                    ReminderScheduler.scheduleNextDay(context, medicine)
                    ReminderSoundService.stop(context)
                    syncCloudOwner()
                    closeReminder()
                }
            )
        }
    }
}

@Composable
private fun RegistrationPage(register: (String) -> Unit) {
    var name by remember { mutableStateOf("") }
    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF164051), Color(0xFF061018)), radius = 1200f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xEE10202A), contentColor = MaterialTheme.colorScheme.onSurface)
        ) {
            Column(Modifier.padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Medication, null, tint = Color(0xFF041017), modifier = Modifier.size(38.dp))
                }
                Spacer(Modifier.height(24.dp))
                Text("Вітаємо в MedTime", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                Text(
                    "Ваш персональний помічник для прийому ліків",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp, bottom = 22.dp)
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Ваше ім’я") },
                    leadingIcon = { Icon(Icons.Rounded.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = { register(name.trim()) },
                    enabled = name.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp)
                        .height(54.dp),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Почати", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun TodayPage(
    profile: UserProfile,
    medicines: List<Medication>,
    logs: List<IntakeLog>,
    modifier: Modifier,
    add: () -> Unit,
    openArchive: () -> Unit
) {
    val next = remember(medicines) { nextMedication(medicines) }
    val startOfDay = remember {
        LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val todayLogs = logs.filter { it.recordedAt >= startOfDay }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Вітаємо, ${profile.name}", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Ваш план прийому на сьогодні", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102733), contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(17.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Наступний прийом", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(next?.time ?: "Не заплановано", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold)
                        }
                    }
                    if (next != null) {
                        Spacer(Modifier.height(18.dp))
                        Text(next.name, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(next.dose, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("Залишилось: ${next.remaining}", color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                    } else {
                        Button(onClick = add, modifier = Modifier.padding(top = 18.dp)) {
                            Icon(Icons.Rounded.Add, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Додати ліки")
                        }
                    }
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard("Прийнято", todayLogs.count { it.status == "TAKEN" }, Icons.Rounded.CheckCircle, Modifier.weight(1f))
                StatCard("Пропущено", todayLogs.count { it.status == "MISSED" }, Icons.Rounded.WarningAmber, Modifier.weight(1f))
            }
        }
        item {
            OutlinedButton(
                onClick = openArchive,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.Schedule, null)
                Spacer(Modifier.width(8.dp))
                Text("Архів прийомів по днях")
            }
        }
        item { Text("Сьогодні", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        if (medicines.isEmpty()) {
            item { InfoCard("Додайте препарат, час прийому та кількість таблеток.") }
        } else {
            items(medicines.sortedBy { it.time }, key = { it.id }) { medicine ->
                MedicineCard(medicine)
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: Int, icon: ImageVector, modifier: Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC10202A), contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.secondary)
            Text(value.toString(), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.padding(top = 12.dp))
            Text(title, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun MedicinesPage(medicines: List<Medication>, modifier: Modifier, delete: (Medication) -> Unit) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Мої ліки", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Час, доза та залишок препаратів", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (medicines.isEmpty()) item { InfoCard("Натисніть +, щоб додати перший препарат.") }
        items(medicines.sortedBy { it.time }, key = { it.id }) { medicine ->
            MedicineCard(medicine) {
                IconButton(onClick = { delete(medicine) }) {
                    Icon(Icons.Rounded.DeleteOutline, "Видалити", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun MedicineCard(medicine: Medication, trailing: (@Composable () -> Unit)? = null) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC10202A), contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(17.dp))
                    .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Medication, null, tint = MaterialTheme.colorScheme.secondary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(medicine.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(medicine.dose, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Label(Icons.Rounded.AccessTime, medicine.time)
                    Label(Icons.Rounded.Inventory2, medicine.remaining.toString())
                }
                if (medicine.remaining <= medicine.lowStockThreshold) {
                    Text("Запас майже закінчився", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
            trailing?.invoke()
        }
    }
}

@Composable
private fun Label(icon: ImageVector, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(5.dp))
        Text(value, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun InfoCard(text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0x9910202A), contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(22.dp))
    }
}

@Composable
private fun FamilyPage(
    store: AppStore,
    relatives: List<RelativeConnection>,
    refreshKey: Int,
    modifier: Modifier,
    refresh: () -> Unit
) {
    val invite = remember(refreshKey) { store.createFamilyInvite() }
    val qr = remember(invite) { invite?.let(::qrBitmap) }
    var resultText by remember { mutableStateOf<String?>(null) }
    val scanner = rememberLauncherForActivityResult(ScanContract()) { result ->
        val relative = result.contents?.let(store::importFamilyInvite)
        resultText = when {
            result.contents == null -> "Сканування скасовано"
            relative == null -> "Це не QR-код MedTime"
            else -> "Підключено: ${relative.ownerName}"
        }
        refresh()
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Родина", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Підключення близьких через QR-код", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102733), contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.QrCode2, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Text("Мій QR-доступ", fontSize = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                    Text(
                        "Родич отримає локальний знімок вашого графіка ліків.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                    qr?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR MedTime",
                            modifier = Modifier
                                .padding(12.dp)
                                .size(230.dp)
                                .clip(RoundedCornerShape(18.dp))
                        )
                    }
                    Button(
                        onClick = {
                            scanner.launch(
                                ScanOptions()
                                    .setDesiredBarcodeFormats(ScanOptions.QR_CODE)
                                    .setPrompt("Відскануйте QR-код MedTime")
                                    .setBeepEnabled(false)
                                    .setOrientationLocked(false)
                            )
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.QrCodeScanner, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Сканувати QR")
                    }
                    resultText?.let { Text(it, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 10.dp)) }
                }
            }
        }
        item { Text("Підключені", fontSize = 21.sp, fontWeight = FontWeight.Bold) }
        if (relatives.isEmpty()) item { InfoCard("Поки немає підключених родичів.") }
        items(relatives, key = { it.id }) { relative ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xCC10202A), contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Row(Modifier.padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.tertiary.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Person, null, tint = MaterialTheme.colorScheme.tertiary)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(relative.ownerName, fontWeight = FontWeight.Bold)
                        Text("Ліків у QR-знімку: ${relative.medicinesSnapshot.size}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }
        }
        item {
            Text(
                "У версії 1.0 QR передає локальний знімок. Жива синхронізація між телефонами буде підключена через QureMED Cloud.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsPage(store: AppStore, refreshKey: Int, modifier: Modifier, refresh: () -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val customSound = remember(refreshKey) { store.customSoundUri() }
    val exactAlarm = remember(refreshKey) { ReminderScheduler.canScheduleExact(context) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            store.setCustomSoundUri(uri)
            message = "Власний звук збережено"
            refresh()
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text("Налаштування", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
            Text("Звук і системні дозволи", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            SettingCard(Icons.Rounded.MusicNote, "Звук нагадування", if (customSound == null) "Вбудований звук iPhone 5" else "Власний аудіофайл") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { picker.launch(arrayOf("audio/*")) }) { Text("Обрати") }
                    if (customSound != null) {
                        TextButton(onClick = {
                            store.setCustomSoundUri(null)
                            message = "Повернуто вбудований звук"
                            refresh()
                        }) { Text("Скинути") }
                    }
                }
            }
        }
        item {
            SettingCard(
                Icons.Rounded.NotificationsActive,
                "Перевірити нагадування",
                "Запустить ваш звук і тестове сповіщення"
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            ReminderSoundService.startTest(context)
                            message = "Тестове нагадування запущено"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.NotificationsActive, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Перевірити дзвінок")
                    }
                    OutlinedButton(
                        onClick = {
                            ReminderSoundService.stop(context)
                            message = "Тест зупинено"
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Зупинити")
                    }
                }
                Text(
                    "У сповіщенні буде кнопка «Прийняти зараз». Вона одразу вимикає дзвінок.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
        item {
            SettingCard(Icons.Rounded.Schedule, "Точні нагадування", if (exactAlarm) "Дозвіл активний" else "Потрібно дозволити будильники") {
                if (!exactAlarm && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    Button(onClick = {
                        context.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:${context.packageName}")))
                    }) { Text("Дозволити") }
                } else {
                    OutlinedButton(onClick = {
                        ReminderScheduler.rescheduleAll(context)
                        message = "Нагадування перезаплановано"
                    }) { Text("Оновити нагадування") }
                }
            }
        }
        message?.let { item { Text(it, color = MaterialTheme.colorScheme.primary) } }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF102733), contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Column(Modifier.padding(22.dp)) {
                    Text("MedTime 1.1.0", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
                    Text("by QureMED Industries", color = MaterialTheme.colorScheme.primary)
                    Text(
                        "Застосунок нагадує про графік, внесений користувачем, і не замінює консультацію лікаря.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingCard(icon: ImageVector, title: String, subtitle: String, action: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xCC10202A), contentColor = MaterialTheme.colorScheme.onSurface)
    ) {
        Column(Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(title, fontWeight = FontWeight.Bold)
                    Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
                }
            }
            Spacer(Modifier.height(14.dp))
            action()
        }
    }
}

@Composable
private fun AddMedicineDialog(dismiss: () -> Unit, save: (Medication) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("09:00") }
    var remaining by remember { mutableStateOf("30") }
    var amount by remember { mutableStateOf("1") }
    val timeValid = remember(time) { runCatching { LocalTime.parse(time) }.isSuccess }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text("Додати ліки") },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    OutlinedTextField(name, { name = it }, label = { Text("Назва препарату") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(dose, { dose = it }, label = { Text("Доза, наприклад 1 таблетка") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                }
                item {
                    OutlinedTextField(
                        value = time,
                        onValueChange = { time = it.take(5) },
                        label = { Text("Час HH:MM") },
                        leadingIcon = { Icon(Icons.Rounded.AccessTime, null) },
                        isError = !timeValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = remaining,
                            onValueChange = { remaining = it.filter(Char::isDigit).take(4) },
                            label = { Text("Залишок") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { amount = it.filter(Char::isDigit).take(2) },
                            label = { Text("За раз") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    save(
                        Medication(
                            name = name.trim(),
                            dose = dose.trim().ifBlank { "За призначенням" },
                            time = time,
                            remaining = remaining.toIntOrNull() ?: 0,
                            amountPerDose = (amount.toIntOrNull() ?: 1).coerceAtLeast(1)
                        )
                    )
                },
                enabled = name.isNotBlank() && timeValid
            ) { Text("Зберегти") }
        },
        dismissButton = { TextButton(onClick = dismiss) { Text("Скасувати") } }
    )
}

@Composable
private fun ReminderDialog(
    medicine: Medication,
    forceReason: Boolean,
    taken: () -> Unit,
    snooze: () -> Unit,
    missed: (String) -> Unit
) {
    var reasonMode by remember(medicine.id, forceReason) { mutableStateOf(forceReason) }
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = { },
        icon = { Icon(Icons.Rounded.NotificationsActive, null, tint = MaterialTheme.colorScheme.primary) },
        title = { Text(if (reasonMode) "Чому не прийняли?" else "Час прийняти ліки") },
        text = {
            Column {
                Text(medicine.name, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
                Text(medicine.dose, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (reasonMode) {
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Причина") },
                        placeholder = { Text("Забув, немає препарату, побічна реакція…") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    )
                } else {
                    Text(
                        "Залишок після прийому: ${(medicine.remaining - medicine.amountPerDose).coerceAtLeast(0)}",
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (reasonMode) {
                Button(onClick = { missed(reason) }) { Text("Зберегти причину") }
            } else {
                Button(onClick = taken) {
                    Icon(Icons.Rounded.CheckCircle, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Прийняти зараз")
                }
            }
        },
        dismissButton = {
            if (reasonMode) {
                TextButton(onClick = { reasonMode = false }) { Text("Назад") }
            } else {
                Row {
                    TextButton(onClick = snooze) {
                        Icon(Icons.Rounded.Snooze, null)
                        Spacer(Modifier.width(4.dp))
                        Text("10 хв")
                    }
                    TextButton(onClick = { reasonMode = true }) { Text("Не прийняв") }
                }
            }
        }
    )
}

private fun nextMedication(medicines: List<Medication>): Medication? {
    val now = LocalDateTime.now()
    return medicines.filter { it.enabled }.minByOrNull { medicine ->
        runCatching {
            var dateTime = now.toLocalDate().atTime(LocalTime.parse(medicine.time))
            if (!dateTime.isAfter(now)) dateTime = dateTime.plusDays(1)
            dateTime
        }.getOrDefault(now.plusYears(1))
    }
}

private fun qrBitmap(content: String): Bitmap {
    val size = 900
    val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, size, size)
    return Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).apply {
        for (x in 0 until size) for (y in 0 until size) {
            setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
        }
    }
}
