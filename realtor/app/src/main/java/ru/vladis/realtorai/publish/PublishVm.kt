package ru.vladis.realtorai.publish

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.*
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class PublishVm(app: Application) : AndroidViewModel(app) {
    private val ctx = app.applicationContext
    private val dao = ru.vladis.realtorai.data.WorkspaceDb.get(app).publications()
    val secrets = PublishSecrets(app)
    val listings = dao.listings().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val pubs = dao.publications().stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    var busy by mutableStateOf(false); private set
    var log by mutableStateOf<String?>(null)

    private fun cfg() = Feeds.Cfg(secrets.get("photo_base"), secrets.get("phone"))
    private fun publisher(p: Platform): Publisher? = when (p) {
        Platform.TELEGRAM -> secrets.get("tg_token").takeIf { it.isNotBlank() }?.let { t -> secrets.get("tg_chat").takeIf { it.isNotBlank() }?.let { TelegramPublisher(t, it) } }
        Platform.VK -> secrets.get("vk_token").takeIf { it.isNotBlank() }?.let { t -> secrets.get("vk_owner").takeIf { it.isNotBlank() }?.let { VkPublisher(t, it) } }
        else -> null
    }
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
        dao.saveListing(l.copy(status = current.status, photos = current.photos,
            cover = l.cover.takeIf { it in current.photoList() } ?: current.cover, updatedAt = System.currentTimeMillis()))
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
        dao.saveListing(cur.copy(photos = rest.joinToString("\n"), cover = if (cur.cover == path) rest.firstOrNull() ?: "" else cur.cover))
    }

    /** Фото уходят в Groq. Результат — черновик: пользователь обязан просмотреть и при необходимости поправить. */
    fun runAi(l: Listing) = operation {
        runCatching { SmartAi.run(ctx, l, secrets.get("vision_model", SmartAi.DEFAULT_VISION)) }
            .onSuccess { d -> dao.saveListing(l.copy(title = d.title, description = d.description, cover = d.cover.ifBlank { l.cover },
                features = d.features.joinToString("\n"), updatedAt = System.currentTimeMillis())) }
            .onFailure { log = "ИИ: ${err(it)}" }
    }

    /** Реальная отправка (Telegram, VK). Уже опубликованное пропускается — дублей нет. */
    fun publishApi(ids: List<Long>, platforms: List<Platform>) = operation {
        for (id in ids) {
            val l = dao.listing(id) ?: continue
            if (l.status == "sold") continue
            val missing = Checks.blocking(l)
            for (pl in platforms.filter { it.isApi }) {
                if (dao.pubsOf(id).find { it.platform == pl.name }?.status in setOf("published", "unknown", "outdated", "removal_pending")) continue
                val r = if (pl == Platform.TELEGRAM && l.postText().length > (if (l.photoList().isEmpty()) 4096 else 1024)) PubResult("failed", message = "Сократите текст объявления для Telegram")
                else if (missing.isNotEmpty()) PubResult("failed", message = "Не заполнено: ${missing.joinToString()}")
                else {
                    val p = publisher(pl)
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
        log = "Готово. Результат по каждой площадке — в карточках."
    }

    /** Файл выгрузки: фиды + фото + тексты. Статус «exported» = файл создан, а НЕ опубликовано. */
    fun exportTo(uri: Uri, ids: List<Long>, platforms: List<Platform>) = operation {
        runCatching {
            val ls = ids.distinct().mapNotNull { dao.listing(it) }
            require(ls.isNotEmpty()) { "Нет выбранных объектов" }
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
        val l = (dao.listing(id) ?: return@operation).copy(price = newPrice, updatedAt = System.currentTimeMillis())
        require(l.status != "sold") { "Объект уже продан" }
        dao.saveListing(l)
        for (p in dao.pubsOf(id)) {
            val pl = Platform.valueOf(p.platform)
            if (p.status in setOf("published", "outdated") && pl.isApi && p.externalId.isNotBlank()) {
                runCatching { (publisher(pl) ?: error("Площадка не настроена")).updatePrice(l, p) }
                    .onSuccess { record(l, pl, it) }
                    .onFailure { dao.savePub(p.copy(status = "outdated", message = "Не удалось обновить цену: ${err(it)}")) }
            } else if (p.status == "exported") record(l, pl, PubResult("outdated", message = "Цена изменена — перевыгрузите фид"))
        }
        log = "Цена сохранена; результат по площадкам — в карточке."
    }

    fun markSold(id: Long) = operation {
        val l = (dao.listing(id) ?: return@operation).copy(status = "sold", updatedAt = System.currentTimeMillis())
        dao.saveListing(l)
        for (p in dao.pubsOf(id)) {
            val pl = Platform.valueOf(p.platform)
            if (p.status in setOf("published", "outdated", "removal_pending") && pl.isApi && p.externalId.isNotBlank()) {
                runCatching { (publisher(pl) ?: error("Площадка не настроена")).unpublish(l, p) }
                    .onSuccess { record(l, pl, it) }
                    .onFailure { dao.savePub(p.copy(status = "removal_pending", message = "Не удалось снять: ${err(it)}")) }
            } else if (!pl.isApi && (p.status == "exported" || p.status == "outdated"))
                record(l, pl, PubResult("removal_pending", message = "Исключён из фидов — перевыгрузите фид, снятие произойдёт после его обработки площадкой"))
        }
        log = "Объект помечен проданным."
    }
}
