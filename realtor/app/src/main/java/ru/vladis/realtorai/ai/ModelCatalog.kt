package ru.vladis.realtorai.ai

data class ModelSpec(val label: String, val file: String, val size: Long, val sha: String, val url: String)

val models = listOf(
    ModelSpec("Qwen 2.5 · 0.5B", "qwen-0.5b-q8.task", 546660344L,
        "e608953f169aeb1bd7b9155fec2559825e08453fc209b84eda3a781ed0452fd2",
        "https://huggingface.co/litert-community/Qwen2.5-0.5B-Instruct/resolve/49bfd323c25c9dae2118906e1f3ffd16e0729b13/Qwen2.5-0.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"),
    ModelSpec("Qwen 2.5 · 1.5B", "Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task", 1597913616L,
        "8d867a7c93a6acf2892f08e0174e2f6f351ad256b7e3cfb6d6cd9c89794b42e0",
        "https://huggingface.co/litert-community/Qwen2.5-1.5B-Instruct/resolve/60257f9f1d00ca1f24cc5ce3b829b4139be1ca2b/Qwen2.5-1.5B-Instruct_multi-prefill-seq_q8_ekv1280.task"),
    ModelSpec("Qwen3 · 4B", "Qwen3-4B-Q4_K_M.gguf", 2497280256L,
        "7485fe6f11af29433bc51cab58009521f205840f5b4ae3a32fa7f92e8534fdf5",
        "https://huggingface.co/Qwen/Qwen3-4B-GGUF/resolve/bc640142c66e1fdd12af0bd68f40445458f3869b/Qwen3-4B-Q4_K_M.gguf"),
    ModelSpec("Qwen3 · 8B · эксперимент", "Qwen3-8B-Q4_K_M.gguf", 5027783488L,
        "d98cdcbd03e17ce47681435b5150e34c1417f50b5c0019dd560e4882c5745785",
        "https://huggingface.co/Qwen/Qwen3-8B-GGUF/resolve/7c41481f57cb95916b40956ab2f0b139b296d974/Qwen3-8B-Q4_K_M.gguf"),
)
