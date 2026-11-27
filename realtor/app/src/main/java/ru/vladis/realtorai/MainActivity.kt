package ru.vladis.realtorai

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ru.vladis.realtorai.ui.App

class MainActivity : ComponentActivity() {
    private val vm: MainVm by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm) }
        if (savedInstanceState == null) receiveShare(intent)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        receiveShare(intent)
    }

    private fun receiveShare(intent: Intent) {
        if (intent.action == Intent.ACTION_SEND && intent.type == "text/plain")
            vm.acceptSharedListing(intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty())
    }
}
