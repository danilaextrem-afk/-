package ru.vladis.realtorai.agent

import org.junit.Assert.*
import org.junit.Test

class AgentProtocolTest {
    @Test fun restoresPendingCallAndPriorEvidence() {
        val decision = AgentProtocol.parse("""{"tool":"FIND","args":{"area":"Москва","budget":11000000,"rooms":2}}""")
        val progress = AgentProgress(true, listOf(checkNotNull(decision.action)), listOf("Источники обновлены"))
        val restored = AgentProtocol.load(AgentProtocol.save(progress))
        assertEquals(progress, restored)
        assertEquals(11_000_000L, restored.pending.single().args.get("budget").asLong)
        assertEquals("Какой район?", AgentProtocol.parse("""{"ask":"Какой район?"}""").question)
    }
    @Test fun rejectsExternalActionsAndUnexpectedArguments() {
        assertTrue(runCatching { AgentProtocol.parse("""{"tool":"PUBLISH","args":{}}""") }.isFailure)
        assertTrue(runCatching { AgentProtocol.parse("""{"tool":"LISTING","args":{"send":true}}""") }.isFailure)
    }
}
