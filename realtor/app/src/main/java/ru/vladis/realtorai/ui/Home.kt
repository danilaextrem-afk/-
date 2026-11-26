package ru.vladis.realtorai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.WorkspaceDb
import ru.vladis.realtorai.publish.Thumb

private val shortDate = DateTimeFormatter.ofPattern("d MMM, HH:mm", Locale.forLanguageTag("ru"))
fun fmtDate(ms: Long): String = Instant.ofEpochMilli(ms).atZone(ZoneId.systemDefault()).format(shortDate)

@Composable
fun Home(
    vm: MainVm, openAnalytics: () -> Unit, openSettings: () -> Unit, openPublish: () -> Unit,
    openHunter: () -> Unit, openSales: () -> Unit, openLeads: () -> Unit, openAutopilot: () -> Unit,
    openAgent: (String) -> Unit, openProperties: () -> Unit, openCrm: () -> Unit, openTasks: () -> Unit,
    openOwner: (Long) -> Unit
) {
    val properties by vm.properties.collectAsState()
    val clients by vm.clients.collectAsState()
    val tasks by vm.tasks.collectAsState()
    val listings by vm.listings.collectAsState()
    val ctx = LocalContext.current
    val db = remember(ctx) { WorkspaceDb.get(ctx) }
    val jobs by remember(db) { db.agent().recent() }.collectAsState(emptyList())
    val runs by remember(db) { db.dao().recentRuns() }.collectAsState(emptyList())
    val offers by remember(db) { db.hunter().offers() }.collectAsState(emptyList())
    val prefs = remember { ctx.getSharedPreferences("first_run", 0) }
    var welcome by rememberSaveable { mutableStateOf(!prefs.getBoolean("seen", false)) }
    fun finishWelcome() { prefs.edit().putBoolean("seen", true).apply(); welcome = false }
    val active = properties.filter { it.status == "active" }
    val pending = tasks.filterNot { it.done }
    val activeJob = jobs.firstOrNull { it.state == "running" || it.state == "queued" }
    val onDeck = pending.filter { it.kind.startsWith("owner/") && it.kind.endsWith("/report") }
    val recent = active.sortedByDescending { it.createdAt }.take(6)
    var command by rememberSaveable { mutableStateOf("") }

    LazyColumn(contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(38.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center) {
                    Icon(androidx.compose.ui.res.painterResource(ru.vladis.realtorai.R.drawable.ic_realtor), null, tint = Color.Unspecified, modifier = Modifier.size(38.dp))
                }
                Text("Realtor AI", Modifier.padding(start = 10.dp).weight(1f), style = MaterialTheme.typography.titleLarge)
                IconButton(openSettings) { Icon(Icons.Default.Settings, "Настройки") }
            }
            Spacer(Modifier.height(12.dp))
            Text(LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.forLanguageTag("ru")))
                .replaceFirstChar(Char::uppercase), color = Lime, style = MaterialTheme.typography.labelMedium)
            Text("Мой рабочий день", style = MaterialTheme.typography.displaySmall)
        }
        item {
            val shape = RoundedCornerShape(28.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape)
                    .background(Brush.linearGradient(listOf(Color(0xFF10382B), Color(0xFF1E302F), Color(0xFF121E1C))))
                    .padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, null, tint = Color(0xFF87EFBB), modifier = Modifier.size(16.dp))
                    Text("Поручение агенту", style = MaterialTheme.typography.titleMedium, color = Color(0xFFF0F8F3))
                }
                Text(activeJob?.goal ?: "Найдите объекты, подготовьте подборку или соберите план недели.",
                    color = Color(0xFFC0D9CA))
                Surface(color = Color(0xFF24493D), shape = RoundedCornerShape(18.dp)) {
                    Row(Modifier.padding(start = 12.dp, end = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        TextField(command, { command = it.take(500) }, modifier = Modifier.weight(1f), singleLine = true,
                            placeholder = { Text("Что нужно сделать?") }, colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent, unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                                focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                                focusedPlaceholderColor = Color(0xFFBAD6C6), unfocusedPlaceholderColor = Color(0xFFBAD6C6)
                            ))
                        IconButton({ openAgent(command.trim()) }) {
                            Icon(Icons.Default.ArrowForward, "Поручить агенту", tint = Color(0xFFA3F6C8))
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("План недели", Modifier.clip(RoundedCornerShape(8.dp)).clickable { openAgent("Составь план на неделю") }.padding(6.dp),
                        color = Color(0xFFACE9C8), style = MaterialTheme.typography.labelMedium)
                    Text("Найти квартиры", Modifier.clip(RoundedCornerShape(8.dp)).clickable { openAgent("Найди выгодные квартиры") }.padding(6.dp),
                        color = Color(0xFFACE9C8), style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (welcome && active.isEmpty() && clients.isEmpty()) item {
            Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Surface2).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.Place, null, tint = Lime, modifier = Modifier.padding(top = 4.dp).size(22.dp))
                Column(Modifier.weight(1f)) {
                    Text("Подключите первый источник", style = MaterialTheme.typography.titleMedium)
                    Text("Публичные торги или ваш фид. Новые объекты появятся в рабочей базе.", color = Muted)
                    Row {
                        TextButton({ finishWelcome(); openAutopilot() }) { Text("Настроить поиск") }
                        TextButton(::finishWelcome) { Text("Позже") }
                    }
                }
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OverviewMetric(Modifier.weight(1f), Icons.Default.Place, offers.count { it.active }.toString(), "Найдено", openHunter)
                OverviewMetric(Modifier.weight(1f), Icons.Default.Person, clients.count { it.stage == "new" }.toString(), "Новые лиды", openCrm)
                OverviewMetric(Modifier.weight(1f), Icons.Default.Done, tasks.count { it.done }.toString(), "Выполнено", openTasks)
            }
        }
        item {
            Surface(shape = RoundedCornerShape(24.dp), color = Surface1) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Потенциальная комиссия", style = MaterialTheme.typography.titleMedium)
                            Text(active.sumOf(vm::earning).rub(), style = MaterialTheme.typography.headlineMedium)
                        }
                        IconButton(openAnalytics) { Icon(Icons.Default.ArrowForward, "Открыть аналитику", tint = Lime) }
                    }
                    if (active.isEmpty()) Text("График появится после добавления объектов с ценой.", color = Muted)
                    else {
                        var period by rememberSaveable { mutableIntStateOf(30) }
                        Chips(listOf("7 дней", "30 дней", "Всё"), when (period) { 7 -> "7 дней"; 30 -> "30 дней"; else -> "Всё" }) {
                            period = when (it) { "7 дней" -> 7; "30 дней" -> 30; else -> 0 }
                        }
                        val threshold = if (period == 0) 0 else System.currentTimeMillis() - period * 86_400_000L
                        val selection = active.filter { it.createdAt >= threshold }.sortedBy { it.createdAt }
                        if (selection.isEmpty()) Text("За этот период объектов нет.", color = Muted)
                        else {
                            val totals = selection.map(vm::earning).runningFold(0.0, Double::plus)
                            RevenueChart(totals)
                            Text("По дате добавления · ${selection.size} объектов · ${totals.last().rub()}", color = Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                val loop = rememberInfiniteTransition(label = "autopilot")
                val alpha by loop.animateFloat(.4f, 1f, infiniteRepeatable(tween(1000), RepeatMode.Reverse), label = "pulse")
                Box(Modifier.size(8.dp).clip(CircleShape).background(Lime.copy(alpha = if (activeJob != null) alpha else 1f)))
                Column(Modifier.weight(1f)) {
                    Text("Автопилот", style = MaterialTheme.typography.titleMedium)
                    Text(runs.firstOrNull()?.let { "Последний запуск · " + fmtDate(it.at) } ?: "Ожидает первого запуска", color = Muted)
                }
                TextButton(openAutopilot) { Text("Открыть автопилот") }
            }
        }
        item {
            ScreenHeading("Воронка", "Клиенты по этапам") { TextButton(openCrm) { Text("CRM") } }
            val pipeline = clients.groupingBy { it.stage }.eachCount()
            if (clients.isEmpty()) Text("Добавьте первый контакт в CRM или импортируйте список.", color = Muted)
            else {
                val denominator = clients.size.toFloat()
                stages.filter { it.first != "lost" }.forEach { (key, title) ->
                    val count = pipeline.getOrDefault(key, 0)
                    Row(Modifier.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(title, Modifier.width(85.dp), style = MaterialTheme.typography.bodySmall)
                        LinearProgressIndicator(progress = { count / denominator }, Modifier.weight(1f).height(5.dp))
                        Text(count.toString(), Modifier.padding(start = 10.dp), style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        item {
            ScreenHeading("Недвижимость", "Последние объекты в работе") { TextButton(openProperties) { Text("Все") } }
            if (recent.isEmpty()) Text("Здесь будут объекты из поиска и вашей CRM.", color = Muted)
            else Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                recent.forEach { property ->
                    val cover = listings.firstOrNull { it.propertyId == property.id }?.cover.orEmpty()
                    Surface(onClick = { openOwner(property.id) }, modifier = Modifier.width(196.dp), shape = RoundedCornerShape(18.dp), color = Surface1) {
                        Column {
                            if (cover.isNotBlank()) Thumb(cover, Modifier.fillMaxWidth().height(94.dp))
                            Column(Modifier.padding(12.dp)) {
                                Text(property.title, maxLines = 1, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.titleMedium)
                                Text(property.address.ifBlank { "Адрес уточняется" }, maxLines = 1, overflow = TextOverflow.Ellipsis, color = Muted)
                                Text(if (property.price > 0) property.price.rub() else "Цена уточняется", color = Lime)
                            }
                        }
                    }
                }
            }
        }
        item { ScreenHeading("Повестка", "Ближайшие открытые задачи") { TextButton(openTasks) { Text("Календарь") } } }
        if (pending.isEmpty()) item { Text("На ближайшие дни задач нет.", color = Muted) }
        items(pending.take(4), key = { it.id }) { task ->
            DashboardLine(Icons.Default.DateRange, task.title, fmtDate(task.dueAt), openTasks)
        }
        if (onDeck.isNotEmpty()) {
            item { Title("Отчёты собственникам") }
            items(onDeck.take(3), key = { "owner-${it.id}" }) { task ->
                DashboardLine(Icons.Default.Info, task.title, fmtDate(task.dueAt)) {
                    task.kind.split('/').getOrNull(1)?.toLongOrNull()?.let(openOwner)
                }
            }
        }
        item {
            Title("Рабочие инструменты")
            DashboardLine(Icons.Default.Share, "Публикации", "Черновики и площадки", openPublish)
            DashboardLine(Icons.Default.Search, "Поиск сделок", "Источники, карта и цены", openHunter)
            DashboardLine(Icons.Default.Person, "Лиды", "Контакты и импорт", openLeads)
            DashboardLine(Icons.Default.Call, "Продажи", "Переговоры и предложения", openSales)
        }
    }
}

@Composable
private fun OverviewMetric(modifier: Modifier, icon: ImageVector, value: String, label: String, click: () -> Unit) {
    Surface(click, modifier, shape = RoundedCornerShape(18.dp), color = Surface1) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = Lime, modifier = Modifier.size(18.dp))
            Text(value, style = MaterialTheme.typography.headlineSmall)
            Text(label, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
internal fun DashboardLine(icon: ImageVector, name: String, detail: String, click: () -> Unit) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = click).padding(vertical = 10.dp, horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Lime, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(name, style = MaterialTheme.typography.titleMedium)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        Icon(Icons.Default.ArrowForward, null, tint = Muted, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun RevenueChart(values: List<Double>) {
    val accent = Lime
    val grid = MaterialTheme.colorScheme.outlineVariant
    val progress by animateFloatAsState((values.lastOrNull() ?: 0.0).coerceAtLeast(0.0).toFloat(), label = "commission")
    val target = (values.lastOrNull() ?: 0.0).coerceAtLeast(1.0)
    Canvas(Modifier.fillMaxWidth().height(94.dp)) {
        for (row in 1..3) {
            val y = row * size.height / 4
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
        }
        val scale = (progress / target).coerceIn(0.0, 1.0)
        val coordinates = values.mapIndexed { i, v ->
            Offset(i * size.width / values.lastIndex.coerceAtLeast(1), size.height * (1 - (v / target * scale).coerceIn(0.0, 1.0) * .88).toFloat())
        }
        if (coordinates.size < 2) return@Canvas
        val line = Path().apply {
            moveTo(coordinates.first().x, coordinates.first().y)
            coordinates.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val shade = Path().apply {
            addPath(line)
            lineTo(size.width, size.height); lineTo(0f, size.height); close()
        }
        drawPath(shade, Brush.verticalGradient(listOf(accent.copy(alpha = .28f), accent.copy(alpha = .02f))))
        drawPath(line, accent, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
    }
}
