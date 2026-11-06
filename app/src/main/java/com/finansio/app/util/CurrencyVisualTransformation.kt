package com.finansio.app.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

class CurrencyVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val originalText = text.text.filter { it.isDigit() }
        if (originalText.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val symbols = DecimalFormatSymbols(Locale("id", "ID"))
        val formatter = DecimalFormat("#,###", symbols)
        val formattedText = formatter.format(originalText.toLongOrNull() ?: 0L)

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (originalText.isEmpty()) return 0
                var originalPos = 0
                var transformedPos = 0
                while (originalPos < offset && transformedPos < formattedText.length) {
                    if (formattedText[transformedPos].isDigit()) {
                        originalPos++
                    }
                    transformedPos++
                }
                return transformedPos
            }

            override fun transformedToOriginal(offset: Int): Int {
                var originalPos = 0
                var transformedPos = 0
                while (transformedPos < offset && transformedPos < formattedText.length) {
                    if (formattedText[transformedPos].isDigit()) {
                        originalPos++
                    }
                    transformedPos++
                }
                return originalPos
            }
        }

        return TransformedText(AnnotatedString(formattedText), offsetMapping)
    }
}
