package ru.vladis.realtorai.ai

import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import ru.vladis.realtorai.data.Client

@RunWith(AndroidJUnit4::class)
class GroqStreamTest {
    @Test fun rendersDeltasAndReadsProviderUsage() {
        val stream = """
            data: {"choices":[{"delta":{"role":"assistant"}}]}

            data: {"choices":[{"delta":{"content":"Гото"}}]}

            data: {"choices":[{"delta":{"content":"во"}}]}

            data: {"choices":[],"x_groq":{"usage":{"total_tokens":42}}}

            data: [DONE]
        """.trimIndent()
        val partial = mutableListOf<String>()
        val reply = response(200, stream).use { Groq.read(it, true, partial::add) }
        assertEquals(listOf("Гото", "Готово"), partial)
        assertEquals("Готово", reply.text)
        assertEquals(CloudQuota(42, 150L, 12L, null), reply.quota)
    }

    @Test fun truncatedStreamAndExhaustedQuotaAreErrors() {
        val truncated = response(200, "data: {\"choices\":[]}")
        assertTrue(truncated.use { runCatching { Groq.read(it, true) {} }.isFailure })
        val throttled = response(429, """{"error":"provider details"}""")
        val error = throttled.use { runCatching { Groq.read(it, true) {} }.exceptionOrNull() }
        assertTrue(error?.message.orEmpty().contains("квота", ignoreCase = true))
    }

    @Test fun recognizesKnownClientNameAndContactDetails() {
        val clients = listOf(Client(name = "Иван Петров"))
        assertTrue(CloudAi.referencesClients("Покупатель Иван Петров", clients))
        assertFalse(CloudAi.referencesClients("Сделай план недели", clients))
        assertTrue(CloudAi.containsContact("Телефон +7 (999) 123-45-67"))
        assertTrue(CloudAi.containsContact("Почта contact@example.com"))
    }

    private fun response(code: Int, body: String) = Response.Builder()
        .request(Request.Builder().url("https://api.groq.com/openai/v1/chat/completions").build())
        .protocol(Protocol.HTTP_1_1).code(code).message("response")
        .header("x-ratelimit-remaining-tokens", "150")
        .header("x-ratelimit-remaining-requests", "12")
        .body(body.toResponseBody()).build()
}
