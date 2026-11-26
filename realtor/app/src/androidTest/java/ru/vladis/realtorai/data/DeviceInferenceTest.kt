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
}
