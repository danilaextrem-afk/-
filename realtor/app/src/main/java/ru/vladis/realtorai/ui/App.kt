package ru.vladis.realtorai.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.Alignment
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
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
    val ctx = LocalContext.current
    val prefs = remember { ctx.getSharedPreferences("display", 0) }
    var theme by remember { mutableStateOf(prefs.getString("theme", "Системная") ?: "Системная") }
    val isDark = when (theme) { "Тёмная" -> true; "Светлая" -> false; else -> isSystemInDarkTheme() }
    RealtorTheme(isDark) {
        SideEffect {
            val window = (ctx as? android.app.Activity)?.window
            if (window != null) androidx.core.view.WindowCompat.getInsetsController(window, window.decorView).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
        AppRoutes(vm, theme) { selection -> theme = selection; prefs.edit().putString("theme", selection).apply() }
    }
}

@Composable private fun AppRoutes(vm: MainVm, theme: String, setTheme: (String) -> Unit) {
    val haptic = LocalHapticFeedback.current
    var command by rememberSaveable { mutableStateOf("") }
    var tab by rememberSaveable { mutableStateOf("home") }
    var sub by rememberSaveable { mutableStateOf<String?>(null) }
    var clientId by rememberSaveable { mutableStateOf<Long?>(null) }
    BackHandler(sub != null || clientId != null) { if (clientId != null) clientId = null else sub = null }
    val tabs = listOf(Triple("home", "Главная", Icons.Default.Home), Triple("props", "Объекты", Icons.Default.Place),
        Triple("crm", "CRM", Icons.Default.Person), Triple("tasks", "Задачи", Icons.Default.DateRange),
        Triple("ai", "ИИ", Icons.Default.Star))
    val wide = LocalConfiguration.current.screenWidthDp >= 700
    Scaffold(containerColor = Graphite, bottomBar = {
        if (!wide) Surface(color = Graphite) {
            Surface(shape = RoundedCornerShape(22.dp), color = Surface1,
                modifier = Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 6.dp)) {
                Row(Modifier.fillMaxWidth().selectableGroup().padding(5.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    tabs.forEach { (id, label, icon) ->
                        val selected = tab == id && sub == null && clientId == null
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(16.dp))
                                .selectable(selected, role = Role.Tab, onClick = {
                                    if (!selected) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    tab = id; sub = null; clientId = null
                                }).padding(vertical = 5.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Surface(shape = RoundedCornerShape(13.dp),
                                color = if (selected) MaterialTheme.colorScheme.primaryContainer else Surface1) {
                                Icon(icon, null, Modifier.padding(horizontal = 11.dp, vertical = 5.dp).size(19.dp),
                                    tint = if (selected) Lime else Muted)
                            }
                            Text(label, style = MaterialTheme.typography.labelSmall, color = if (selected) Lime else Muted)
                        }
                    }
                }
            }
        }
    }) { pad ->
        Row(Modifier.fillMaxSize().padding(pad)) {
            if (wide) NavigationRail(containerColor = Surface1) {
                tabs.forEach { (id, label, icon) ->
                    NavigationRailItem(selected = tab == id && sub == null && clientId == null,
                        onClick = { if (tab != id || sub != null) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); tab = id; sub = null; clientId = null },
                        icon = { Icon(icon, null) }, label = { Text(label) })
                }
            }
            Box(Modifier.weight(1f), contentAlignment = androidx.compose.ui.Alignment.TopCenter) {
                AnimatedContent(Triple(tab, sub, clientId), Modifier.widthIn(max = 880.dp).fillMaxSize(),
                    transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) }, label = "nav") { (t, s, c) ->
                    when {
                        c != null -> ClientDetail(vm, c) { clientId = null }
                        s?.startsWith("owner/") == true -> ru.vladis.realtorai.owner.OwnerScreen(vm, s.substringAfter('/').toLong(), { sub = "publish" }, { sub = null })
                        s == "agent" -> ru.vladis.realtorai.agent.AgentScreen(vm, command) { sub = null }
                        s == "autopilot" -> ru.vladis.realtorai.autopilot.AutopilotScreen(vm, { sub = "hunter" }, { tab = "crm"; sub = null }, { tab = "tasks"; sub = null }, { sub = null })
                        s == "analytics" -> Analytics(vm) { sub = null }
                        s == "settings" -> SettingsScreen(vm, theme, setTheme) { sub = null }
                        s == "models" -> ModelScreen(vm) { sub = null }
                        s == "publish" -> ru.vladis.realtorai.publish.PublishScreen(vm) { sub = null }
                        s == "sales" -> SalesWorkspace(vm) { sub = null }
                        s == "leads" -> LeadWorkspace(vm, { sub = null }, { clientId = it })
                        s == "hunter" || s == "map" -> ru.vladis.realtorai.hunter.HunterScreen(vm, s == "map") { sub = null }
                        else -> when (t) {
                            "home" -> Home(vm, { sub = "analytics" }, { sub = "settings" }, { sub = "publish" }, { sub = "hunter" }, { sub = "sales" }, { sub = "leads" }, { sub = "autopilot" },
                                { request -> command = request; sub = "agent" }, { tab = "props" }, { tab = "crm" }, { tab = "tasks" }, { sub = "owner/$it" })
                            "props" -> Properties(vm, { sub = "analytics" }, { sub = "owner/$it" }, { sub = "map" })
                            "crm" -> Crm(vm) { clientId = it }
                            "tasks" -> Tasks(vm)
                            else -> Assistant(vm) { sub = "models" }
                        }
                    }
                }
            }
        }
    }
}
