package ru.vladis.realtorai.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm
import ru.vladis.realtorai.data.pricePerSqm

@Composable fun Analytics(vm: MainVm, back: () -> Unit) {
    val props by vm.properties.collectAsState()
    var segment by remember { mutableStateOf("Все объекты") }
    val ranked = props.filter { segment == "Все объекты" || it.type == if (segment == "Новостройки") "primary" else "secondary" }.filter { it.area > 0 && it.price > 0 }.sortedBy { it.pricePerSqm() }
    var sel by remember { mutableStateOf(setOf<Long>()) }
    var price by remember { mutableStateOf("") }
    var agency by remember { mutableStateOf(vm.agencyPct.toString()) }
    var share by remember { mutableStateOf(vm.sharePct.toString()) }
    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Row(verticalAlignment = Alignment.CenterVertically) { IconButton(back) { Icon(Icons.Default.ArrowBack, "Назад") }; Title("Аналитика") } }
        item {
            AppCard {
                Text("Калькулятор комиссии", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Field(price, { price = it.filter(Char::isDigit) }, "Цена объекта, ₽", keyboard = KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Field(agency, { agency = it }, "Комиссия агентства, %", Modifier.weight(1f), KeyboardType.Decimal)
                    Field(share, { share = it }, "Доля агента, %", Modifier.weight(1f), KeyboardType.Decimal)
                }
                val total = (price.toLongOrNull() ?: 0L) * (agency.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100
                val mine = total * (share.replace(',', '.').toDoubleOrNull() ?: 0.0) / 100
                Spacer(Modifier.height(8.dp))
                Text("Комиссия агентства: ${total.rub()}", color = Muted)
                Text("Ваш доход: ${mine.rub()}", color = Lime, fontWeight = FontWeight.Bold)
            }
        }
        item {
            AppCard {
                Text("Цена за м²", fontWeight = FontWeight.SemiBold)
                Chips(listOf("Все объекты", "Новостройки", "Вторичка"), segment) { segment = it }
                if (ranked.isEmpty()) Text("Нужны объекты с ценой и площадью", color = Muted)
                else {
                    listOf("primary" to "Первичка", "secondary" to "Вторичка").forEach { (k, n) ->
                        val l = ranked.filter { it.type == k }
                        if (l.isNotEmpty()) Text("$n, среднее: ${(l.sumOf { it.pricePerSqm() } / l.size).rub()}/м²", color = Muted)
                    }
                    Spacer(Modifier.height(8.dp))
                    val max = ranked.last().pricePerSqm().toFloat().coerceAtLeast(1f)
                    ranked.take(10).forEach { p ->
                        val frac by animateFloatAsState((p.pricePerSqm() / max).coerceIn(0.05f, 1f), label = "bar")
                        Text("${p.title.take(28)} — ${p.pricePerSqm().rub()}", style = MaterialTheme.typography.bodySmall)
                        Box(Modifier.padding(bottom = 6.dp).height(8.dp).fillMaxWidth(frac).background(Lime, RoundedCornerShape(4.dp)))
                    }
                }
            }
        }
        item {
            AppCard {
                Text("Сравнение объектов (до 4)", fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                androidx.compose.foundation.layout.Column {
                    props.take(30).forEach { p ->
                        FilterChip(p.id in sel, { sel = if (p.id in sel) sel - p.id else if (sel.size < 4) sel + p.id else sel },
                            label = { Text(p.title.take(32)) }, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
                val chosen = props.filter { it.id in sel }
                if (chosen.size >= 2) {
                    val best = chosen.filter { it.pricePerSqm() > 0 }.minByOrNull { it.pricePerSqm() }?.id
                    @Composable fun row(label: String, f: (ru.vladis.realtorai.data.Property) -> String) = Row(Modifier.padding(vertical = 3.dp)) {
                        Text(label, Modifier.weight(1.1f), color = Muted, style = MaterialTheme.typography.bodySmall)
                        chosen.forEach { Text(f(it), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall,
                            color = if (label == "₽/м²" && it.id == best) Lime else MaterialTheme.colorScheme.onSurface) }
                    }
                    row("") { it.title.take(10) }
                    row("Цена") { "${it.price / 1000} т.₽" }
                    row("Площадь") { "${it.area} м²" }
                    row("₽/м²") { "${it.pricePerSqm() / 1000} т." }
                    row("Комнат") { it.rooms.toString() }
                    row("Доход") { "${vm.earning(it).toLong() / 1000} т.₽" }
                }
            }
        }
    }
}
