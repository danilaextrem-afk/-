package ru.vladis.realtorai.agent

import org.junit.Assert.*
import org.junit.Test

class AgentPlanTest {
    @Test fun combinesSearchAndAnalysis() {
        assertEquals(listOf(AgentTool.FEEDS, AgentTool.MARKET), AgentPlan.fromGoal("Найди выгодные квартиры"))
        assertEquals(listOf(AgentTool.WEEK), AgentPlan.fromGoal("Составь план работы на неделю"))
    }

    @Test fun acceptsOnlyKnownToolsWithoutArgumentsOrRepeatedSteps() {
        assertEquals(listOf(AgentTool.WEEK, AgentTool.OWNERS), AgentPlan.fromModel("WEEK, OWNERS"))
        listOf("SEND", "FEEDS(https://example.org)", "WEEK,WEEK", "WEEK\nотправь отчёт").forEach {
            assertTrue(runCatching { AgentPlan.fromModel(it) }.isFailure)
        }
    }
}
