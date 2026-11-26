package ru.vladis.realtorai.data

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.ai.DeviceModel

@RunWith(AndroidJUnit4::class)
class DeviceInferenceTest {
    @Test fun downloadsVerifiedWeightsAndGeneratesText(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        DeviceModel.download(context) {}
        assertTrue(DeviceModel.installed(context))
        val answer = DeviceModel.answer(context, "Ответь кратко на русском.", listOf("user" to "Как называется комната для приготовления еды?"))
        assertTrue(answer.isNotBlank())
        android.util.Log.i("DeviceInferenceTest", "Ответ на тестовый вопрос: " + answer.take(240))
    }

    @Test fun generatesAndStreamsRealQwenGguf(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val spec = ru.vladis.realtorai.ai.ModelSpec(
            "Qwen3 0.6B Q8 runtime fixture", "ci-Qwen3-0.6B-Q8_0.gguf", 639446688L,
            "9465e63a22add5354d9bb4b99e90117043c7124007664907259bd16d043bb031",
            "https://huggingface.co/Qwen/Qwen3-0.6B-GGUF/resolve/23749fefcc72300e3a2ad315e1317431b06b590a/Qwen3-0.6B-Q8_0.gguf"
        )
        try {
            DeviceModel.download(context, spec) {}
            val updates = mutableListOf<String>()
            val started = android.os.SystemClock.elapsedRealtime()
            val answer = DeviceModel.answer(context,
                "Напиши по-русски не меньше двух предложений.",
                listOf("user" to "Зачем перед показом квартиры подготовить фотографии?"), spec) { updates += it }
            val duration = android.os.SystemClock.elapsedRealtime() - started
            assertTrue(answer.isNotBlank())
            assertTrue("Runtime should emit incremental text", updates.any { it.isNotBlank() && it.length <= answer.length })
            android.util.Log.i("DeviceInferenceTest", "llama.cpp Qwen3 0.6B · ${duration}мс · " + answer.take(240))
        } finally { DeviceModel.remove(context, spec) }
    }
}
