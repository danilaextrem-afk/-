package ru.vladis.realtorai.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.filled.Star
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

@Composable fun Assistant(vm: MainVm, openModels: () -> Unit) {
    val props by vm.properties.collectAsState()
    val clients by vm.clients.collectAsState()
    var mode by remember { mutableStateOf("Анализ") }
    var text by androidx.compose.runtime.saveable.rememberSaveable { mutableStateOf("") }
    var pSel by remember { mutableStateOf<Property?>(null) }
    var cSel by remember { mutableStateOf<Client?>(null) }
    var pMenu by remember { mutableStateOf(false) }
    var cMenu by remember { mutableStateOf(false) }
    val clip = LocalClipboardManager.current
    val speech = rememberLocalSpeech()

    val state = rememberLazyListState()
    LaunchedEffect(vm.chat.size, vm.busy) {
        if (vm.chat.isNotEmpty()) state.animateScrollToItem(vm.chat.size - 1 + if (vm.busy) 1 else 0)
    }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Title("ИИ-ассистент"); Spacer(Modifier.weight(1f))
            TextButton({ vm.clearChat() }) { Text("Очистить") }
        }
        TextButton(openModels, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) {
            Text(if (vm.deviceReady) vm.activeModel.label + " · на устройстве" else "Выбрать локальную модель", color = Lime)
        }
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
            if (vm.chat.isEmpty()) item {
                Column(Modifier.fillMaxWidth().padding(top = 28.dp, bottom = 18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center) { Icon(Icons.Default.Star, null, tint = Lime) }
                    Text("Чем помочь?", style = MaterialTheme.typography.headlineMedium)
                    Text(if (vm.deviceReady) "Подключите карточку объекта или клиента, чтобы дать контекст."
                        else "Выберите модель. После загрузки разговор работает на телефоне.", color = Muted)
                    if (!vm.deviceReady) FilledTonalButton(openModels) { Text("Открыть модели") }
                    else listOf("Подготовь меня к показу квартиры", "Составь сообщение собственнику", "Помоги ответить на возражение").forEach { idea ->
                        Surface(onClick = { text = idea }, shape = RoundedCornerShape(12.dp), color = Surface1) {
                            Text(idea, Modifier.padding(12.dp), style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
            items(vm.chat.size) { i ->
                val (role, msg) = vm.chat[i]
                val mine = role == "user"
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                    if (!mine) Text("Realtor AI", style = MaterialTheme.typography.labelMedium, color = Lime, modifier = Modifier.padding(bottom = 6.dp))
                    val shape = RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = if (mine) 18.dp else 5.dp, bottomEnd = if (mine) 5.dp else 18.dp)
                    Text(msg, Modifier.widthIn(max = 440.dp).background(if (mine) MaterialTheme.colorScheme.primaryContainer else Surface1, shape).padding(14.dp),
                        color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.bodyMedium)
                    if (!mine) Row {
                        TextButton({ clip.setText(AnnotatedString(msg)) }) { Text("Копировать") }
                        if (speech != null) TextButton({ speech.speak(msg.take(3500), android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "reply-$i") }) { Text("Озвучить") }
                    }
                }
            }
            if (vm.busy) item {
                if (vm.streamingAnswer.isNotBlank()) Text(vm.streamingAnswer)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp)
                    Text("Готовлю ответ", color = Muted)
                }
                if (vm.cancellableGeneration) TextButton(vm::stopGeneration) { Text("Остановить генерацию") }
            }
            vm.error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
        }
        Surface(shape = RoundedCornerShape(22.dp), color = Surface1, modifier = Modifier.padding(bottom = 8.dp)) {
            Column(Modifier.padding(horizontal = 10.dp, vertical = 3.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextField(text, { text = it.take(400) }, Modifier.weight(1f),
                        placeholder = { Text("Сообщение") }, maxLines = 4, colors = TextFieldDefaults.colors(
                            focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                            focusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent,
                            unfocusedIndicatorColor = androidx.compose.ui.graphics.Color.Transparent))
                    FilledIconButton(enabled = text.isNotBlank() && (vm.deviceReady || vm.canSearch(text)) && !vm.busy, onClick = {
                        vm.ask(mode, text.trim(), pSel, cSel); text = ""
                    }) { Icon(Icons.Default.Send, "Отправить") }
                }
                VoiceInput({ words -> text = listOf(text, words).filter { it.isNotBlank() }.joinToString(" ").take(400) }, { vm.error = it })
            }
        }
    }
}
