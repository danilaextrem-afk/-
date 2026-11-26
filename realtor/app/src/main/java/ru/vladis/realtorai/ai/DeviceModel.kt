package ru.vladis.realtorai.ai

import android.content.Context
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object DeviceModel {
    private const val size = 546660344L
    private const val hash = "e608953f169aeb1bd7b9155fec2559825e08453fc209b84eda3a781ed0452fd2"
    private const val url = "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/49bfd323c25c9dae2118906e1f3ffd16e0729b13/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"
    private fun model(ctx: Context) = File(ctx.noBackupFilesDir, "qwen-0.5b-q8.task")
    fun installed(ctx: Context) = model(ctx).length() == size

    suspend fun download(ctx: Context, progress: suspend (Int) -> Unit) = withContext(Dispatchers.IO) {
        val target = model(ctx)
        check(ctx.noBackupFilesDir.usableSpace > size + 100_000_000) { "Освободите минимум 650 МБ" }
        val temp = File.createTempFile("qwen", ".part", ctx.noBackupFilesDir)
        try {
            val digest = MessageDigest.getInstance("SHA-256")
            val request = Request.Builder().url(url).build()
            OkHttpClient().newCall(request).execute().use { response ->
                check(response.isSuccessful) { "Загрузка: HTTP ${response.code}" }
                val input = checkNotNull(response.body).byteStream()
                temp.outputStream().buffered().use { output ->
                    val buffer = ByteArray(65536)
                    var received = 0L
                    var lastPercent = -1
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val count = input.read(buffer)
                        if (count < 0) break
                        received += count
                        check(received <= size) { "Неверный размер модели" }
                        output.write(buffer, 0, count)
                        digest.update(buffer, 0, count)
                        val percent = (received * 100 / size).toInt()
                        if (percent != lastPercent) { progress(percent); lastPercent = percent }
                    }
                }
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            check(temp.length() == size && actual == hash) { "Файл модели повреждён" }
            check(temp.renameTo(target)) { "Не удалось сохранить модель" }
        } finally { temp.delete() }
    }

    suspend fun answer(ctx: Context, system: String, messages: List<Pair<String, String>>): String =
        withContext(Dispatchers.IO) {
            val options = LlmInference.LlmInferenceOptions.builder()
                .setModelPath(model(ctx).path).setMaxTokens(1024).build()
            LlmInference.createFromOptions(ctx, options).use { engine ->
                val context = messages.dropLast(1).takeLast(2).joinToString("\n") { it.second.take(150) }
                val prompt = system.take(450) + "\n" + context + "\n" + messages.last().second.take(1000)
                engine.generateResponse(prompt).trim().also { check(it.isNotBlank()) { "Модель вернула пустой ответ" } }
            }
        }
}
