package ru.vladis.realtorai.office

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import ru.vladis.realtorai.ui.Field

@Composable
internal fun OfficeOwnerReport(
    row: OfficeRow, snapshot: JSONObject, busy: Boolean, close: () -> Unit,
    save: (Int, Long?, Long?, String) -> Unit, issue: () -> Unit, revoke: () -> Unit,
    problem: String?
) {
    val report = snapshot.getJSONObject("report")
    val revision = report.optInt("revision")
    val active = !snapshot.isNull("expires_at")
    fun count(key: String) = if (report.isNull(key)) "" else report.optLong(key).toString()
    val oldNote = report.optString("recommendation")
    var views by remember(row.id, revision) { mutableStateOf(count("views")) }
    var inquiries by remember(row.id, revision) { mutableStateOf(count("inquiries")) }
    var note by remember(row.id, revision) { mutableStateOf(oldNote) }
    var consent by remember(row.id, active) { mutableStateOf(false) }
    val changes = views != count("views") || inquiries != count("inquiries") || note.trim() != oldNote
    fun valid(value: String) = value.isBlank() || value.toLongOrNull()?.let { it in 0L..1_000_000_000L } == true

    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("Отчёт собственнику") }, text = {
        Column(Modifier.heightIn(max = 410.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(row.data.optString("title"), style = MaterialTheme.typography.titleMedium)
            Text("Введите только подтверждённые показатели. Пустые поля отображаются как «Нет данных».")
            Field(views, { views = it.filter(Char::isDigit).take(10) }, "Просмотры", keyboard = KeyboardType.Number)
            Field(inquiries, { inquiries = it.filter(Char::isDigit).take(10) }, "Обращения", keyboard = KeyboardType.Number)
            Field(note, { note = it.take(1000) }, "Рекомендация", single = false)
            if (active) TextButton(revoke, enabled = !busy) { Text("Отозвать действующую ссылку") }
            Text(if (active) "Новая ссылка заменит действующую." else "Ссылка действует 30 дней.")
            Checkbox(checked = consent, onCheckedChange = { consent = it }, enabled = !busy)
            Text("Собственник согласился получать отчёт по защищённой ссылке")
            TextButton(issue, enabled = !busy && consent && !changes) { Text("Создать ссылку") }
            if (changes && consent) Text("Сохраните правки перед созданием ссылки.")
            if (problem != null) Text(problem, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        Button({ save(revision, views.toLongOrNull(), inquiries.toLongOrNull(), note.trim()) },
            enabled = !busy && changes && valid(views) && valid(inquiries)) { Text("Сохранить отчёт") }
    }, dismissButton = { TextButton(close, enabled = !busy) { Text("Закрыть") } })
}

@Composable
internal fun OfficeOwnerLink(link: String, close: () -> Unit) {
    val context = LocalContext.current
    AlertDialog(onDismissRequest = close, title = { Text("Ссылка для собственника готова") },
        text = { Text("По ссылке открывается отчёт по этому объекту. Передайте её лично собственнику.") },
        confirmButton = {
            Button({
                val share = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, link)
                context.startActivity(Intent.createChooser(share, "Отправить отчёт собственнику"))
                close()
            }) { Text("Поделиться") }
        }, dismissButton = { TextButton(close) { Text("Закрыть") } })
}
