package ru.vladis.realtorai.ai

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.PowerManager
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.OkHttpClient
import okhttp3.Request

internal object NativeQwen {
    init { System.loadLibrary("realtor_inference") }
    external fun generate(file: String, prompt: ByteArray, sink: NativeTokenSink?): ByteArray?
}

internal class NativeTokenSink(private val onUpdate: (ByteArray) -> Boolean) {
    @androidx.annotation.Keep fun update(value: ByteArray): Boolean = onUpdate(value)
}

object DeviceModel {
    private val downloadLock = Mutex()
    private val generationLock = Mutex()
    private val http = OkHttpClient()
    private fun prefs(ctx: Context) = ctx.getSharedPreferences("inference", Context.MODE_PRIVATE)
    fun selected(ctx: Context) = models.firstOrNull { it.file == prefs(ctx).getString("model", "") } ?: models.first()
    fun select(ctx: Context, spec: ModelSpec) {
        require(installed(ctx, spec))
        prefs(ctx).edit().putString("model", spec.file).apply()
    }
    private fun file(ctx: Context, spec: ModelSpec) = File(ctx.noBackupFilesDir, spec.file)
    fun installed(ctx: Context, spec: ModelSpec = selected(ctx)) = file(ctx, spec).length() == spec.size
    fun partial(ctx: Context, spec: ModelSpec) = File(ctx.noBackupFilesDir, spec.file + ".part").length()

    fun availableMemory(ctx: Context): Long = ActivityManager.MemoryInfo().also {
        ctx.getSystemService(ActivityManager::class.java).getMemoryInfo(it)
    }.availMem / 1_000_000

    fun recommended(ctx: Context): ModelSpec =
        models.take(3).lastOrNull { it.minimumFreeMb + 500 <= availableMemory(ctx) } ?: models.first()

    suspend fun remove(ctx: Context, spec: ModelSpec) = downloadLock.withLock {
        generationLock.withLock { withContext(Dispatchers.IO) {
            for (file in listOf(file(ctx, spec), File(ctx.noBackupFilesDir, spec.file + ".part"))) {
                if (file.exists()) check(file.delete()) { "Не удалось удалить модель" }
            }
            if (selected(ctx) == spec) {
                val fallback = models.firstOrNull { installed(ctx, it) }
                if (fallback == null) prefs(ctx).edit().remove("model").apply() else select(ctx, fallback)
            }
        } }
    }

    suspend fun importVerified(ctx: Context, uri: android.net.Uri, spec: ModelSpec, progress: suspend (Int) -> Unit) =
        downloadLock.withLock { withContext(Dispatchers.IO) {
            require(spec in models && spec.file.endsWith(".gguf"))
            if (installed(ctx, spec)) return@withContext
            check(ctx.noBackupFilesDir.usableSpace > spec.size + 100_000_000L) { "Недостаточно места для копии модели" }
            val temporary = File.createTempFile("gguf-import-", ".tmp", ctx.noBackupFilesDir)
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                checkNotNull(ctx.contentResolver.openInputStream(uri)) { "Файл недоступен" }.use { input ->
                    temporary.outputStream().buffered().use { output ->
                        val buffer = ByteArray(262144)
                        var size = 0L
                        var lastPercent = -1
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            size += read
                            check(size <= spec.size) { "Файл крупнее выбранной модели" }
                            digest.update(buffer, 0, read)
                            output.write(buffer, 0, read)
                            val percent = (size * 100 / spec.size).toInt()
                            if (percent != lastPercent) { progress(percent); lastPercent = percent }
                        }
                    }
                }
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                check(temporary.length() == spec.size && hash == spec.sha) { "GGUF не совпадает с выбранной моделью по SHA-256" }
                check(temporary.renameTo(file(ctx, spec)))
                File(ctx.noBackupFilesDir, spec.file + ".part").delete()
            } finally { temporary.delete() }
        } }

    suspend fun download(ctx: Context, spec: ModelSpec = selected(ctx), progress: suspend (Int) -> Unit) =
        downloadLock.withLock { withContext(Dispatchers.IO) {
            if (installed(ctx, spec)) return@withContext
            val part = File(ctx.noBackupFilesDir, spec.file + ".part")
            if (part.length() > spec.size) check(part.delete())
            var received = part.length()
            check(ctx.noBackupFilesDir.usableSpace > spec.size - received + 100_000_000L) { "Недостаточно места для модели" }
            if (received < spec.size) {
                val request = Request.Builder().url(spec.url).header("Accept-Encoding", "identity")
                if (received > 0) request.header("Range", "bytes=$received-")
                http.newCall(request.build()).execute().use { r ->
                    check(r.code == 200 || r.code == 206) { "HTTP ${r.code}. Загрузку можно продолжить." }
                    val append = r.code == 206
                    if (append) check(r.header("Content-Range")?.startsWith("bytes $received-") == true)
                    else received = 0
                    val input = checkNotNull(r.body).byteStream()
                    java.io.FileOutputStream(part, append).buffered().use { output ->
                        val buffer = ByteArray(131072)
                        var last = -1
                        while (true) {
                            currentCoroutineContext().ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            received += n
                            check(received <= spec.size) { "Неверный размер файла" }
                            output.write(buffer, 0, n)
                            val percent = (received * 100 / spec.size).toInt()
                            if (last != percent) { progress(percent); last = percent }
                        }
                    }
                }
            }
            check(part.length() == spec.size) { "Загрузка прервалась. Нажмите «Продолжить»." }
            val digest = MessageDigest.getInstance("SHA-256")
            part.inputStream().use { input ->
                val buffer = ByteArray(262144)
                while (true) {
                    currentCoroutineContext().ensureActive()
                    val n = input.read(buffer)
                    if (n < 0) break
                    digest.update(buffer, 0, n)
                }
            }
            if (digest.digest().joinToString("") { "%02x".format(it) } != spec.sha) {
                part.delete()
                error("Проверка SHA-256 не прошла. Повторите загрузку.")
            }
            check(part.renameTo(file(ctx, spec))) { "Не удалось сохранить модель" }
        } }

    suspend fun answer(
        ctx: Context, system: String, messages: List<Pair<String, String>>,
        spec: ModelSpec = selected(ctx), onPartial: ((String) -> Unit)? = null
    ): String = generationLock.withLock { withContext(Dispatchers.IO) {
            check(installed(ctx, spec)) { "Скачайте выбранную модель" }
            if (Build.VERSION.SDK_INT >= 29) check(ctx.getSystemService(PowerManager::class.java).currentThermalStatus < PowerManager.THERMAL_STATUS_SEVERE) { "Устройство перегрето. Повторите после охлаждения." }
            val mem = ActivityManager.MemoryInfo().also { ctx.getSystemService(ActivityManager::class.java).getMemoryInfo(it) }
            check(!mem.lowMemory && (spec == models.first() || mem.availMem / 1_000_000 > spec.minimumFreeMb)) { "Свободной памяти мало. Выберите меньшую модель." }
            val history = messages.dropLast(1).takeLast(2).joinToString("\n") { it.second.take(160) }
            val prompt = system.take(580) + "\n" + history + "\n" + messages.last().second.take(1200)
            val answer = if (spec.file.endsWith(".gguf")) {
                val safe = prompt.replace("<|", "‹|")
                val formatted = "<|im_start|>user\n$safe /no_think<|im_end|>\n<|im_start|>assistant\n<think>\n\n</think>\n\n"
                val modelPrompt = if (spec.file.startsWith("DeepSeek"))
                    "<｜begin▁of▁sentence｜><｜User｜>$safe<｜Assistant｜><think>\n"
                else if (spec.file.startsWith("Qwen3.5")) "<|im_start|>system\n/no_think<|im_end|>\n" + formatted
                else formatted
                val job = currentCoroutineContext()
                val sink = NativeTokenSink { bytes ->
                    if (job.isActive && bytes.isNotEmpty()) onPartial?.invoke(bytes.decodeToString().trimEnd('�'))
                    job.isActive
                }
                val bytes = NativeQwen.generate(file(ctx, spec).path, modelPrompt.toByteArray(), sink)
                job.ensureActive()
                checkNotNull(bytes).decodeToString()
            } else {
                val options = LlmInference.LlmInferenceOptions.builder().setModelPath(file(ctx,spec).path).setMaxTokens(1024).build()
                LlmInference.createFromOptions(ctx, options).use { it.generateResponse(prompt) }
            }
            answer.trim().also { check(it.isNotBlank()) { "Модель вернула пустой ответ" } }
        } }
}
