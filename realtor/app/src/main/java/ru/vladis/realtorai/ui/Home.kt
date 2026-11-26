package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.vladis.realtorai.MainVm
import java.text.SimpleDateFormat
import java.util.*

private val dayFmt = SimpleDateFormat("d MMM, HH:mm", Locale("ru"))
fun fmtDate(ms: Long): String = dayFmt.format(Date(ms))

@Composable fun Stat(label: String, value: String, modifier: Modifier) = AppCard(modifier) {
    Text(value, fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Lime)
    Text(label, color = Muted)
}

@Composable fun Home(vm: MainVm, openAnalytics: () -> Unit, openSettings: () -> Unit, openPublish: () -> Unit, openHunter: () -> Unit, openSales: () -> Unit, openLeads: () -> Unit, openAutopilot: () -> Unit) {
    val props by vm.properties.collectAsState()
    val clients by vm.clients.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val active = props.filter { it.status == "active" }
    val open = tasks.filter { !it.done }
    val meetings = open.count { it.kind == "meeting" && it.dueAt >= System.currentTimeMillis() }
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text("REALTOR / AI", color = Lime, fontWeight = FontWeight.Bold, letterSpacing = 2.sp); Title("Мой рабочий день") }
                IconButton(openSettings) { Icon(Icons.Default.Settings, "Настройки") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF204D45),Color(0xFF26364C)))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                Text("АВТОПИЛОТ",color=Lime,style=MaterialTheme.typography.labelMedium)
                Text("Импорт и подбор",color=Color.White,style=MaterialTheme.typography.headlineSmall)
                Text("Проверить фиды, добавить новые объекты в CRM и найти совпадения по бюджету.",color=Color(0xFFD1E1DF))
                Button(onClick=openAutopilot,modifier=Modifier.fillMaxWidth()) { Text("Открыть автопилот"); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward,null,Modifier.size(18.dp)) }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF214D43), Color(0xFF233141)))).padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("ПОТЕНЦИАЛЬНАЯ КОМИССИЯ", color = Lime, style = MaterialTheme.typography.labelMedium)
                Text(active.sumOf { vm.earning(it) }.rub(), style = MaterialTheme.typography.headlineMedium, color = Color.White)
                Text("${active.size} объектов в работе · не гарантированный доход", color = Color(0xFFC9DBD7))
                FilledTonalButton(openAnalytics) { Text("Аналитика дохода"); Spacer(Modifier.width(8.dp)); Icon(Icons.Default.ArrowForward, null, Modifier.size(16.dp)) }
            }
        }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Stat("Объекты", active.size.toString(), Modifier.weight(1f)); Stat("Клиенты", clients.size.toString(), Modifier.weight(1f)) } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) { Stat("Задачи", open.size.toString(), Modifier.weight(1f)); Stat("Встречи", meetings.toString(), Modifier.weight(1f)) } }
        item { Title("Инструменты") }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleTile("Публикации", "Карточки и экспорт", Modifier.weight(1f), openPublish)
            ModuleTile("Поиск сделок", "Объекты и цены", Modifier.weight(1f), openHunter)
        } }
        item { Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            ModuleTile("Продажи", "Переговоры и расчёты", Modifier.weight(1f), openSales)
            ModuleTile("Лид-центр", "Воронка и контакты", Modifier.weight(1f), openLeads)
        } }
        item { Text("Ближайшие задачи", style = MaterialTheme.typography.titleLarge) }
        if (open.isEmpty()) item { AppCard { Text("Нет запланированных задач", color = Muted) } }
        items(open.take(4)) { t -> AppCard { Text(t.title, fontWeight = FontWeight.SemiBold); Text((if (t.kind == "meeting") "Встреча · " else "") + fmtDate(t.dueAt), color = Muted) } }
    }
}

@Composable private fun ModuleTile(title: String, subtitle: String, modifier: Modifier, onClick: () -> Unit) = AppCard(modifier, onClick) {
    Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp)); Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
}
