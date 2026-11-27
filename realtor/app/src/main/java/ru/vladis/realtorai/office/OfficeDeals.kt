package ru.vladis.realtorai.office

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import org.json.JSONObject
import ru.vladis.realtorai.ui.AppCard
import ru.vladis.realtorai.ui.Lime
import ru.vladis.realtorai.ui.Muted
import ru.vladis.realtorai.ui.rub

internal enum class DealStage(val key: String, val label: String) {
    QUALIFICATION("qualification", "Согласование"),
    SHOWING("showing", "Показ"),
    DEPOSIT("deposit", "Задаток"),
    CLOSED("closed", "Закрыта"),
    CANCELLED("cancelled", "Отменена");

    val next get() = when (this) {
        QUALIFICATION -> SHOWING
        SHOWING -> DEPOSIT
        DEPOSIT -> CLOSED
        CLOSED, CANCELLED -> null
    }
}

internal fun officeDealData(property: String, buyer: String, stage: DealStage, amount: Long = 0, commission: Long = 0) =
    JSONObject().put("property_id", property).put("buyer_id", buyer).put("stage", stage.key)
        .put("amount", amount).put("commission", commission)

@Composable
internal fun OfficeDealCard(row: OfficeRow, enabled: Boolean, onStage: (DealStage) -> Unit) {
    val stage = DealStage.entries.firstOrNull { it.key == row.data.optString("stage") }
    AppCard {
        Text(row.propertyTitle, style = MaterialTheme.typography.titleMedium)
        Text(row.buyerName, color = Muted)
        stage?.let { Text(it.label, color = Lime) }
        Text("Ответственный: ${row.assignedTo}", color = Muted)
        if (stage == DealStage.CLOSED) {
            Text("Цена сделки: ${row.data.optLong("amount").rub()}")
            Text("Комиссия: ${row.data.optLong("commission").rub()}")
        }
        stage?.next?.let { next ->
            TextButton({ onStage(next) }, enabled = enabled) {
                Text(if (next == DealStage.CLOSED) "Закрыть сделку" else "Следующий этап: ${next.label}")
            }
            TextButton({ onStage(DealStage.CANCELLED) }, enabled = enabled) { Text("Отменить сделку") }
        }
    }
}

@Composable
internal fun OfficeDealPicker(client: OfficeClient, dismiss: () -> Unit, open: (OfficeRow, OfficeRow) -> Unit) {
    var buyer by remember { mutableStateOf<OfficeRow?>(null) }
    var property by remember { mutableStateOf<OfficeRow?>(null) }
    val section = if (buyer == null) OfficeSection.BUYERS else OfficeSection.PROPERTIES
    var records by remember(section) { mutableStateOf(emptyList<OfficeRow>()) }
    var after by remember(section) { mutableStateOf("") }
    var next by remember(section) { mutableStateOf("") }
    var attempt by remember(section) { mutableIntStateOf(0) }
    var loading by remember(section) { mutableStateOf(true) }
    var error by remember(section) { mutableStateOf<String?>(null) }

    LaunchedEffect(client, section, after, attempt) {
        loading = true
        error = null
        try {
            val page = client.records(section, after)
            records = (records + page.rows).distinctBy { it.id }
            next = page.next
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            error = "Не удалось загрузить список офиса."
        } finally {
            loading = false
        }
    }

    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(when { property != null -> "Открыть сделку?"; buyer == null -> "Выберите покупателя"; else -> "Выберите объект" }) },
        text = {
            val selected = property
            if (selected != null) {
                Text("${selected.data.getString("title")} · ${requireNotNull(buyer).data.getString("name")}")
            } else {
                LazyColumn(Modifier.heightIn(max = 360.dp)) {
                    items(records, key = { it.id }) { row ->
                        TextButton({
                            if (buyer == null) buyer = row else property = row
                        }) { Text(row.data.getString(if (buyer == null) "name" else "title")) }
                    }
                    if (loading) item { LinearProgressIndicator(Modifier.fillMaxWidth()) }
                    error?.let { problem -> item {
                        Text(problem)
                        TextButton({ attempt++ }) { Text("Повторить") }
                    } }
                    if (!loading && error == null && records.isEmpty()) item {
                        Text(if (buyer == null) "Сначала добавьте покупателя в общую базу офиса."
                        else "Сначала добавьте объект в общую базу офиса.")
                    }
                    if (!loading && error == null && next.isNotEmpty()) item {
                        TextButton({ after = next }) { Text("Показать ещё") }
                    }
                }
            }
        },
        confirmButton = {
            property?.let { selected ->
                Button({ open(selected, requireNotNull(buyer)) }) { Text("Открыть сделку") }
            }
        },
        dismissButton = {
            TextButton({
                when { property != null -> property = null; buyer != null -> buyer = null; else -> dismiss() }
            }) { Text(if (buyer == null) "Отмена" else "Назад") }
        }
    )
}

@Composable
internal fun OfficeDealTransition(
    row: OfficeRow, stage: DealStage, dismiss: () -> Unit, confirm: (Long, Long) -> Unit
) {
    var price by remember(row.id, stage) { mutableStateOf("") }
    var fee by remember(row.id, stage) { mutableStateOf("") }
    val amount = price.toLongOrNull() ?: 0L
    val commission = fee.toLongOrNull() ?: 0L
    val closes = stage == DealStage.CLOSED
    AlertDialog(
        onDismissRequest = dismiss,
        title = { Text(if (closes) "Закрыть сделку" else "Подтвердить этап") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(row.propertyTitle)
                if (closes) {
                    OutlinedTextField(price, { price = it.filter { c -> c in '0'..'9' }.take(13) },
                        label = { Text("Цена сделки, ₽") }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(fee, { fee = it.filter { c -> c in '0'..'9' }.take(13) },
                        label = { Text("Комиссия, ₽") }, singleLine = true, isError = commission > amount,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                } else Text("Новый этап: ${stage.label}")
            }
        },
        confirmButton = { Button({ confirm(amount, commission) },
            enabled = !closes || (amount in 1L..1_000_000_000_000L && commission <= amount)) { Text("Подтвердить") } },
        dismissButton = { TextButton(dismiss) { Text("Назад") } }
    )
}
