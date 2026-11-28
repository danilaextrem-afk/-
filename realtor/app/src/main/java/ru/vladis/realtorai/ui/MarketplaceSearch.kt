package ru.vladis.realtorai.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import ru.vladis.realtorai.agent.ListingQuery
import ru.vladis.realtorai.agent.PropertyIndex

@Composable
internal fun MarketplaceSearchButton(query: ListingQuery) {
    val browser = LocalUriHandler.current
    var chooseSite by remember { mutableStateOf(false) }
    var openError by remember { mutableStateOf(false) }

    OutlinedButton({ openError = false; chooseSite = true }, Modifier.fillMaxWidth()) {
        Text("Поиск по площадкам без аккаунта")
    }
    if (chooseSite) AlertDialog(
        onDismissRequest = { chooseSite = false },
        title = { Text("Где искать квартиру") },
        text = {
            Column(Modifier.heightIn(max = 380.dp).verticalScroll(rememberScrollState())) {
                Text("Откроется поиск по выбранному сайту в браузере. Условия и актуальность проверьте в объявлении.")
                if (openError) Text("Не удалось открыть браузер.", color = MaterialTheme.colorScheme.error)
                PropertyIndex.browserSites.forEach { site ->
                    TextButton({
                        runCatching { browser.openUri(PropertyIndex.browserSearch(site, query)) }
                            .onSuccess { chooseSite = false }
                            .onFailure { openError = true }
                    }) { Text(site.name) }
                }
            }
        },
        confirmButton = { TextButton({ chooseSite = false }) { Text("Закрыть") } }
    )
}
