package ru.vladis.realtorai.local

import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import java.io.OutputStream

object ProposalPdf {
    fun write(text: String, out: OutputStream) {
        val doc = PdfDocument()
        val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 38, 48); textSize = 13f }
        val heading = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(20, 80, 70); textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
        try {
            val lines = text.lines().flatMap { line ->
                val parts = mutableListOf<String>(); var rest = line
                while (rest.isNotEmpty()) {
                    val count = body.breakText(rest, true, 499f, null).coerceAtLeast(1)
                    val split = if (count < rest.length) rest.lastIndexOf(' ', count - 1).takeIf { it > 0 } ?: count else count
                    parts += rest.take(split); rest = rest.drop(split).trimStart()
                }
                if (parts.isEmpty()) listOf("") else parts
            }
            lines.chunked(31).ifEmpty { listOf(emptyList()) }.forEachIndexed { i, chunk ->
                val page = doc.startPage(PdfDocument.PageInfo.Builder(595, 842, i + 1).create())
                page.canvas.drawText("REALTOR AI", 48f, 65f, heading)
                page.canvas.drawText("Коммерческое предложение  •  ${i + 1}", 48f, 92f, body)
                chunk.forEachIndexed { j, line -> page.canvas.drawText(line, 48f, 135f + j * 21f, body) }
                doc.finishPage(page)
            }
            doc.writeTo(out)
        } finally { doc.close() }
    }
}
