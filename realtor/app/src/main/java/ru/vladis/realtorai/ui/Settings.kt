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

@Composable fun SettingsScreen(vm: MainVm, theme: String, setTheme: (String) -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var model by remember { mutableStateOf(vm.model) }
    var agency by remember { mutableStateOf(vm.agencyPct.toString()) }
    var share by remember { mutableStateOf(vm.sharePct.toString()) }
    var status by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<android.net.Uri?>(null) }
    var exporting by remember { mutableStateOf(false) }
    var askPassword by remember { mutableStateOf(false) }
    var restoreLegacy by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    val exp = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        if (uri != null) { selected = uri; exporting = true; askPassword = true }
    }
    val imp = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) scope.launch {
            runCatching { vm.isEncryptedBackup(uri) }.onSuccess {
                selected = uri
                exporting = false
                if (it) askPassword = true else restoreLegacy = true
            }.onFailure { status = "Не удалось открыть копию" }
        }
    }

    fun transfer(password: CharArray?) {
        val uri = selected ?: return
        val save = exporting
        askPassword = false
        restoreLegacy = false
        busy = true
        scope.launch {
            try {
                if (save) vm.exportBackup(uri, requireNotNull(password)) else vm.importBackup(uri, password)
                status = if (save) "Защищённая копия сохранена" else "Данные восстановлены"
            } catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (e: Exception) {
                status = if (e is javax.crypto.AEADBadTagException) "Пароль неверен или архив повреждён"
                    else "Операция не завершена: " + (e.localizedMessage ?: "ошибка чтения")
            } finally { password?.fill('\u0000'); busy = false; selected = null }
        }
    }

    if (askPassword) {
        var password by remember { mutableStateOf("") }
        AlertDialog(onDismissRequest = { askPassword = false }, title = { Text(if (exporting) "Пароль резервной копии" else "Восстановить рабочую базу?") },
            text = { Column {
                if (!exporting) Text("Данные в приложении будут заменены содержимым копии.")
                Field(password, { password = it.take(256) }, "Пароль, от 8 символов", password = true)
            } }, confirmButton = { Button({ transfer(password.toCharArray()) }, enabled = password.length >= 8) {
                Text(if (exporting) "Зашифровать и сохранить" else "Восстановить")
            } }, dismissButton = { TextButton({ askPassword = false }) { Text("Отмена") } })
    }
    if (restoreLegacy) AlertDialog(onDismissRequest = { restoreLegacy = false }, title = { Text("Восстановить старую копию?") },
        text = { Text("Данные в приложении будут заменены содержимым копии.") },
        confirmButton = { Button({ transfer(null) }) { Text("Восстановить") } },
        dismissButton = { TextButton({ restoreLegacy = false }) { Text("Отмена") } })
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }; Title("Настройки") } }
        item {
            Text("Внешний вид", style = MaterialTheme.typography.titleLarge)
            Chips(listOf("Системная", "Тёмная", "Светлая"), theme, setTheme)
        }
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
                Text("Рабочая база и фотографии", color = Muted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton({ exp.launch("realtor-ai.raibak") }, enabled = !busy) { Text("Защищённый экспорт") }
                    OutlinedButton({ imp.launch(arrayOf("*/*")) }, enabled = !busy) { Text("Импорт") }
                }
                Text("Архив защищён паролем. Он включает CRM, поиск, цены, публикации, фотографии, сопровождение и поручения.", color = Muted, style = MaterialTheme.typography.bodySmall)
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
