package ru.vladis.realtorai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ai.Prompts
import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.Property

@Composable
fun Assistant(vm: MainVm, openModels: () -> Unit, openSettings: () -> Unit, openAgent: (String) -> Unit) {
    val props by vm.properties.collectAsState()
    val clients by vm.clients.collectAsState()
    var mode by rememberSaveable { mutableStateOf("") }
    var text by rememberSaveable { mutableStateOf("") }
    var property by remember { mutableStateOf<Property?>(null) }
    var client by remember { mutableStateOf<Client?>(null) }
    var modelMenu by remember { mutableStateOf(false) }
    var contextMenu by remember { mutableStateOf(false) }
    var agentConsent by remember { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val speech = rememberLocalSpeech()
    val list = rememberLazyListState()
    val modelLabel = when {
        vm.cloudReady -> "GPT-OSS 120B"
        vm.deviceReady -> vm.activeModel.label
        else -> "Подключить ИИ"
    }
    if (agentConsent) AlertDialog(
        onDismissRequest = { agentConsent = false },
        title = { Text("Передать поручение в Groq?") },
        text = { Text("Groq получит текст поручения. Уберите персональные данные перед продолжением. Инструменты CRM выполняются на устройстве.") },
        confirmButton = { TextButton({ agentConsent = false; openAgent(text.trim()) }) { Text("Продолжить") } },
        dismissButton = { TextButton({ agentConsent = false }) { Text("Назад") } }
    )
    LaunchedEffect(vm.chat.size, vm.busy, vm.error) {
        val item = vm.chat.size - 1 + (if (vm.busy) 1 else 0) + (if (vm.error != null) 1 else 0)
        if (vm.chat.isNotEmpty() && item >= 0) list.scrollToItem(item)
    }
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ИИ-ассистент", style = MaterialTheme.typography.headlineSmall)
                Box {
                    TextButton({ modelMenu = true }, enabled = !vm.busy, contentPadding = PaddingValues(0.dp)) {
                        Text(modelLabel, color = Lime)
                        Icon(Icons.Default.ArrowDropDown, null)
                    }
                    DropdownMenu(modelMenu, { modelMenu = false }) {
                        DropdownMenuItem({ Text(if (vm.cloudConfigured) "GPT-OSS 120B · Groq" else "Подключить Groq") }, {
                            modelMenu = false
                            if (vm.cloudConfigured) vm.configureCloud(null, true) else openSettings()
                        })
                        DropdownMenuItem({ Text("Локальный режим") }, {
                            modelMenu = false
                            vm.configureCloud(null, false)
                            if (!vm.deviceReady) openModels()
                        })
                        DropdownMenuItem({ Text("Выбрать локальную модель") }, { modelMenu = false; openModels() })
                    }
                }
            }
            if (vm.chat.isNotEmpty()) IconButton(vm::clearChat, enabled = !vm.busy) {
                Icon(Icons.Default.Add, "Новый диалог")
            }
        }
        HorizontalDivider()
        LazyColumn(Modifier.weight(1f).fillMaxWidth(), state = list,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)) {
            if (vm.chat.isEmpty()) item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Star, null, Modifier.size(28.dp), tint = Lime)
                    Text("Что нужно сделать?", style = MaterialTheme.typography.displaySmall)
                    Text("Москва и Московская область", color = Muted)
                    HorizontalDivider(Modifier.padding(top = 8.dp))
                    DashboardLine(Icons.Default.Search, "Найти квартиры", "Поиск по подключённым источникам") {
                        text = "Найди двушки от собственников в Даниловском районе до 15 миллионов"
                    }
                    DashboardLine(Icons.Default.DateRange, "Составить план", "Многоэтапное поручение") {
                        openAgent("Составь план на неделю")
                    }
                    DashboardLine(Icons.Default.Call, "Подготовиться к звонку", "Аргументы и вопросы для разговора") {
                        text = "Подготовь меня к первому звонку собственнику"
                    }
                }
            }
            items(vm.chat.size) { index ->
                val (role, message) = vm.chat[index]
                if (role == "user") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                        Text(message, Modifier.widthIn(max = 500.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(14.dp)).padding(14.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                } else {
                    Text("Realtor AI", style = MaterialTheme.typography.labelMedium, color = Lime)
                    SelectionContainer { ResponseBody(message) }
                    Row {
                        TextButton({ clipboard.setText(AnnotatedString(message)) }) { Text("Копировать") }
                        if (speech != null) TextButton({
                            speech.speak(message.take(3500), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "reply-$index")
                        }) { Text("Озвучить") }
                        if (index == vm.chat.lastIndex && vm.canRetry && !vm.busy) {
                            TextButton(vm::retryChat) { Text("Повторить") }
                        }
                    }
                }
            }
            if (vm.busy) item {
                if (vm.streamingAnswer.isBlank()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text("Готовлю ответ", color = Muted)
                    }
                } else ResponseBody(vm.streamingAnswer)
            }
            vm.error?.let { issue ->
                item {
                    Text(issue, color = MaterialTheme.colorScheme.error)
                    if (vm.canRetry && !vm.busy) TextButton(vm::retryChat) { Text("Повторить") }
                }
            }
        }
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            CloudUsage(vm)
            Surface(shape = RoundedCornerShape(16.dp), color = Surface1, border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
                Column(Modifier.fillMaxWidth().padding(start = 12.dp, end = 8.dp, top = 12.dp)) {
                    BasicTextField(text, { text = it }, Modifier.fillMaxWidth().heightIn(min = 44.dp),
                        maxLines = 6, textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                        cursorBrush = SolidColor(Lime), decorationBox = { inner ->
                            Box {
                                if (text.isEmpty()) Text("Сообщение или поручение", color = Muted)
                                inner()
                            }
                        })
                    if (client != null || property != null) Text(
                        listOfNotNull(client?.name, property?.title).joinToString(" · ") + " · локально",
                        color = Muted, style = MaterialTheme.typography.labelSmall
                    )
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Box {
                            IconButton({ contextMenu = true }) { Icon(Icons.Default.Add, "Контекст и режимы") }
                            DropdownMenu(contextMenu, { contextMenu = false }) {
                                DropdownMenuItem({ Text("Очистить контекст") }, {
                                    client = null; property = null; mode = ""; contextMenu = false
                                })
                                Prompts.modes.forEach { (label, _) ->
                                    DropdownMenuItem({ Text(label) }, { mode = label; contextMenu = false })
                                }
                                props.take(20).forEach { item ->
                                    DropdownMenuItem({ Text(item.title) }, { property = item; contextMenu = false })
                                }
                                clients.take(20).forEach { item ->
                                    DropdownMenuItem({ Text(item.name) }, { client = item; contextMenu = false })
                                }
                            }
                        }
                        VoiceInput({ text = listOf(text, it).filter(String::isNotBlank).joinToString(" ") }, { vm.error = it }, compact = true)
                        Spacer(Modifier.weight(1f))
                        TextButton({
                            if (vm.cloudReady) agentConsent = true else openAgent(text.trim())
                        }, enabled = !vm.busy && text.isNotBlank()) { Text("Поручить") }
                        FilledIconButton(onClick = {
                            if (vm.busy) vm.stopGeneration()
                            else {
                                vm.ask(mode, text.trim(), property, client)
                                text = ""
                            }
                        }, enabled = vm.busy || (text.isNotBlank() && (vm.deviceReady || vm.cloudReady || vm.canSearch(text)))) {
                            Icon(if (vm.busy) Icons.Default.Close else Icons.Default.Send, if (vm.busy) "Остановить" else "Отправить")
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun CloudUsage(vm: MainVm) {
    val usage = vm.cloudQuota ?: return
    val detail = listOfNotNull(
        usage.usedTokens?.let { "$it токенов за ответ" },
        usage.remainingTokens?.let { "$it токенов в квоте" },
        usage.remainingRequests?.let { "$it запросов осталось" }
    )
    if (detail.isNotEmpty()) Text(detail.joinToString(" · "), Modifier.padding(bottom = 4.dp),
        style = MaterialTheme.typography.labelSmall, color = Muted)
}

private val boldPattern = Regex("""\*\*(.*?)\*\*""")
private val linkPattern = Regex("""https?://[^\s<>()\[\]]+""")

@Composable
private fun ResponseBody(markdown: String) {
    val browser = LocalUriHandler.current
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        markdown.lines().forEach { line ->
            val plain = line.trim()
            val table = plain.count { it == '|' } >= 2
            if (table && plain.all { it in " |-:" }) return@forEach
            val isHeading = plain.startsWith("#")
            val content = plain.trimStart('#').trimStart().replace(Regex("^[-*] "), "• ")
            val parsed = buildAnnotatedString {
                var start = 0
                boldPattern.findAll(content).forEach { match ->
                    append(content.substring(start, match.range.first))
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(match.groupValues[1]) }
                    start = match.range.last + 1
                }
                append(content.substring(start))
            }
            if (table) {
                Row(Modifier.horizontalScroll(rememberScrollState()).background(Surface1)) {
                    content.trim('|').split('|').forEach { cell ->
                        Text(cell.trim(), Modifier.width(150.dp).padding(8.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (content.isNotEmpty()) {
                Text(parsed, style = if (isHeading) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium)
            }
            linkPattern.findAll(content).map { it.value.trimEnd('.', ',', ';') }.distinct().forEach { url ->
                TextButton({ runCatching { browser.openUri(url) } }) {
                    Text(android.net.Uri.parse(url).host ?: "Открыть источник")
                }
            }
        }
    }
}
