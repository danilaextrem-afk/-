package ru.vladis.realtorai.ai

import org.junit.Assert.*
import org.junit.Test

class ModelMemoryTest {
    @Test fun qwen9bUsesMappedWorkingSetAndKeepsAndroidReserve() {
        val q4 = models.single { it.file == "Qwen3.5-9B-Q4_K_M.gguf" }
        val iq3 = models.single { it.file == "Qwen3.5-9B-UD-IQ3_XXS.gguf" }
        assertTrue(ModelMemory.floorMb(q4, 400) in 4_500L..6_000L)
        assertTrue(ModelMemory.floorMb(iq3, 400) < ModelMemory.floorMb(q4, 400))
        assertTrue(ModelMemory.floorMb(q4, 6_000) >= 6_350)
        assertTrue(ModelMemory.compact(q4, 5_800))
    }
}
