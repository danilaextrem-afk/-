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
    val sources = hunter.subs()
    var active by mutableStateOf(AutopilotEngine.enabled(ctx)); private set
    var busy by mutableStateOf(false); private set
    var report by mutableStateOf(AutopilotEngine.report(ctx)); private set
    var lastRun by mutableStateOf(AutopilotEngine.lastRun(ctx)); private set
    fun toggle(on: Boolean) {
        AutopilotEngine.setEnabled(ctx,on); active=on
        if(on) runNow(false)
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
    val sources by vm.sources.collectAsState(initial=emptyList())
    var raw by remember { mutableStateOf("") }
    var link by remember { mutableStateOf("") }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> uri?.let { vm.importFile(it) } }
    val now=System.currentTimeMillis()
    val due=tasks.filter { !it.done && it.dueAt <= now }
    val matches=clients.map { it to AutoRules.matches(it,properties) }.filter { it.second.isNotEmpty() }
    LaunchedEffect(Unit) { vm.refreshStatus() }
    LazyColumn(contentPadding=PaddingValues(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
        item {
            Row(verticalAlignment=Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("REALTOR / AI",color=Lime,style=MaterialTheme.typography.labelMedium)
                    Title("Автопилот")
                }
                TextButton(back) { Text("Назад") }
            }
        }
        item {
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Color(0xFF194D43),Color(0xFF202A3A)))).padding(20.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment=Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(if(vm.active) "Автопилот включён" else "Автопилот выключен",fontWeight=FontWeight.Bold,color=Color.White,style=MaterialTheme.typography.titleLarge)
                        Text("Источники: ${sources.size} · без API-ключей",color=Color(0xFFBBDAD2))
                    }
                    Switch(checked=vm.active,onCheckedChange={vm.toggle(it)})
                }
                Text(if(sources.isEmpty()) "Подключите разрешённый CSV/JSON-фид либо импортируйте объявления. Сейчас автоматического интернет-поиска нет." else "Фиды обновляются в фоне примерно каждые 6 часов при доступной сети. Подходящие объявления добавляются в CRM.",color=Color.White)
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
                Text("Добавить объявления автоматически",style=MaterialTheme.typography.titleLarge)
                Text("Вставьте ссылку или текст. Проверяем цену, площадь и дубли; неполные данные не выдумываем.",color=Muted)
                Field(link,{link=it},"Ссылка (необязательно)")
                Field(raw,{raw=it},"Текст объявления",single=false)
                Button(onClick={vm.importText(raw,link)},enabled=!vm.busy && (raw.isNotBlank() || link.isNotBlank()),modifier=Modifier.fillMaxWidth()) { Text("Обработать и добавить") }
                OutlinedButton(onClick={file.launch("*/*")},enabled=!vm.busy,modifier=Modifier.fillMaxWidth()) { Text("Импорт CSV / JSON") }
            }
        }
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
                Text("Бюджет ${client.budget.rub()} · предварительно по цене",color=Muted)
                picks.forEach { p -> Text("${p.title} · ${p.price.rub()}") }
                TextButton(openCrm) { Text("Открыть CRM") }
            }
        }
        item { Text("Автопилот не отправляет сообщения и не публикует объявления без вашего участия. Изменения цены не перезаписывают карточки CRM автоматически.",color=Muted,style=MaterialTheme.typography.bodySmall) }
    }
}
