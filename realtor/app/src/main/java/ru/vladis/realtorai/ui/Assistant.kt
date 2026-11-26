package ru.vladis.realtorai.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ai.Prompts
import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.Property

@Composable fun Assistant(vm: MainVm) {
    val props by vm.properties.collectAsState()
    val clients by vm.clients.collectAsState()
    var mode by remember { mutableStateOf("Анализ") }
    var text by remember { mutableStateOf("") }
    var pSel by remember { mutableStateOf<Property?>(null) }
    var cSel by remember { mutableStateOf<Client?>(null) }
    var pMenu by remember { mutableStateOf(false) }
    var cMenu by remember { mutableStateOf(false) }
    val clip = LocalClipboardManager.current
    val speech = rememberLocalSpeech()

    val state = rememberLazyListState()
    LaunchedEffect(vm.chat.size) { if (vm.chat.isNotEmpty()) state.animateScrollToItem(vm.chat.size - 1) }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Title("ИИ-ассистент"); Spacer(Modifier.weight(1f))
            TextButton({ vm.clearChat() }) { Text("Очистить") }
        }
        var manager by remember { mutableStateOf(false) }
        OutlinedButton({ manager = true }) { Text(if (vm.deviceReady) vm.activeModel.label else "Скачать локальную модель") }
        if (manager) AlertDialog(onDismissRequest = { manager = false }, title = { Text("Модели на устройстве") },
            text = { LazyColumn {
                item {
                    Text("Рекомендуется при текущей памяти: " + vm.recommendedModel().label, color = Lime)
                    if (vm.deviceReady) TextButton(vm::benchmarkModel, enabled = !vm.busy) { Text("Тест производительности") }
                    if (vm.benchmark.isNotBlank()) Text(vm.benchmark, color = Muted)
                }
                items(ru.vladis.realtorai.ai.models.size) { i ->
                    val spec = ru.vladis.realtorai.ai.models[i]
                    Text(spec.label, style = MaterialTheme.typography.titleMedium)
                    Text("${spec.size / 1_000_000} МБ · свободно от ${spec.minimumFreeMb} МБ ОЗУ", color = Muted)
                    Text(spec.profile, color = Muted)
                    val present = remember(spec, vm.modelRevision, vm.downloading, vm.activeModel) { vm.hasModel(spec) }
                    if (vm.downloading == spec) {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        TextButton(vm::pauseModel) { Text("Пауза · ${vm.modelProgress}% · ${vm.modelSpeed}") }
                    } else if (present) {
                        TextButton({ vm.chooseModel(spec) }, enabled = vm.activeModel != spec && !vm.busy) { Text(if (vm.activeModel == spec) "Активна" else "Использовать") }
                        TextButton({ vm.removeModel(spec) }, enabled = !vm.busy && vm.downloading == null) { Text("Удалить веса " + spec.label) }
                    } else TextButton({ vm.installModel(spec) }, enabled = vm.downloading == null) {
                        Text(if (vm.partialModel(spec) > 0) "Продолжить загрузку" else "Скачать")
                    }
                }
                vm.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
            } }, confirmButton = { TextButton({ manager = false }) { Text("Закрыть") } })
        Chips(Prompts.modes.map { it.first }, mode) { mode = it }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box {
                AssistChip({ pMenu = true }, label = { Text(pSel?.title?.take(18) ?: "+ Объект") })
                DropdownMenu(pMenu, { pMenu = false }) {
                    DropdownMenuItem({ Text("Без объекта") }, { pSel = null; pMenu = false })
                    props.take(30).forEach { p -> DropdownMenuItem({ Text(p.title.take(40)) }, { pSel = p; pMenu = false }) }
                }
            }
            Box {
                AssistChip({ cMenu = true }, label = { Text(cSel?.name?.take(18) ?: "+ Клиент") })
                DropdownMenu(cMenu, { cMenu = false }) {
                    DropdownMenuItem({ Text("Без клиента") }, { cSel = null; cMenu = false })
                    clients.take(30).forEach { c -> DropdownMenuItem({ Text(c.name) }, { cSel = c; cMenu = false }) }
                }
            }
        }
        LazyColumn(Modifier.weight(1f).padding(vertical = 8.dp), state = state, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.chat.isEmpty()) item { Text(
                if (vm.hasKey || vm.deviceReady) "Выберите режим, при необходимости прикрепите объект/клиента и опишите задачу." else "Скачайте модель один раз. Сообщения и данные CRM обрабатываются на телефоне.", color = Muted) }
            items(vm.chat.size) { i ->
                val (role, msg) = vm.chat[i]
                val mine = role == "user"
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                    Text(msg, Modifier.widthIn(max = 320.dp).background(if (mine) Color0 else Surface2, RoundedCornerShape(16.dp)).padding(12.dp),
                        color = if (mine) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.onSurface)
                    if (!mine) Row {
                        TextButton({ clip.setText(AnnotatedString(msg)) }) { Text("Копировать") }
                        if (speech != null) TextButton({ speech.speak(msg.take(3500), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "reply-$i") }) { Text("Озвучить") }
                    }
                }
            }
            if (vm.busy) item {
                if (vm.streamingAnswer.isNotBlank()) Text(vm.streamingAnswer)
                LinearProgressIndicator(Modifier.fillMaxWidth())
                if (vm.cancellableGeneration) TextButton(vm::stopGeneration) { Text("Остановить генерацию") }
            }
            vm.error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
        }
        VoiceInput({ words -> text = listOf(text, words).filter { it.isNotBlank() }.joinToString(" ") }, { vm.error = it })
        Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(text, { text = it }, "Сообщение", Modifier.weight(1f), single = false)
            IconButton(enabled = text.isNotBlank() && vm.deviceReady && !vm.busy, onClick = { vm.ask(mode, text.trim(), pSel, cSel); text = "" }) {
                Icon(Icons.Default.Send, "Отправить", tint = Lime)
            }
        }
    }
}
private val Color0 = Lime
