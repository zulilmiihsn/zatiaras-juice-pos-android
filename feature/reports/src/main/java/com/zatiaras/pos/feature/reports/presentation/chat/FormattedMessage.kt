package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun FormattedMessage(
    content: String,
    isUser: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    val headerColor = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

    Column(modifier = modifier) {
        content.split("\n").forEach { line ->
            when {
                line.startsWith("---") -> {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 8.dp),
                        color = (if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.outlineVariant)
                            .copy(alpha = 0.3f),
                    )
                }
                line.trim().startsWith("#") -> {
                    HeadingLine(
                        line = line,
                        headerColor = headerColor,
                        textColor = textColor,
                    )
                }
                line.trim().startsWith("- ") || line.trim().startsWith("* ") -> {
                    BulletLine(
                        text = line.trim().substring(2),
                        textColor = textColor,
                    )
                }
                line.trim().let { it.isNotEmpty() && it[0].isDigit() && it.contains(". ") } -> {
                    NumberedLine(
                        line = line,
                        textColor = textColor,
                    )
                }
                line.isBlank() -> {
                    Spacer(modifier = Modifier.height(4.dp))
                }
                else -> {
                    Text(
                        text = parseMarkdown(line),
                        color = textColor,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 2.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeadingLine(
    line: String,
    headerColor: androidx.compose.ui.graphics.Color,
    textColor: androidx.compose.ui.graphics.Color,
) {
    val hashCount = line.takeWhile { it == '#' }.length
    val headingContent = line.dropWhile { it == '#' || it == ' ' }.trim()
    if (hashCount > 0 && headingContent.isNotEmpty()) {
        val style = when (hashCount) {
            1 -> MaterialTheme.typography.headlineSmall
            2 -> MaterialTheme.typography.titleMedium
            3 -> MaterialTheme.typography.titleSmall
            else -> MaterialTheme.typography.bodyMedium
        }
        Text(
            text = parseMarkdown(headingContent),
            style = style,
            fontWeight = FontWeight.Bold,
            color = headerColor,
            modifier = Modifier.padding(top = (8 - hashCount).coerceAtLeast(4).dp, bottom = 4.dp),
        )
    } else {
        Text(
            text = parseMarkdown(line),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}

@Composable
private fun BulletLine(
    text: String,
    textColor: androidx.compose.ui.graphics.Color,
) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = "*",
            color = textColor,
            modifier = Modifier.padding(end = 8.dp),
        )
        Text(
            text = parseMarkdown(text),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun NumberedLine(
    line: String,
    textColor: androidx.compose.ui.graphics.Color,
) {
    val dotIndex = line.indexOf(". ")
    val number = line.take(dotIndex + 2)
    val text = line.drop(dotIndex + 2)
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text(
            text = number,
            color = textColor,
            modifier = Modifier.padding(end = 4.dp),
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = parseMarkdown(text),
            color = textColor,
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
