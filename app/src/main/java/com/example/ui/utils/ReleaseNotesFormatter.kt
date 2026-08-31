package com.example.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@Composable
fun buildFormattedReleaseNotes(markdown: String?): AnnotatedString {
    val notes = markdown ?: return AnnotatedString("No release notes available.")
    val primaryColor = MaterialTheme.colorScheme.primary

    return buildAnnotatedString {
        val lines = notes.lines()
        var isFirst = true

        lines.forEach { rawLine ->
            val line = rawLine.trim()
            if (line.isEmpty()) return@forEach

            if (!isFirst) {
                append("\n\n")
            }
            isFirst = false

            if (line.startsWith("#") || line.startsWith("🚀")) {
                // Header line
                val cleanHeader = line.replace(Regex("^#+\\s*"), "")
                withStyle(SpanStyle(fontWeight = FontWeight.Black, color = primaryColor)) {
                    append(cleanHeader)
                }
            } else {
                // Bullet or normal line
                val cleanLine = if (line.startsWith("- ")) line.substring(2) else line
                val parts = cleanLine.split("**")

                parts.forEachIndexed { index, part ->
                    if (index % 2 == 1) {
                        // Bold title portion
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(part)
                        }
                    } else {
                        // Normal text
                        append(part)
                    }
                }
            }
        }
    }
}
