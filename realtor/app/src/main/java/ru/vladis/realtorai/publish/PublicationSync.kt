package ru.vladis.realtorai.publish

import android.content.Context
import androidx.room.withTransaction
import androidx.work.*
import kotlinx.coroutines.CancellationException
import ru.vladis.realtorai.data.WorkspaceDb

internal object PublicationSync {
    suspend fun stage(dao: PublishDao, id: Long, sold: Boolean) {
        for (pub in dao.pubsOf(id)) {
            if (pub.status !in setOf("published", "exported", "outdated", "removal_pending")) continue
            val platform = Platform.valueOf(pub.platform)
            val message = if (platform.isApi) "Ожидается синхронизация через API"
                else if (sold) "Объект исключён из выгрузки. Обновите фид в кабинете площадки."
                else "Выгрузите фид с новой ценой."
            dao.savePub(pub.copy(status = if (sold) "removal_pending" else "outdated", message = message,
                updatedAt = System.currentTimeMillis()))
        }
    }

    fun enqueue(ctx: Context, id: Long) {
        val request = OneTimeWorkRequestBuilder<PublicationSyncWorker>()
            .setInputData(workDataOf("listing" to id))
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork("publish-sync-$id", ExistingWorkPolicy.APPEND_OR_REPLACE, request)
    }
}

class PublicationSyncWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    override suspend fun doWork(): Result {
        val db = WorkspaceDb.get(applicationContext)
        val dao = db.publications()
        val id = inputData.getLong("listing", -1)
        val listing = dao.listing(id) ?: return Result.success()
        val secrets = PublishSecrets(applicationContext)
        for (pub in dao.pubsOf(id)) {
            val platform = Platform.valueOf(pub.platform)
            val removing = listing.status == "sold" && pub.status == "removal_pending"
            val repricing = listing.status != "sold" && pub.status == "outdated"
            if (!platform.isApi || pub.externalId.isBlank() || (!removing && !repricing)) continue
            if (repricing && listing.approvedAt == null) {
                dao.savePub(pub.copy(message = "Требуется подтверждение согласия собственника"))
                continue
            }
            val publisher = secrets.publisher(platform)
            val reply = if (publisher == null) null else try {
                if (removing) publisher.unpublish(listing, pub) else publisher.updatePrice(listing, pub)
            } catch (e: CancellationException) { throw e }
            catch (_: Exception) { null }
            db.withTransaction {
                val current = dao.listing(id)
                if (current != listing || dao.pubsOf(id).none { it == pub }) return@withTransaction
                if (reply == null) dao.savePub(pub.copy(message = if (publisher == null) "Площадка не подключена"
                    else "Площадка не подтвердила изменение. Проверьте статус перед повтором."))
                else dao.savePub(pub.copy(status = reply.status, externalId = reply.externalId.ifBlank { pub.externalId },
                    meta = reply.meta.ifBlank { pub.meta }, url = reply.url.ifBlank { pub.url }, message = reply.message,
                    price = listing.price, updatedAt = System.currentTimeMillis()))
            }
        }
        return Result.success()
    }
}
