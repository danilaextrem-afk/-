package ru.vladis.realtorai.automation

import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.data.Property

/** Точки расширения. Реальных интеграций НЕТ — реестры пусты, используется ручной импорт. */
interface ListingSource {
    val name: String
    suspend fun search(query: String): List<Property>
}

interface CrmConnector {
    val name: String
    suspend fun pushClient(c: Client): Result<Unit>
    suspend fun pullClients(): Result<List<Client>>
}

object IntegrationRegistry {
    val listingSources = mutableListOf<ListingSource>()
    val crmConnectors = mutableListOf<CrmConnector>()
}
