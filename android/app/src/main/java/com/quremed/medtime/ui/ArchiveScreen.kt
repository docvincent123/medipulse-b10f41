package com.quremed.medtime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quremed.medtime.data.IntakeLog
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun ArchiveScreen(
    logs: List<IntakeLog>,
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val zone = ZoneId.systemDefault()
    val dayFormatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    val grouped = remember(logs) {
        logs.groupBy {
            Instant.ofEpochMilli(it.recordedAt).atZone(zone).toLocalDate()
        }.toList().sortedByDescending { it.first }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp, 8.dp, 18.dp, 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("Архів прийомів", fontSize = 30.sp, fontWeight = FontWeight.ExtraBold)
                    Text("Історія по днях", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onBack) { Text("Назад") }
            }
        }

        if (grouped.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF10202A),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        "Історія поки порожня. Після першого нагадування тут з’являться записи.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(20.dp)
                    )
                }
            }
        }

        grouped.forEach { (day, dayLogs) ->
            item(key = "day-" + day.toString()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF102733),
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Column(Modifier.padding(18.dp)) {
                        Text(day.format(dayFormatter), fontSize = 19.sp, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "Прийнято: " + dayLogs.count { it.status == "TAKEN" } +
                                "  •  Пропущено: " + dayLogs.count { it.status == "MISSED" } +
                                "  •  Відкладено: " + dayLogs.count { it.status == "SNOOZED" },
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp)
                        )

                        dayLogs.sortedByDescending { it.recordedAt }.forEach { log ->
                            val time = Instant.ofEpochMilli(log.recordedAt)
                                .atZone(zone)
                                .format(timeFormatter)
                            val statusText = when (log.status) {
                                "TAKEN" -> "Прийнято"
                                "SNOOZED" -> "Відкладено"
                                "MISSED" -> "Не прийнято"
                                else -> log.status
                            }
                            val statusColor = when (log.status) {
                                "TAKEN" -> MaterialTheme.colorScheme.primary
                                "MISSED" -> MaterialTheme.colorScheme.error
                                else -> MaterialTheme.colorScheme.secondary
                            }

                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        log.medicationName,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        time,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                                Text(statusText, color = statusColor, fontSize = 13.sp)
                                if (!log.reason.isNullOrBlank()) {
                                    Text(
                                        "Причина: " + log.reason,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(top = 3.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
