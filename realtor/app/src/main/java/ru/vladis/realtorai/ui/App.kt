package ru.vladis.realtorai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.vladis.realtorai.MainVm

@Composable fun App(vm: MainVm) {
    var tab by rememberSaveable { mutableStateOf("home") }
    var sub by rememberSaveable { mutableStateOf<String?>(null) }
    var clientId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(sub != null || clientId != null) { if (clientId != null) clientId = null else sub = null }
    val tabs = listOf(Triple("home", "Главная", Icons.Default.Home), Triple("props", "Объекты", Icons.Default.Place),
        Triple("crm", "CRM", Icons.Default.Person), Triple("tasks", "Задачи", Icons.Default.DateRange),
        Triple("ai", "ИИ", Icons.Default.Star))
    val wide = LocalConfiguration.current.screenWidthDp >= 700
    Scaffold(containerColor = Graphite, bottomBar = {
        if (!wide) NavigationBar(containerColor = Surface1) {
            tabs.forEach { (id, label, icon) ->
                NavigationBarItem(selected = tab == id && sub == null && clientId == null,
                    onClick = { tab = id; sub = null; clientId = null },
                    icon = { Icon(icon, null) }, label = { Text(label) }, colors = NavigationBarItemDefaults.colors(indicatorColor = Surface2, selectedIconColor = Lime, selectedTextColor = Lime, unselectedIconColor = Muted, unselectedTextColor = Muted))
            }
        }
    }) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            if (wide) NavigationRail(containerColor = Surface1) {
                tabs.forEach { (id, label, icon) ->
                    NavigationRailItem(selected = tab == id && sub == null && clientId == null,
                        onClick = { tab = id; sub = null; clientId = null },
                        icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
            Box(Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                AnimatedContent(Triple(tab, sub, clientId), Modifier.widthIn(max = 880.dp).fillMaxSize(),
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "nav") { (t, s, c) ->
                    when {
                        c != null -> ClientDetail(vm, c) { clientId = null }
                        s == "autopilot" -> ru.vladis.realtorai.autopilot.AutopilotScreen(vm, { sub = "hunter" }, { tab = "crm"; sub = null }, { tab = "tasks"; sub = null }, { sub = null })
                        s == "analytics" -> Analytics(vm) { sub = null }
                        s == "settings" -> SettingsScreen(vm) { sub = null }
                        s == "publish" -> ru.vladis.realtorai.publish.PublishScreen(vm) { sub = null }
                        s == "sales" -> SalesWorkspace(vm) { sub = null }
                        s == "leads" -> LeadWorkspace(vm, { sub = null }, { clientId = it })
                        s == "hunter" -> ru.vladis.realtorai.hunter.HunterScreen(vm) { sub = null }
                        else -> when (t) {
                            "home" -> Home(vm, { sub = "analytics" }, { sub = "settings" }, { sub = "publish" }, { sub = "hunter" }, { sub = "sales" }, { sub = "leads" }, { sub = "autopilot" })
                            "props" -> Properties(vm) { sub = "analytics" }
                            "crm" -> Crm(vm) { clientId = it }
                            "tasks" -> Tasks(vm)
                            else -> Assistant(vm)
                        }
                    }
                }
            }
        }
    }
}
