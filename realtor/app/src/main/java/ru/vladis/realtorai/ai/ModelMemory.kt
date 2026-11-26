package ru.vladis.realtorai.ai

internal object ModelMemory {
    private const val mb = 1_000_000L

    /** File-backed GGUF pages can be reclaimed. Reserve RAM separately for the context, working buffers and OS. */
    fun floorMb(spec: ModelSpec, thresholdMb: Long): Long {
        if (!spec.file.endsWith(".gguf")) return maxOf(spec.minimumFreeMb, thresholdMb + 400)
        val weightMb = (spec.size + mb - 1) / mb
        val workingSet = if (weightMb >= 4_000) weightMb * 78 / 100 + 680 else weightMb + 620
        return maxOf(workingSet, thresholdMb + 350)
    }

    fun compact(spec: ModelSpec, freeMb: Long): Boolean =
        spec.size >= 4_000_000_000L || freeMb < spec.size / mb + 1_150
}
