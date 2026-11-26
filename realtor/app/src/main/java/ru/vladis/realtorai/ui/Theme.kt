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

val Graphite = Color(0xFF0E1514)
val Surface1 = Color(0xFF182220)
val Surface2 = Color(0xFF23332F)
val Lime = Color(0xFF61DFAC)
val Muted = Color(0xFFADC0B9)

@Composable fun RealtorTheme(content: @Composable () -> Unit) = MaterialTheme(
    colorScheme = darkColorScheme(primary = Lime, onPrimary = Color(0xFF10291F), background = Graphite,
        onBackground = Color(0xFFF2F2F3), surface = Surface1, onSurface = Color(0xFFF2F2F3),
        surfaceVariant = Surface2, onSurfaceVariant = Muted, secondaryContainer = Color(0xFF183C35),
        onSecondaryContainer = Lime, outline = Color(0xFF364657)),
    typography = Typography(
        headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = (-0.8).sp, lineHeight = 38.sp),
        headlineSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 25.sp, letterSpacing = (-0.5).sp, lineHeight = 31.sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.2.sp)),
    shapes = Shapes(small = RoundedCornerShape(12.dp), medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp)),
    content = content)

@Composable fun AppCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, content: @Composable ColumnScope.() -> Unit) =
    Card(modifier.fillMaxWidth().animateContentSize(), shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF30443C)),
        colors = CardDefaults.cardColors(containerColor = Surface1)) {
        Column(Modifier.then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp), content = content)
    }

@Composable fun Title(t: String) = Text(t, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)

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
