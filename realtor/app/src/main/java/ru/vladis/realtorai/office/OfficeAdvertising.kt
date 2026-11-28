package ru.vladis.realtorai.office

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Switch
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
import org.json.JSONArray
import org.json.JSONObject
import ru.vladis.realtorai.data.Property
import ru.vladis.realtorai.publish.Listing
import ru.vladis.realtorai.ui.Field

@Composable
internal fun OfficeAdvertisingPolicy(
    feed: OfficeFeed, policy: JSONObject, canEdit: Boolean, close: () -> Unit, save: (Boolean, Int) -> Unit
) {
    val configured = policy.optBoolean("feed_configured")
    var enabled by remember(policy) { mutableStateOf(policy.optBoolean("enabled")) }
    var limit by remember(policy) { mutableStateOf(policy.optInt("active_limit").toString()) }
    val count = policy.optInt("active_count")
    val newLimit = limit.toIntOrNull()
    AlertDialog(onDismissRequest = close, title = { Text(feed.label) }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(if (configured) "HTTPS-фид настроен на сервере" else "Офису нужно подключить HTTPS-фид")
            Text("Объектов в очереди: $count")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Передавать объекты")
                Switch(enabled, { enabled = it }, enabled = canEdit && configured)
            }
            if (canEdit) Field(limit, { limit = it.filter(Char::isDigit).take(4) },
                "Лимит объектов", keyboard = KeyboardType.Number)
            else Text("Лимит объектов: $limit")
        }
    }, confirmButton = {
        if (canEdit) Button({ save(enabled, requireNotNull(newLimit)) },
            enabled = newLimit != null && newLimit in 0..1000 &&
                (!enabled || configured && newLimit >= maxOf(1, count))) { Text("Сохранить") }
    }, dismissButton = { TextButton(close) { Text("Закрыть") } })
}

@Composable
internal fun OfficeAdvertisingOffer(
    feed: OfficeFeed, row: OfficeRow, offer: JSONObject, local: Property?, draft: Listing?, close: () -> Unit,
    approve: (JSONObject) -> Unit, withdraw: () -> Unit, reserve: (() -> Unit)?
) {
    val state = offer.getString("state")
    val active = state == "in_feed" || state == "feed_disabled"
    var consent by remember(row.id) { mutableStateOf("") }
    var phone by remember(row.id) { mutableStateOf("") }
    var description by remember(row.id) { mutableStateOf((draft?.description ?: local?.description).orEmpty().take(3000)) }
    var photos by remember(row.id) { mutableStateOf("") }
    var floor by remember(row.id) { mutableStateOf(local?.floor?.takeIf { it > 0 }?.toString().orEmpty()) }
    var floors by remember(row.id) { mutableStateOf(draft?.floorsTotal?.takeIf { it > 0 }?.toString().orEmpty()) }
    var rights by remember(row.id) { mutableStateOf(false) }
    val links = photos.lines().map(String::trim).filter(String::isNotEmpty)
    val floorNumber = floor.toIntOrNull()
    val floorsTotal = floors.toIntOrNull()
    val cian = feed == OfficeFeed.CIAN
    val descriptionValid = if (cian) description.trim().length >= 15 && '&' !in description else description.isNotBlank()
    val floorsValid = !cian || (floorNumber != null && floorsTotal != null && floorsTotal in floorNumber..150)
    AlertDialog(onDismissRequest = close, title = { Text(feed.label) }, text = {
        Column(Modifier.heightIn(max = 390.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(row.data.optString("title"))
            Text(when (state) {
                "in_feed" -> "В корпоративном фиде. Статус на площадке неизвестен."
                "feed_disabled" -> "Передача приостановлена офисом."
                "withdrawn_from_feed" -> "Исключён из корпоративного фида."
                else -> "Ещё не добавлен в корпоративный фид."
            })
            if (reserve != null) TextButton(reserve) { Text("Зарезервировать бюджет") }
            if (!active) {
                Field(consent, { consent = it.take(180) }, "Реквизиты согласия собственника")
                Field(phone, { phone = it.take(12) }, "Публичный телефон агента (+7…)", keyboard = KeyboardType.Phone)
                Field(floor, { floor = it.filter(Char::isDigit).take(3) }, "Этаж", keyboard = KeyboardType.Number)
                if (cian) Field(floors, { floors = it.filter(Char::isDigit).take(3) },
                    "Этажей в доме", keyboard = KeyboardType.Number)
                Field(description, { description = it.take(3000) }, "Описание объявления", single = false)
                if (cian) Text("Циан принимает от 15 символов. Символ & уберите из описания.")
                Field(photos, { photos = it.take(3600) }, "Разрешённые HTTPS-фото, по одному в строке", single = false)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Switch(rights, { rights = it })
                    Text("Собственник разрешил размещение и использование материалов")
                }
            }
        }
    }, confirmButton = {
        if (active) Button(withdraw) { Text("Убрать из фида") }
        else Button({
            approve(JSONObject().put("consent_reference", consent.trim()).put("phone", phone)
                .put("media_rights", rights).put("description", description.trim())
                .put("floor", floorNumber).put("images", JSONArray(links)).apply {
                    if (cian) put("floors_total", floorsTotal)
                })
        }, enabled = rights && consent.isNotBlank() && phone.matches(Regex("\\+7[0-9]{10}")) &&
            descriptionValid && floorsValid && floorNumber != null && floorNumber in 1..150 &&
            links.size in 1..10 && links.all { it.startsWith("https://") && it.length <= 512 }) {
            Text("Добавить в фид")
        }
    }, dismissButton = { TextButton(close) { Text("Закрыть") } })
}
