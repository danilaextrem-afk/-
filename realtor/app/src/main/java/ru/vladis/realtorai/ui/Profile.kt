package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.MainVm

@Composable
fun Profile(vm: MainVm, open: (String) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(22.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Рабочее пространство", style = MaterialTheme.typography.labelMedium, color = Lime)
            Text("Профиль", style = MaterialTheme.typography.displaySmall)
        }
        item {
            OutlinedButton({ open("office") }, Modifier.fillMaxWidth()) { Text("Общая база офиса") }
        }
        item {
            HorizontalDivider()
            DashboardLine(Icons.Default.Settings, "Настройки", "Groq, оформление и копии базы") { open("settings") }
            DashboardLine(Icons.Default.Star, "Модели на устройстве", vm.activeModel.label) { open("models") }
            DashboardLine(Icons.Default.DateRange, "Календарь", "Встречи, звонки и напоминания") { open("tasks") }
        }
        item {
            Text("Инструменты", style = MaterialTheme.typography.titleLarge)
            HorizontalDivider(Modifier.padding(top = 10.dp))
            DashboardLine(Icons.Default.Place, "Недвижимость", "Ваши объекты и сопровождение") { open("properties") }
            DashboardLine(Icons.Default.PlayArrow, "Автопилот", "Расписание и журнал работы") { open("autopilot") }
            DashboardLine(Icons.Default.Share, "Публикации", "Черновики и выгрузка") { open("publish") }
            DashboardLine(Icons.Default.Person, "Лиды", "Входящие и импорт") { open("leads") }
            DashboardLine(Icons.Default.Call, "Продажи", "Подготовка к переговорам") { open("sales") }
            DashboardLine(Icons.Default.Info, "Аналитика", "Рынок и комиссия") { open("analytics") }
        }
    }
}
