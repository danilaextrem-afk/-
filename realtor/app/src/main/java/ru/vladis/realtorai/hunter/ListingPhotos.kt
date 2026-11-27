package ru.vladis.realtorai.hunter

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Dns
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.Inet4Address
import java.net.InetAddress
import java.net.Proxy
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

internal object PhotoLoader {
    private const val maxBytes = 8_000_000L
    private val cache = object : LruCache<String, Bitmap>(24 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.allocationByteCount
    }
    private val client = OkHttpClient.Builder()
        .proxy(Proxy.NO_PROXY)
        .followSslRedirects(false)
        .dns(object : Dns {
            override fun lookup(hostname: String): List<InetAddress> =
                Dns.SYSTEM.lookup(hostname).filter { publicAddress(it) }.takeIf { it.isNotEmpty() }
                    ?: throw UnknownHostException("Photo host is not public")
        })
        .callTimeout(12, TimeUnit.SECONDS).build()

    private fun publicAddress(ip: InetAddress): Boolean {
        val first = ip.address[0].toInt() and 255
        val second = ip.address[1].toInt() and 255
        if (ip.isAnyLocalAddress || ip.isLoopbackAddress || ip.isLinkLocalAddress ||
            ip.isSiteLocalAddress || ip.isMulticastAddress) return false
        return if (ip is Inet4Address) first in 1..223 && !(first == 100 && second in 64..127) else first in 32..63
    }

    suspend fun load(url: String): Bitmap? = withContext(Dispatchers.IO) {
        cache.get(url)?.let { return@withContext it }
        val target = ListingOrigin.webUrl(url) ?: return@withContext null
        val bitmap = runCatching {
            client.newCall(Request.Builder().url(target).build()).execute().use { reply ->
                val body = reply.body ?: return@use null
                if (!reply.isSuccessful || body.contentLength() > maxBytes) return@use null
                val stream = body.source()
                if (stream.request(maxBytes + 1)) return@use null
                val bytes = stream.readByteArray()
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                if (options.outWidth <= 0 || options.outHeight <= 0) return@use null
                var sample = 1
                while (maxOf(options.outWidth, options.outHeight) / sample > 1600) sample *= 2
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().also { it.inSampleSize = sample })
            }
        }.getOrNull()
        bitmap?.also { cache.put(url, it) }
    }
}

@Composable
internal fun ListingPhoto(url: String, modifier: Modifier, description: String, fit: Boolean = false) {
    var attempt by remember(url) { mutableIntStateOf(0) }
    val result by produceState<Pair<Boolean, Bitmap?>>(false to null, url, attempt) {
        value = false to null
        value = true to PhotoLoader.load(url)
    }
    val (finished, bitmap) = result
    Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant), contentAlignment = Alignment.Center) {
        when {
            bitmap != null -> Image(bitmap.asImageBitmap(), description, Modifier.fillMaxSize(),
                contentScale = if (fit) ContentScale.Fit else ContentScale.Crop)
            finished -> TextButton({ attempt++ }) { Text("Фото не загрузилось · Повторить") }
            else -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp)
        }
    }
}

@Composable
internal fun ListingGallery(photos: List<String>) {
    if (photos.isEmpty()) {
        Text("Источник не предоставил фотографии", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    val preview = rememberPagerState { photos.size }
    var enlarged by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        HorizontalPager(preview) { page ->
            ListingPhoto(photos[page], Modifier.fillMaxWidth().height(230.dp).clickable { enlarged = true },
                "Фото ${page + 1} из ${photos.size}")
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${preview.currentPage + 1} из ${photos.size}", style = MaterialTheme.typography.labelMedium)
            TextButton({ enlarged = true }) { Text("Открыть галерею") }
        }
    }
    if (enlarged) Dialog(
        onDismissRequest = { enlarged = false },
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        val gallery = rememberPagerState(initialPage = preview.currentPage.coerceIn(photos.indices)) { photos.size }
        Surface(Modifier.fillMaxSize()) {
            Column {
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text("Фото ${gallery.currentPage + 1} из ${photos.size}", Modifier.weight(1f))
                    IconButton({ enlarged = false }) { Icon(Icons.Default.Close, "Закрыть галерею") }
                }
                HorizontalPager(gallery, Modifier.weight(1f)) { page ->
                    ListingPhoto(photos[page], Modifier.fillMaxSize(), "Фото ${page + 1} из ${photos.size}", fit = true)
                }
            }
        }
    }
}
