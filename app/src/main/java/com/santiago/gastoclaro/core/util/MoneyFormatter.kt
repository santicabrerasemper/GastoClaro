package com.santiago.gastoclaro.core.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

private val argentinaLocale = Locale.forLanguageTag("es-AR")

fun Long.formatCurrency(): String = NumberFormat.getCurrencyInstance(argentinaLocale)
    .format(BigDecimal.valueOf(this, 2))

fun Long.formatUsdCurrency(): String = "USD " + NumberFormat.getNumberInstance(argentinaLocale)
    .apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    .format(BigDecimal.valueOf(this, 2))

fun Long.formatPlainAmount(): String = BigDecimal(this)
    .divide(BigDecimal(100))
    .setScale(2, RoundingMode.UNNECESSARY)
    .stripTrailingZeros()
    .toPlainString()
    .replace('.', ',')

fun formatMoneyInput(raw: String): String {
    val clean = raw.trim().replace("$", "").replace(" ", "")
    if (clean.isBlank()) return ""

    val decimalSeparatorIndex = decimalSeparatorIndex(clean)
    val integerDigits = if (decimalSeparatorIndex >= 0) {
        clean.take(decimalSeparatorIndex).filter(Char::isDigit)
    } else {
        clean.filter(Char::isDigit)
    }
    val decimals = if (decimalSeparatorIndex >= 0) {
        clean.drop(decimalSeparatorIndex + 1).filter(Char::isDigit).take(2)
    } else {
        ""
    }

    val groupedInteger = integerDigits.trimStart('0')
        .ifBlank { "0" }
        .reversed()
        .chunked(3)
        .joinToString(".")
        .reversed()

    return if (decimalSeparatorIndex >= 0) "$groupedInteger,$decimals" else groupedInteger
}

fun parseMoneyToCents(raw: String): Long? {
    val clean = raw.trim().replace("$", "").replace(" ", "")
    if (clean.isBlank()) return null
    val normalized = when {
        clean.contains(',') -> clean.replace(".", "").replace(',', '.')
        clean.count { it == '.' } > 1 -> clean.replace(".", "")
        clean.contains('.') && clean.substringAfterLast('.').length > 2 -> clean.replace(".", "")
        else -> clean
    }
    return runCatching {
        BigDecimal(normalized)
            .setScale(2, RoundingMode.HALF_UP)
            .movePointRight(2)
            .longValueExact()
    }.getOrNull()
}

private fun decimalSeparatorIndex(clean: String): Int {
    val commaIndex = clean.lastIndexOf(',')
    if (commaIndex >= 0) return commaIndex

    val dotIndex = clean.lastIndexOf('.')
    if (dotIndex < 0) return -1

    val dotCount = clean.count { it == '.' }
    val decimals = clean.length - dotIndex - 1
    return if (dotCount == 1 && decimals in 0..2) dotIndex else -1
}
