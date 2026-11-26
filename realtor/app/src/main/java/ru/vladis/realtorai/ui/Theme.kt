package ru.vladis.realtorai.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import java.util.Locale

val Graphite: Color @Composable get() = MaterialTheme.colorScheme.background
val Surface1: Color @Composable get() = MaterialTheme.colorScheme.surface
val Surface2: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
val Lime: Color @Composable get() = MaterialTheme.colorScheme.primary
val Muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

private val dark = darkColorScheme(
    primary = Color(0xFF79E5B2), onPrimary = Color(0xFF09291E),
    primaryContainer = Color(0xFF12392D), onPrimaryContainer = Color(0xFFA6EECB),
    background = Color(0xFF101412), onBackground = Color(0xFFE7EEE9),
    surface = Color(0xFF1A211E), onSurface = Color(0xFFE7EEE9),
    surfaceVariant = Color(0xFF25302A), onSurfaceVariant = Color(0xFFA8B8AE),
    secondaryContainer = Color(0xFF21352C), onSecondaryContainer = Color(0xFFD2F2E0),
    outline = Color(0xFF8B9C92), outlineVariant = Color(0xFF34453D)
)
private val light = lightColorScheme(
    primary = Color(0xFF005A3C), onPrimary = Color.White,
    primaryContainer = Color(0xFFBDEED6), onPrimaryContainer = Color(0xFF073628),
    background = Color(0xFFF4F6F1), onBackground = Color(0xFF15231C),
    surface = Color(0xFFFFFFFF), onSurface = Color(0xFF15231C),
    surfaceVariant = Color(0xFFE4EDE6), onSurfaceVariant = Color(0xFF465950),
    secondaryContainer = Color(0xFFDCEEE2), onSecondaryContainer = Color(0xFF17382B),
    outline = Color(0xFF657B6F), outlineVariant = Color(0xFFCCDAD1)
)

@Composable fun RealtorTheme(darkMode: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = if (darkMode) dark else light,
    typography = Typography(
        displaySmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 40.sp, letterSpacing = (-1.7).sp, lineHeight = 45.sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 32.sp, letterSpacing = (-0.9).sp, lineHeight = 37.sp),
        headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 26.sp, letterSpacing = (-0.7).sp, lineHeight = 32.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 17.sp),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.25.sp)),
    shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
    content = content)

@Composable fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(20.dp)
    val body: @Composable () -> Unit = {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
    if (onClick == null) Surface(modifier.fillMaxWidth(), shape = shape, color = Surface1, content = body)
    else Surface(onClick, modifier.fillMaxWidth(), shape = shape, color = Surface1, content = body)
}

@Composable fun Title(t: String) = Text(t, style = MaterialTheme.typography.headlineSmall)

@Composable fun ScreenHeading(title: String, detail: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.headlineMedium)
            Text(detail, color = Muted, style = MaterialTheme.typography.bodySmall)
        }
        action?.invoke()
    }
}

@Composable fun Field(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier,
                      keyboard: KeyboardType = KeyboardType.Text, single: Boolean = true, password: Boolean = false) =
    OutlinedTextField(value, onChange, modifier.fillMaxWidth(), label = { Text(label) }, singleLine = single,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(14.dp))

@Composable fun Chips(options: List<String>, selected: String, onSelect: (String) -> Unit) =
    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { FilterChip(selected == it, { onSelect(it) }, label = { Text(it) }) }
    }

fun Long.rub(): String = String.format(Locale.US, "%,d", this).replace(',', ' ') + " ₽"
fun Double.rub(): String = toLong().rub()
