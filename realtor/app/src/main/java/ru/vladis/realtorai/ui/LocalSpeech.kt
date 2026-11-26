package ru.vladis.realtorai.ui

import android.speech.tts.TextToSpeech
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun rememberLocalSpeech(): TextToSpeech? {
    val context = LocalContext.current
    var ready by remember { mutableStateOf<TextToSpeech?>(null) }
    DisposableEffect(context) {
        var active = true
        var engine: TextToSpeech? = null
        val main = android.os.Handler(android.os.Looper.getMainLooper())
        engine = TextToSpeech(context) { status ->
            main.post {
                if (active && status == TextToSpeech.SUCCESS) {
                    val local = engine?.voices?.filter { it.locale.language == "ru" && !it.isNetworkConnectionRequired }
                        ?.maxByOrNull { it.quality }
                    if (local != null && engine?.setVoice(local) == TextToSpeech.SUCCESS) ready = engine
                }
            }
        }
        onDispose { active = false; engine.shutdown() }
    }
    return ready
}
