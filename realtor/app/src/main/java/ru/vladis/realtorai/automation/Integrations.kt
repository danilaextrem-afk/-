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
    fun enabled(ctx: android.content.Context): List<ListingSource> {
        val prefs = ctx.getSharedPreferences("autopilot", 0)
        return listingSources.filter { prefs.getBoolean(it.name, true) }
    }
    val crmConnectors = mutableListOf<CrmConnector>()
}
