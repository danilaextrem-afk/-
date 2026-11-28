package ru.vladis.realtorai.office

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONObject
import ru.vladis.realtorai.ui.Field
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale
import java.util.UUID

private val moneyInput = Regex("[0-9]{1,11}(?:[,.][0-9]{1,2})?")
private val receiptInput = Regex("[A-Za-z0-9][A-Za-z0-9_.:/-]{0,119}")

private fun String.minorUnits(): Long? =
    if (!matches(moneyInput)) null
    else replace(',', '.').toBigDecimalOrNull()?.movePointRight(2)?.toLong()?.takeIf { it <= 1_000_000_000_000 }

private fun Long.money(): String = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("ru-RU"))
    .format(BigDecimal.valueOf(this, 2))

private fun Long.inputMoney(): String = BigDecimal.valueOf(this, 2).stripTrailingZeros().toPlainString()

@Composable
internal fun OfficeReservePromotion(
    feed: OfficeFeed, row: OfficeRow, busy: Boolean, problem: String?, close: () -> Unit,
    reserve: (String, Long) -> Unit
) {
    val id = remember(row.id, feed) { UUID.randomUUID().toString() }
    var amount by remember { mutableStateOf("") }
    val value = amount.minorUnits()
    AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("Резерв на продвижение") }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("${row.data.optString("title")} · ${feed.label}")
            Text("Резерв уменьшает доступный лимит офиса. Покупку оформляют в кабинете площадки.")
            Field(amount, { amount = it.take(14) }, "Сумма, ₽", keyboard = KeyboardType.Decimal)
            if (problem != null) Text(problem, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        Button({ reserve(id, requireNotNull(value)) }, enabled = !busy && value != null && value > 0) {
            Text("Зарезервировать")
        }
    }, dismissButton = { TextButton(close, enabled = !busy) { Text("Отмена") } })
}

@Composable
internal fun OfficePromotionBudget(
    summary: JSONObject, entries: List<JSONObject>, canEdit: Boolean, busy: Boolean, problem: String?,
    close: () -> Unit, saveLimit: (Long) -> Unit, nextPage: (() -> Unit)?,
    settle: (JSONObject, Long, String) -> Unit, release: (JSONObject) -> Unit
) {
    val currentLimit = summary.getLong("limit_minor")
    var limit by remember(currentLimit) { mutableStateOf(currentLimit.inputMoney()) }
    var decision by remember { mutableStateOf<Pair<JSONObject, Boolean>?>(null) }
    val value = limit.minorUnits()
    if (decision == null) {
        AlertDialog(onDismissRequest = { if (!busy) close() }, title = { Text("Бюджет продвижения") }, text = {
            Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Доступно ${summary.getLong("available_minor").money()}", style = MaterialTheme.typography.titleMedium)
                Text("В резерве ${summary.getLong("reserved_minor").money()} · Учтено ${summary.getLong("spent_minor").money()}")
                if (canEdit) Field(limit, { limit = it.take(14) }, "Лимит, ₽", keyboard = KeyboardType.Decimal)
                else Text("Лимит ${currentLimit.money()}")
                if (problem != null) Text(problem, color = MaterialTheme.colorScheme.error)
                if (entries.isEmpty()) Text("Операций пока нет")
                entries.forEach { entry ->
                    val amount = entry.getLong("reserved_minor")
                    val feed = OfficeFeed.entries.firstOrNull { it.route == entry.optString("channel") }
                    Text("${entry.getString("property_title")} · ${feed?.label.orEmpty()}",
                        style = MaterialTheme.typography.titleSmall)
                    when (entry.getString("status")) {
                        "reserved" -> {
                            Text("Резерв ${amount.money()}")
                            if (canEdit) {
                                TextButton({ decision = entry to true }, enabled = !busy) { Text("Подтвердить расход") }
                                TextButton({ decision = entry to false }, enabled = !busy) { Text("Освободить резерв") }
                            }
                        }
                        "settled" -> Text("Учтено ${entry.getLong("actual_minor").money()} · № ${entry.getString("receipt")}")
                        else -> Text("Освобождён резерв ${amount.money()}")
                    }
                }
                if (nextPage != null) TextButton(nextPage, enabled = !busy) { Text("Показать ещё") }
            }
        }, confirmButton = {
            if (canEdit) Button({ saveLimit(requireNotNull(value)) },
                enabled = !busy && value != null && value != currentLimit) { Text("Сохранить лимит") }
        }, dismissButton = { TextButton(close, enabled = !busy) { Text("Закрыть") } })
    }
    decision?.let { (entry, charge) ->
        val stillOpen = entries.any { it.getString("id") == entry.getString("id") && it.getString("status") == "reserved" }
        if (!stillOpen) {
            androidx.compose.runtime.LaunchedEffect(entry) { decision = null }
            return@let
        }
        var amount by remember(entry) { mutableStateOf(entry.getLong("reserved_minor").inputMoney()) }
        var receipt by remember(entry) { mutableStateOf("") }
        val charged = amount.minorUnits()
        AlertDialog(onDismissRequest = { if (!busy) decision = null },
            title = { Text(if (charge) "Подтвердить расход" else "Освободить резерв") }, text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (charge) {
                        Text("Учтите сумму, которую уже списала площадка.")
                        Field(amount, { amount = it.take(14) }, "Фактическая сумма, ₽", keyboard = KeyboardType.Decimal)
                        Field(receipt, { receipt = it.take(120) }, "Номер документа площадки")
                    } else Text("Освободите средства, если площадка их не списала.")
                    if (problem != null) Text(problem, color = MaterialTheme.colorScheme.error)
                }
            }, confirmButton = {
                Button({
                    if (charge) settle(entry, requireNotNull(charged), receipt.trim()) else release(entry)
                }, enabled = !busy && (!charge || (charged != null && charged > 0 && receipt.trim().matches(receiptInput)))) {
                    Text("Подтвердить")
                }
            }, dismissButton = { TextButton({ decision = null }, enabled = !busy) { Text("Отмена") } })
    }
}
