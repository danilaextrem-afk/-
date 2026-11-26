package ru.vladis.realtorai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import ru.vladis.realtorai.ui.App
import ru.vladis.realtorai.ui.RealtorTheme

class MainActivity : ComponentActivity() {
    private val vm: MainVm by viewModels()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { App(vm) }
    }
}
