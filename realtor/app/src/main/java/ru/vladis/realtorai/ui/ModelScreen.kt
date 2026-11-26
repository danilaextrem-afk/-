package ru.vladis.realtorai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.ai.DeviceModel
import ru.vladis.realtorai.ai.ModelSpec
import ru.vladis.realtorai.ai.models

@Composable
fun ModelScreen(vm: MainVm, back: () -> Unit) {
    val ctx = LocalContext.current
    var importing by remember { mutableStateOf<ModelSpec?>(null) }
    var removing by remember { mutableStateOf<ModelSpec?>(null) }
    val file = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) importing?.let { vm.importModel(it, uri) }
        importing = null
    }
    val measurement by DeviceModel.lastMeasurement.collectAsState()
    val recommend = vm.recommendedModel()
    LazyColumn(contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }
            ScreenHeading("Модели на устройстве", "Модели загружаются один раз и дальше работают офлайн")
        }
        item {
            Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (vm.deviceReady) "Активная модель" else "Рекомендация", style = MaterialTheme.typography.labelMedium)
                    Text(if (vm.deviceReady) vm.activeModel.label else recommend.label, style = MaterialTheme.typography.headlineSmall)
                    Text("Доступно ОЗУ сейчас: ${DeviceModel.availableMemory(ctx)} МБ")
                    measurement?.let { m ->
                        Text("${m.model} · ${m.backend}")
                        Text("Пик PSS процесса: ${m.peakPssMb} МиБ (+${m.deltaPssMb} МиБ)".plus(m.context?.let { " · контекст $it" }.orEmpty()), color = Muted)
                    }
                    if (vm.deviceReady && vm.activeModel != recommend && vm.hasModel(recommend)) {
                        TextButton({ vm.chooseModel(recommend) }, enabled = !vm.busy) { Text("Выбрать рекомендуемую") }
                    }
                    if (vm.deviceReady) TextButton(vm::benchmarkModel, enabled = !vm.busy) { Text("Сравнить установленные модели") }
                    else Button({ vm.installModel(recommend) }, enabled = vm.downloading == null) { Text("Скачать рекомендуемую") }
                    if (vm.benchmark.isNotBlank()) Text(vm.benchmark, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        vm.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
        item { Title("Каталог") }
        items(models, key = { it.file }) { spec ->
            val present = remember(spec, vm.modelRevision, vm.downloading, vm.activeModel) { vm.hasModel(spec) }
            Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(Modifier.padding(top = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(shape = CircleShape, color = Surface2) { Icon(Icons.Default.Star, null, Modifier.padding(9.dp).size(16.dp), tint = Lime) }
                    Column(Modifier.weight(1f)) {
                        Text(spec.label, style = MaterialTheme.typography.titleMedium)
                        Text("${spec.size / 1_000_000} МБ · свободно от ${DeviceModel.requiredMemory(ctx, spec)} МБ ОЗУ", color = Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text(spec.profile, color = Muted)
                when {
                    vm.downloading == spec -> {
                        LinearProgressIndicator(progress = { (vm.modelProgress ?: 0) / 100f }, modifier = Modifier.fillMaxWidth())
                        TextButton(vm::pauseModel) { Text("Пауза · ${vm.modelProgress}% ${vm.modelSpeed}") }
                    }
                    present -> Row {
                        FilledTonalButton({ vm.chooseModel(spec) }, enabled = vm.activeModel != spec && !vm.busy) { Text(if (vm.activeModel == spec) "Активна" else "Использовать") }
                        TextButton({ removing = spec }, enabled = !vm.busy && vm.downloading == null) { Text("Удалить") }
                    }
                    else -> Row {
                        FilledTonalButton({ vm.installModel(spec) }, enabled = vm.downloading == null) { Text(if (vm.partialModel(spec) > 0) "Продолжить" else "Скачать") }
                        if (spec.file.endsWith(".gguf")) TextButton({
                            importing = spec; file.launch(arrayOf("*/*"))
                        }, enabled = vm.downloading == null && !vm.busy) { Text("Импорт GGUF") }
                    }
                }
            }
        }
    }
    removing?.let { spec ->
        AlertDialog(onDismissRequest = { removing = null }, title = { Text("Удалить веса модели?") }, text = { Text(spec.label) },
            confirmButton = { TextButton({ vm.removeModel(spec); removing = null }) { Text("Удалить") } },
            dismissButton = { TextButton({ removing = null }) { Text("Отмена") } })
    }
}
