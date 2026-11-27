package ru.vladis.realtorai.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.hunter.ListingOrigin
import ru.vladis.realtorai.local.CallOutcome
import ru.vladis.realtorai.local.CallWorkflow
import java.time.LocalDateTime
import java.time.ZoneId

@Composable
internal fun CallScreen(vm: MainVm, propertyId: Long, openCalendar: () -> Unit, back: () -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val properties by vm.properties.collectAsState()
    val clients by vm.clients.collectAsState()
    val offers by vm.sourceOffers.collectAsState()
    val property = properties.firstOrNull { it.id == propertyId }
    val offer = offers.firstOrNull { it.id == property?.sourceOfferId }
    val owner = clients.firstOrNull { it.id == property?.ownerId }
    val phone = ListingOrigin.dialable(owner?.phone) ?: offer?.takeIf { it.seller == "owner" }
        ?.let { ListingOrigin.dialable(it.contactPhone ?: it.ownerPhone) }
    val optedOut = property?.ownerContactBlocked == true || owner?.doNotContact == true ||
        (phone != null && !CallWorkflow.canDial(property, phone, clients))
    var script by remember(propertyId) { mutableStateOf("") }
    var issue by remember(propertyId) { mutableStateOf<String?>(null) }
    var result by remember(propertyId) { mutableStateOf<CallOutcome?>(null) }
    var note by remember(propertyId) { mutableStateOf("") }
    var schedule by remember(propertyId) { mutableStateOf<LocalDateTime?>(null) }
    var busy by remember(propertyId) { mutableStateOf(false) }
    val objections = listOf("Я сам продам", "С агентами не работаю", "Не хочу платить комиссию",
        "У вас есть покупатель?", "Уже работаю с агентством", "Не собираюсь снижать цену")
    fun prepare(objection: String) {
        val current = property ?: return
        busy = true
        scope.launch {
            try { script = vm.prepareCall(current, objection); issue = null }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { issue = e.message ?: "Не удалось подготовить звонок" }
            finally { busy = false }
        }
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { TextButton(back) { Text("Назад") }; ScreenHeading("Звонок собственнику", property?.title.orEmpty()) }
        if (property == null) return@LazyColumn
        item {
            if (optedOut) Text("Контакт отказался от дальнейших звонков", color = MaterialTheme.colorScheme.error)
            else if (phone == null) Text("В CRM и в источнике нет номера собственника", color = Muted)
            else Button({
                runCatching { ctx.startActivity(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", phone, null))) }
                    .onFailure { issue = "Приложение для звонков недоступно" }
            }) { Text("Открыть номер в телефоне") }
            Text(listOfNotNull(property.price.takeIf { it > 0 }?.rub(), property.area.takeIf { it > 0 }?.let { "$it м²" },
                property.address.takeIf(String::isNotBlank)).joinToString(" · "), color = Muted)
        }
        item {
            Text("Подготовка к разговору", style = MaterialTheme.typography.titleLarge)
            TextButton({ prepare("") }, enabled = !busy && !optedOut) { Text("Подготовить сценарий") }
            if (!optedOut) Chips(objections, "") { if (!busy) prepare(it) }
            if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (script.isNotBlank()) Text(script)
        }
        if (!optedOut) item {
            Text("Итог звонка", style = MaterialTheme.typography.titleLarge)
            Chips(CallOutcome.entries.map { it.title }, result?.title.orEmpty()) { selected ->
                result = CallOutcome.entries.first { it.title == selected }
                schedule = null
            }
            if (result != null) Field(note, { note = it.take(2_000) }, "Заметка", single = false)
            val needsTime = result in listOf(CallOutcome.CALLBACK, CallOutcome.MEETING)
            if (needsTime) OutlinedButton({
                val initial = LocalDateTime.now().plusDays(1).withHour(12).withMinute(0)
                DatePickerDialog(ctx, { _, year, month, day ->
                    TimePickerDialog(ctx, { _, hour, minute ->
                        schedule = LocalDateTime.of(year, month + 1, day, hour, minute)
                    }, initial.hour, initial.minute, true).show()
                }, initial.year, initial.monthValue - 1, initial.dayOfMonth).show()
            }) { Text(schedule?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()?.let(::fmtDate) ?: "Дата и время") }
            if (result != null) Button({
                val selected = requireNotNull(result)
                val due = schedule?.atZone(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
                busy = true
                scope.launch {
                    try {
                        vm.recordCall(propertyId, selected, note, due)
                        result = null
                        note = ""
                        issue = "Результат сохранён"
                        if (selected == CallOutcome.MEETING) openCalendar()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { issue = e.message ?: "Не удалось сохранить результат" }
                    finally { busy = false }
                }
            }, enabled = !busy && (!needsTime || schedule != null)) { Text("Сохранить итог") }
        }
        issue?.let { message -> item { Text(message, color = Muted) } }
    }
}
