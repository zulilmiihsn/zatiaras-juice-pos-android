package com.zatiaras.pos.feature.reports.presentation.chat

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.zatiaras.pos.core.ui.theme.InfoBlue
import com.zatiaras.pos.core.ui.theme.ProfitGreenDark

fun parseMarkdown(text: String): AnnotatedString {
    val highlightColor = ProfitGreenDark

    return buildAnnotatedString {
        val boldRegex = """\*\*(.*?)\*\*|__(.*?)__""".toRegex()
        val italicRegex = """\*(.*?)\*|_(.*?)_""".toRegex()
        val currencyRegex = """Rp\s?[0-9.]+""".toRegex()
        val percentRegex = """[0-9.]+%""".toRegex()

        var currentIdx = 0
        val tokens = (
            boldRegex.findAll(text).map { it to "bold" } +
                italicRegex.findAll(text).map { it to "italic" } +
                currencyRegex.findAll(text).map { it to "currency" } +
                percentRegex.findAll(text).map { it to "percent" }
            )
            .sortedBy { it.first.range.first }
            .toList()

        tokens.forEach { (match, type) ->
            if (match.range.first > currentIdx) {
                append(text.substring(currentIdx, match.range.first))
            }

            when (type) {
                "bold" -> {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(match.groupValues[1].ifEmpty { match.groupValues[2] })
                    }
                }
                "italic" -> {
                    withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                        append(match.groupValues[1].ifEmpty { match.groupValues[2] })
                    }
                }
                "currency" -> {
                    withStyle(
                        SpanStyle(
                            color = highlightColor,
                            fontWeight = FontWeight.ExtraBold,
                        ),
                    ) {
                        append(match.value)
                    }
                }
                "percent" -> {
                    withStyle(
                        SpanStyle(
                            color = InfoBlue,
                            fontWeight = FontWeight.Bold,
                        ),
                    ) {
                        append(match.value)
                    }
                }
            }
            currentIdx = match.range.last + 1
        }

        if (currentIdx < text.length) {
            append(text.substring(currentIdx))
        }
    }
}
