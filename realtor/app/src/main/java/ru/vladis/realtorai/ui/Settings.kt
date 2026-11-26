package ru.vladis.realtorai.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.automation.IntegrationRegistry

@Composable fun SettingsScreen(vm: MainVm, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var model by remember { mutableStateOf(vm.model) }
    var agency by remember { mutableStateOf(vm.agencyPct.toString()) }
    var share by remember { mutableStateOf(vm.sharePct.toString()) }
    var status by remember { mutableStateOf("") }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            val s = vm.exportJson()
            withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)?.use { it.write(s.toByteArray()) } }
            status = "Экспорт выполнен"
        }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching {
                val s = withContext(Dispatchers.IO) { ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes().decodeToString() } }
                vm.importJson(s)
            }.onSuccess { status = "Данные восстановлены" }.onFailure { status = "Ошибка импорта файла" }
        }
    }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }; Title("Настройки") } }
        item { AppCard {
            Text("Локальный режим", fontWeight = FontWeight.SemiBold)
            Text("CRM, калькуляторы, шаблоны и анализ данных работают без API-ключей. Локальную Qwen можно скачать в разделе ИИ.", color = Muted)
        } }
        item {
            AppCard {
                Text("Комиссия", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Field(agency, { agency = it }, "Комиссия агентства, % от цены", keyboard = KeyboardType.Decimal)
                Spacer(Modifier.height(8.dp))
                Field(share, { share = it }, "Доля агента от комиссии, %", keyboard = KeyboardType.Decimal)
            }
        }
        item {
            Button({
                val a = agency.replace(',', '.').toDoubleOrNull(); val b = share.replace(',', '.').toDoubleOrNull()
                if (a == null || b == null || a !in 0.0..100.0 || b !in 0.0..100.0) status = "Введите значения от 0 до 100%"
                else { vm.saveSettings(null, model, a, b); status = "Настройки сохранены" }
            }, Modifier.fillMaxWidth()) { Text("Сохранить") }
        }
        item {
            AppCard {
                Text("Резервная копия", fontWeight = FontWeight.SemiBold)
                Text("JSON без API-ключа", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ exp.launch("realtor-ai-backup.json") }) { Text("Экспорт") }
                    OutlinedButton({ imp.launch(arrayOf("application/json", "*/*")) }) { Text("Импорт") }
                }
                Text("Импорт заменяет данные основной CRM; карточки публикаций и база поиска в эту копию не входят", color = Muted, style = MaterialTheme.typography.bodySmall)
            }
        }
        item {
            AppCard {
                Text("Интеграции", fontWeight = FontWeight.SemiBold)
                val n = IntegrationRegistry.listingSources.size + IntegrationRegistry.crmConnectors.size
                Text(if (n == 0) "Подключённых площадок и CRM нет. Доступен ручной импорт объявлений (Объекты → Импорт)." else
                    (IntegrationRegistry.listingSources.map { it.name } + IntegrationRegistry.crmConnectors.map { it.name }).joinToString(), color = Muted)
            }
        }
        if (status.isNotBlank()) item { Text(status, color = Lime) }
    }
}
