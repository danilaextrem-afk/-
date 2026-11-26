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
    val state = rememberLazyListState()
    LaunchedEffect(vm.chat.size) { if (vm.chat.isNotEmpty()) state.animateScrollToItem(vm.chat.size - 1) }
    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 16.dp)) {
        Row(Modifier.padding(top = 16.dp), verticalAlignment = Alignment.CenterVertically) {
            Title("ИИ-ассистент"); Spacer(Modifier.weight(1f))
            TextButton({ vm.clearChat() }) { Text("Очистить") }
        }
        if (vm.deviceReady) Text("Qwen 2.5 · на устройстве", color = Lime)
        else OutlinedButton(onClick = vm::installModel, enabled = vm.modelProgress == null) {
            Text(vm.modelProgress?.let { "Загрузка Qwen · $it%" } ?: "Скачать локальный ИИ · 547 МБ")
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
            if (vm.chat.isEmpty()) item { Text(
                if (vm.hasKey || vm.deviceReady) "Выберите режим, при необходимости прикрепите объект/клиента и опишите задачу." else "Локальный режим: шаблоны, подбор по базе и проверочные списки. Не языковая модель.", color = Muted) }
            items(vm.chat.size) { i ->
                val (role, msg) = vm.chat[i]
                val mine = role == "user"
                Column(Modifier.fillMaxWidth(), horizontalAlignment = if (mine) Alignment.End else Alignment.Start) {
                    Text(msg, Modifier.widthIn(max = 320.dp).background(if (mine) Color0 else Surface2, RoundedCornerShape(16.dp)).padding(12.dp),
                        color = if (mine) androidx.compose.ui.graphics.Color.Black else MaterialTheme.colorScheme.onSurface)
                    if (!mine) TextButton({ clip.setText(AnnotatedString(msg)) }) { Text("Копировать") }
                }
            }
            if (vm.busy) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
            vm.error?.let { e -> item { Text(e, color = MaterialTheme.colorScheme.error) } }
        }
        VoiceInput({ words -> text = listOf(text, words).filter { it.isNotBlank() }.joinToString(" ") }, { vm.error = it })
        Row(Modifier.padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Field(text, { text = it }, "Сообщение", Modifier.weight(1f), single = false)
            IconButton(enabled = text.isNotBlank() && !vm.busy, onClick = { vm.ask(mode, text.trim(), pSel, cSel); text = "" }) {
                Icon(Icons.Default.Send, "Отправить", tint = Lime)
            }
        }
    }
}
private val Color0 = Lime
