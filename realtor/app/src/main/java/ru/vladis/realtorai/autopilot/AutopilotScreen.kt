package ru.vladis.realtorai.autopilot

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ai.Importer
import ru.vladis.realtorai.hunter.*
import ru.vladis.realtorai.ui.*
import java.io.ByteArrayOutputStream

class AutoVm(app: Application): AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val hunter = HunterDb.get(ctx)
    val history = ru.vladis.realtorai.data.WorkspaceDb.get(ctx).dao().recentRuns()
    private val prefs = ctx.getSharedPreferences("autopilot", 0)
    val catalogs = ru.vladis.realtorai.automation.IntegrationRegistry.listingSources.map { it.name }
    val enabledCatalogs = mutableStateListOf<String>().apply { addAll(catalogs.filter { prefs.getBoolean(it, true) }) }
    fun toggleCatalog(name: String, enabled: Boolean) {
        prefs.edit().putBoolean(name, enabled).apply()
        if (enabled) enabledCatalogs.add(name) else enabledCatalogs.remove(name)
    }
    var catalogQuery by mutableStateOf(prefs.getString("catalogQuery", "").orEmpty()); private set
    fun setQuery(value: String) {
        catalogQuery = value.take(100)
        prefs.edit().putString("catalogQuery", catalogQuery).apply()
    }
    val sources = hunter.subs()
    var active by mutableStateOf(AutopilotEngine.enabled(ctx)); private set
    var busy by mutableStateOf(false); private set
    var report by mutableStateOf(AutopilotEngine.report(ctx)); private set
    var lastRun by mutableStateOf(AutopilotEngine.lastRun(ctx)); private set
    fun toggle(on: Boolean) {
        AutopilotEngine.setEnabled(ctx,on); active=on
        if(on) runNow()
    }
    fun refreshStatus() {
        report = AutopilotEngine.report(ctx); lastRun = AutopilotEngine.lastRun(ctx)
    }
    private fun work(action: suspend () -> String) {
        if(busy) return
        busy = true
        viewModelScope.launch {
            try { report=action(); lastRun=AutopilotEngine.lastRun(ctx) }
            catch(e: kotlinx.coroutines.CancellationException) { throw e }
            catch(e: Exception) { report="Не выполнено: " + (e.message ?: e.javaClass.simpleName) }
            finally { busy=false }
        }
    }
    fun runNow(refresh: Boolean = true) = work {
        val errors = if(refresh) HuntRun.refreshAll(ctx).errors else emptyList()
        AutopilotEngine.sync(ctx,errors).summary()
    }
    fun importText(text: String, link: String) = work {
        val url = link.trim()
        if(url.isNotBlank() && AutoRules.urlKey(url)==null) error("Укажите ссылку http/https на объявление.")
        val raw = text.trim().ifBlank { if(url.isBlank()) error("Вставьте текст объявления или ссылку.") else Importer.fetch(url) }
        val row = HunterImport.fromText(raw,url)
        if(!AutoRules.valid(row)) error("Не удалось подтвердить цену и площадь. Вставьте текст объявления с этими данными.")
        val result = HunterImport.ingest(hunter,listOf(row))
        result.describe() + "\n" + AutopilotEngine.sync(ctx).summary()
    }
    fun importFile(uri: Uri) = work {
        val raw = withContext(Dispatchers.IO) {
            ctx.contentResolver.openInputStream(uri)?.use { input ->
                val out = ByteArrayOutputStream()
                val buf = ByteArray(8192)
                while(true) { val n=input.read(buf); if(n<0) break; if(out.size()+n>5_000_000) error("Файл больше 5 МБ"); out.write(buf,0,n) }
                out.toString("UTF-8").trimStart('\uFEFF')
            } ?: error("Не удалось открыть файл")
        }
        val rows = if(raw.trimStart().startsWith("[") || raw.trimStart().startsWith("{")) HunterImport.fromJson(raw,"file") else HunterImport.fromCsv(raw,"file")
        if(rows.isEmpty()) error("В файле нет распознанных объявлений")
        val result = HunterImport.ingest(hunter,rows)
        result.describe() + "\n" + AutopilotEngine.sync(ctx).summary()
    }
}

@Composable fun AutopilotScreen(main: MainVm, openHunter: () -> Unit, openCrm: () -> Unit, openTasks: () -> Unit, back: () -> Unit, vm: AutoVm = viewModel()) {
    val properties by main.properties.collectAsState()
    val clients by main.clients.collectAsState()
    val tasks by main.tasks.collectAsState()
    val history by vm.history.collectAsState(initial=emptyList())
    val sources by vm.sources.collectAsState(initial=emptyList())
    var raw by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    val geoContext = androidx.compose.ui.platform.LocalContext.current
    var geo by remember { mutableStateOf(GeoPreferences.read(geoContext)) }
    var editGeo by remember { mutableStateOf(false) }
    if (editGeo) GeoDialog(geo, { editGeo = false }) {
        geo = it
        GeoPreferences.save(geoContext, it)
        editGeo = false
    }
    var showCatalog by remember { mutableStateOf(false) }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { vm.importFile(it) } }
    val now=System.currentTimeMillis()
    val start = java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault())
    val end = start.plusDays(1).toInstant().toEpochMilli()
    val due = tasks.filter { !it.done && it.dueAt < end }
    val todayRuns = history.filter { it.at >= start.toInstant().toEpochMilli() }
    val matches=clients.map { it to AutoRules.matches(it,properties) }.filter { it.second.isNotEmpty() }
    LaunchedEffect(Unit) { vm.refreshStatus() }
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                TextButton(back) { Text("Назад") }
                ScreenHeading("Автопилот", "Мониторинг источников и действия в CRM")
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF194D43),Color(0xFF202A3A)))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if(vm.active) "Автопилот включён" else "Автопилот выключен",fontWeight=FontWeight.Bold,color=Color.White,style=MaterialTheme.typography.titleLarge)
                        Text("Источники: ${sources.size + vm.enabledCatalogs.size} · без API-ключей",color=Color(0xFFBBDAD2))
                    }
                    Switch(checked=vm.active,onCheckedChange={vm.toggle(it)})
                }
                Text(if (vm.busy) "Идёт обработка источников" else if (vm.active) "Фоновое обновление примерно каждые 6 часов" else "Запустите поиск вручную или включите фоновое обновление", color=Color.White)
                TextButton({ editGeo = true }) { Text("География · " + geo.locations.joinToString(", ").ifBlank { "Москва и МО" }, color = Color(0xFFA5F1C8)) }
                TextButton({ showCatalog = !showCatalog }) { Text(if (showCatalog) "Свернуть источники" else "Источники и район", color = Color(0xFFA5F1C8)) }
                if (showCatalog) vm.catalogs.forEach { name ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(name in vm.enabledCatalogs, { vm.toggleCatalog(name, it) })
                        Text(name.substringAfter(" · "))
                    }
                }
                if (showCatalog) Field(vm.catalogQuery, vm::setQuery, "Город или улица для поиска")
                Button(onClick={vm.runNow()},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()) { Text(if(vm.busy) "Обработка…" else "Запустить сейчас") }
            }
        }
        item {
            AppCard {
                Text("Результат последнего запуска",fontWeight=FontWeight.Bold)
                if(vm.lastRun>0) Text(fmtDate(vm.lastRun),color=Muted)
                Text(vm.report)
                if(vm.busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                TextButton(openHunter) { Text("Управлять источниками") }
            }
        }
        item {
            AppCard {
                Text("Импорт объявлений",style=MaterialTheme.typography.titleLarge)
                Text("Вставьте ссылку или текст. Проверяем цену, площадь и дубли; неполные данные не выдумываем.",color=Muted)
                Text("Для импорта собственника из фида укажите owner_name, owner_phone и crm_allowed=true только при наличии разрешения на перенос контакта в CRM.",color=Muted,style=MaterialTheme.typography.bodySmall)
                Field(link,{link=it},"Ссылка (необязательно)")
                Field(raw,{raw=it},"Текст объявления",single=false)
                Button(onClick={vm.importText(raw,link)},enabled=!vm.busy && (raw.isNotBlank() || link.isNotBlank()),modifier=Modifier.fillMaxWidth()) { Text("Обработать и добавить") }
                OutlinedButton(onClick={file.launch("*/*")},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()) { Text("Импорт CSV / JSON") }
            }
        }
        item { AppCard {
            Text("Отчёт за сегодня", style = MaterialTheme.typography.titleLarge)
            Text("Запусков в журнале: ${todayRuns.size} · задач до конца дня: ${due.size} · клиентов с подбором: ${matches.size}", color = Muted)
        } }
        if (history.isNotEmpty()) item { ScreenHeading("Журнал запусков", "Результаты фоновой и ручной работы") }
        items(history, key = { "run-" + it.id }) { entry ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.padding(top = 7.dp).size(8.dp).clip(androidx.compose.foundation.shape.CircleShape).background(Lime))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(fmtDate(entry.at), color = Lime, style = MaterialTheme.typography.labelMedium)
                    Text(entry.summary)
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
        val found = properties.filter { it.description.startsWith("Источник: Торги.Открыто") }
        if (found.isNotEmpty()) item { AppCard {
            Text("Найденные на торгах · ${found.size}", style = MaterialTheme.typography.titleLarge)
            found.take(3).forEach { Text(it.title.take(90) + " · " + it.price.rub()) }
        } }
        item { Title("План действий") }
        item {
            Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                AppCard(Modifier.weight(1f),openTasks) { Text("${due.size}",style=MaterialTheme.typography.headlineMedium,color=Lime); Text("Просрочено / сегодня") }
                AppCard(Modifier.weight(1f),openCrm) { Text("${matches.size}",style=MaterialTheme.typography.headlineMedium,color=Lime); Text("Клиенты с подбором") }
            }
        }
        items(due.take(5)) { t -> AppCard(onClick=openTasks) { Text(t.title,fontWeight=FontWeight.Bold); Text(fmtDate(t.dueAt),color=Muted) } }
        if(matches.isEmpty()) item { AppCard { Text("Пока нет совпадений по бюджету. Добавьте покупателей с бюджетом и активные объекты.",color=Muted) } }
        items(matches.take(10)) { (client,picks) ->
            AppCard {
                Text(client.name,fontWeight=FontWeight.Bold)
                Text("Бюджет ${client.budget.rub()} · с учётом критериев",color=Muted)
                picks.forEach { p -> Text("${p.title} · ${p.price.rub()}") }
                TextButton(openCrm) { Text("Открыть CRM") }
            }
        }
        item { Text("Автопилот не отправляет сообщения и не публикует объявления без вашего участия. Цена связанного объекта обновляется из его источника. История доступна в разделе поиска.",color=Muted,style=MaterialTheme.typography.bodySmall) }
    }
}
