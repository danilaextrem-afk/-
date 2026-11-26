package ru.vladis.realtorai.ai

data class ModelSpec(val label: String, val file: String, val size: Long, val sha: String, val url: String, val profile: String = "Локальный диалог", val minimumFreeMb: Long = size / 1_000_000 + 600)

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
    ModelSpec("Qwen 3.5 · 9B · эксперимент", "Qwen3.5-9B-Q4_K_M.gguf", 5680522464L,
        "03b74727a860a56338e042c4420bb3f04b2fec5734175f4cb9fa853daf52b7e8",
        "https://huggingface.co/unsloth/Qwen3.5-9B-GGUF/resolve/3885219b6810b007914f3a7950a8d1b469d598a5/Qwen3.5-9B-Q4_K_M.gguf",
        "Приоритетный тяжёлый режим. Текст, анализ и планирование. Скорость зависит от устройства.", 7000),
    ModelSpec("Qwen3 · 14B · эксперимент", "Qwen3-14B-Q4_K_M.gguf", 9001752960L,
        "500a8806e85ee9c83f3ae08420295592451379b4f8cf2d0f41c15dffeb6b81f0",
        "https://huggingface.co/Qwen/Qwen3-14B-GGUF/resolve/530227a7d994db8eca5ab5ced2fb692b614357fd/Qwen3-14B-Q4_K_M.gguf",
        "Эксперимент. Загрузка весов требует большого запаса физической памяти.", 10100),
    ModelSpec("DeepSeek R1 Distill · 7B · эксперимент", "DeepSeek-R1-Distill-Qwen-7B-Q4_K_M.gguf", 4683073504L,
        "731ece8d06dc7eda6f6572997feb9ee1258db0784827e642909d9b565641937b",
        "https://huggingface.co/bartowski/DeepSeek-R1-Distill-Qwen-7B-GGUF/resolve/361004151d4f4f6b446dc5e6d46fbf4422a80d5f/DeepSeek-R1-Distill-Qwen-7B-Q4_K_M.gguf",
        "Альтернативный режим рассуждений. Возможны длинные ответы.", 5500),
)
