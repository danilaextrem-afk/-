package ru.vladis.realtorai.automation

import ru.vladis.realtorai.data.Client
import ru.vladis.realtorai.hunter.Offer


interface ListingSource {
    val name: String
    suspend fun search(query: String): List<Offer>
}

interface CrmConnector {
    val name: String
    suspend fun pushClient(c: Client): Result<Unit>
    suspend fun pullClients(): Result<List<Client>>
}

object IntegrationRegistry {
    val listingSources = listOf(AuctionSource("bankruptcy", "Банкротные торги"), AuctionSource("bank_collateral", "Банковские залоги"))
    val crmConnectors = mutableListOf<CrmConnector>()
}
