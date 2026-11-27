package ru.vladis.realtorai.publish

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PublishVm(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val db = ru.vladis.realtorai.data.WorkspaceDb.get(app)
    private val dao = db.publications()
    val secrets = PublishSecrets(app)
    val listings = dao.listings().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pubs = dao.publications().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var busy by mutableStateOf(false); private set
    var log by mutableStateOf<String?>(null)

    private fun cfg() = Feeds.Cfg(secrets.get("photo_base"), secrets.get("phone"))
    private suspend fun record(l: Listing, pl: Platform, r: PubResult) {
        dao.savePub(Publication(listingId = l.id, platform = pl.name, status = r.status, externalId = r.externalId,
            meta = r.meta, url = r.url, message = r.message, price = l.price))
    }
    private fun err(e: Throwable): String {
        var msg = e.message ?: e.javaClass.simpleName
        for (key in listOf("tg_token", "vk_token")) {
            val secret = secrets.get(key)
            if (secret.isNotBlank()) msg = msg.replace(secret, "[скрыто]")
        }
        return msg
    }
    private val operationLock = kotlinx.coroutines.sync.Mutex()
    private fun operation(block: suspend () -> Unit) = viewModelScope.launch {
        operationLock.lock()
        busy = true; log = null
        try { block() }
        catch (e: kotlinx.coroutines.CancellationException) { throw e }
        catch (e: Exception) { log = err(e) }
        finally { busy = false; operationLock.unlock() }
    }

    fun create(l: Listing, onId: (Long) -> Unit) = viewModelScope.launch { onId(dao.saveListing(l)) }
    fun save(l: Listing) = operation {
        val current = dao.listing(l.id) ?: return@operation
        val updated = l.copy(status = current.status, photos = current.photos, approvedAt = current.approvedAt,
            cover = l.cover.takeIf { it in current.photoList() } ?: current.cover, updatedAt = current.updatedAt)
        if (updated != current) dao.saveListing(updated.copy(
            approvedAt = current.approvedAt.takeIf { updated.copy(price = current.price) == current },
            updatedAt = System.currentTimeMillis()))

    }
    fun delete(l: Listing) = operation {
        if (dao.pubsOf(l.id).any { it.status in setOf("published", "outdated", "removal_pending", "exported", "unknown") }) {
            log = "Сначала проверьте и снимите публикации; история не удалена."
            return@operation
        }
        dao.deletePubs(l.id); dao.deleteListing(l); File(ctx.filesDir, "publish/${l.id}").deleteRecursively()
    }

    fun addPhotos(l: Listing, uris: List<Uri>) = operation {
        val added = withContext(Dispatchers.IO) {
            val dir = File(ctx.filesDir, "publish/${l.id}").apply { mkdirs() }
            uris.mapNotNull { u ->
                runCatching {
                    val f = File(dir, "${System.nanoTime()}.jpg")
                    ctx.contentResolver.openInputStream(u)!!.use { i -> f.outputStream().use { o -> i.copyTo(o) } }
                    f.absolutePath
                }.getOrNull()
            }
        }
        val cur = dao.listing(l.id) ?: return@operation
        dao.saveListing(l.copy(photos = (cur.photoList() + added).joinToString("\n"), cover = cur.cover.ifBlank { added.firstOrNull() ?: "" }))
    }
    fun removePhoto(l: Listing, path: String) = operation {
        val cur = dao.listing(l.id) ?: return@operation
        val rest = cur.photoList() - path
        if (path !in cur.photoList()) return@operation
        File(path).delete()
        dao.saveListing(cur.copy(approvedAt = null, photos = rest.joinToString("\n"), cover = if (cur.cover == path) rest.firstOrNull() ?: "" else cur.cover))
    }

    /** Фото уходят в Groq. Результат — черновик: пользователь обязан просмотреть и при необходимости поправить. */
    fun runAi(l: Listing) = operation {
        require(dao.listing(l.id)?.status != "sold") { "Объект уже продан" }
        runCatching { SmartAi.run(ctx, l, secrets.get("vision_model", SmartAi.DEFAULT_VISION)) }
            .onSuccess { d -> dao.saveListing(l.copy(approvedAt = null, title = d.title, description = d.description, cover = d.cover.ifBlank { l.cover },
                features = d.features.joinToString("\n"), updatedAt = System.currentTimeMillis())) }
            .onFailure { log = "ИИ: ${err(it)}" }
    }

    fun activate(draft: Listing) = operation {
        val current = requireNotNull(dao.listing(draft.id))
        check(current.status != "sold") { "Объект уже продан" }
        require(Checks.blocking(draft).isEmpty()) { "Заполните обязательные параметры" }
        val offer = current.propertyId?.let { db.dao().property(it)?.sourceOfferId }?.let { db.hunter().offer(it) }
        val images = if (current.photos.isBlank() && offer != null) copyApprovedPhotos(current.id, offer.photoUrls) else current.photoList()
        val approved = draft.copy(approvedAt = System.currentTimeMillis(), photos = images.joinToString("\n"),
            cover = current.cover.ifBlank { images.firstOrNull().orEmpty() }, updatedAt = System.currentTimeMillis())
        db.withTransaction {
            check(dao.listing(current.id) == current) { "Карточка изменилась. Проверьте данные и подтвердите заново." }
            dao.saveListing(approved)
            approved.propertyId?.let { db.dao().property(it) }?.takeIf { it.status != "deal" }?.let { property ->
                db.dao().saveProperty(property.copy(address = approved.address, price = approved.price,
                    area = approved.area, rooms = approved.rooms, floor = approved.floor, type = approved.market,
                    status = if (property.status == "review") "active" else property.status))
            }
        }
        approved.propertyId?.let { ru.vladis.realtorai.owner.OwnerService(ctx).start(it) }
        val connected = Platform.entries.filter { it.isApi && secrets.publisher(it) != null }
        if (connected.isEmpty()) {
            log = "Карточка согласована. Реклама не запущена: Telegram и ВКонтакте не подключены. Доступен экспорт фидов."
        } else {
            sendToChannels(listOf(approved.id), connected)
            val sent = dao.pubsOf(approved.id).count { it.status == "published" && Platform.valueOf(it.platform) in connected }
            if (sent > 0 && approved.propertyId != null)
                ru.vladis.realtorai.owner.OwnerService(ctx).recordPublishingStarted(approved.propertyId, sent)
            val photoNote = if (offer != null && offer.photoUrls?.isNotBlank() == true && images.isEmpty())
                " Фотографии из источника загрузить не удалось." else ""
            log = "Подтверждено площадками: $sent из ${connected.size}. Для XML-площадок доступен экспорт фидов.$photoNote"
        }
    }

    private suspend fun copyApprovedPhotos(id: Long, urls: String?): List<String> = withContext(Dispatchers.IO) {
        val dir = File(ctx.filesDir, "publish/$id").apply { mkdirs() }
        ru.vladis.realtorai.hunter.ListingOrigin.photos(urls).take(12).mapNotNull { url ->
            val image = ru.vladis.realtorai.hunter.PhotoLoader.load(url) ?: return@mapNotNull null
            val path = File.createTempFile("approved-", ".jpg", dir)
            try {
                path.outputStream().use { check(image.compress(android.graphics.Bitmap.CompressFormat.JPEG, 88, it)) }
                path.absolutePath
            } catch (e: java.io.IOException) { path.delete(); null }
            catch (e: IllegalStateException) { path.delete(); null }
        }
    }

    /** Реальная отправка (Telegram, VK). Уже опубликованное пропускается — дублей нет. */
    fun publishApi(ids: List<Long>, platforms: List<Platform>) = operation {
        val awaiting = ids.count { dao.listing(it)?.approvedAt == null }
        sendToChannels(ids, platforms)
        log = if (awaiting == 0) "Статусы отправки сохранены в карточках." else "$awaiting карточек ждут согласия собственника."
    }

    private suspend fun sendToChannels(ids: List<Long>, platforms: List<Platform>) {
        for (id in ids) {
            val l = dao.listing(id) ?: continue
            if (l.status == "sold" || l.approvedAt == null) continue
            val missing = Checks.blocking(l)
            for (pl in platforms.filter { it.isApi }) {
                if (dao.pubsOf(id).find { it.platform == pl.name }?.status in setOf("published", "unknown", "outdated", "removal_pending")) continue
                val r = if (pl == Platform.TELEGRAM && l.postText().length > (if (l.photoList().isEmpty()) 4096 else 1024)) PubResult("failed", message = "Сократите текст объявления для Telegram")
                else if (missing.isNotEmpty()) PubResult("failed", message = "Не заполнено: ${missing.joinToString()}")
                else {
                    val p = secrets.publisher(pl)
                    if (p == null) PubResult("failed", message = "Площадка не настроена (Площадки → токены)")
                    else {
                        // Сбой после отправки может означать, что запись создана. Не повторяем автоматически.
                        record(l, pl, PubResult("unknown", message = "Отправка начата; проверьте площадку перед повтором"))
                        runCatching { p.publish(l) }.getOrElse { PubResult("unknown", message = "Результат не подтверждён: ${err(it)}") }
                    }
                }
                record(l, pl, r)
            }
        }
    }

    /** Файл выгрузки: фиды + фото + тексты. Статус «exported» = файл создан, а НЕ опубликовано. */
    fun exportTo(uri: Uri, ids: List<Long>, platforms: List<Platform>) = operation {
        runCatching {
            val ls = ids.distinct().mapNotNull { dao.listing(it) }
            require(ls.isNotEmpty()) { "Нет выбранных объектов" }
            require(ls.none { it.status != "sold" && it.approvedAt == null }) { "Сначала подтвердите согласие на размещение для всех выбранных объектов" }
            val bad = ls.filter { it.status != "sold" && Checks.blocking(it).isNotEmpty() }
            require(bad.isEmpty()) { "Заполните обязательные поля ${bad.size} объект(ов)" }
            withContext(Dispatchers.IO) { ctx.contentResolver.openOutputStream(uri)!!.use { Feeds.writeZip(it, ls, platforms.toSet(), cfg()) } }
            ls.filter { it.status != "sold" }.forEach { l -> platforms.filter { !it.isApi }.forEach { pl ->
                record(l, pl, PubResult("exported", message = if (pl == Platform.VT24)
                    "API VT 24 не подключён: карточка выгружена в файл (listings.json). Публикация не выполнена."
                else "Файл выгрузки создан. Публикация НЕ подтверждена: загрузите фид в кабинет площадки."))
            } }
            log = "Выгрузка сохранена. Публикация не подтверждена."
        }.onFailure { log = "Экспорт: ${err(it)}" }
    }

    fun updatePrice(id: Long, newPrice: Long) = operation {
        require(newPrice > 0) { "Цена должна быть положительной" }
        db.withTransaction {
            val l = requireNotNull(dao.listing(id))
            require(l.status != "sold") { "Объект уже продан" }
            dao.saveListing(l.copy(price = newPrice, updatedAt = System.currentTimeMillis()))
            l.propertyId?.let { db.dao().property(it) }?.let { db.dao().saveProperty(it.copy(price = newPrice)) }
            PublicationSync.stage(dao, id, sold = false)
        }
        PublicationSync.enqueue(ctx, id)
        log = "Цена сохранена. Проверяйте статусы обновления по площадкам."
    }

    fun markSold(id: Long) = operation {
        db.withTransaction {
            val l = requireNotNull(dao.listing(id))
            dao.saveListing(l.copy(status = "sold", updatedAt = System.currentTimeMillis()))
            l.propertyId?.let { db.dao().property(it) }?.let { db.dao().saveProperty(it.copy(status = "deal")) }
            PublicationSync.stage(dao, id, sold = true)
        }
        PublicationSync.enqueue(ctx, id)
        log = "Объект помечен проданным. Запрошено снятие через подключённые API."
    }
}
