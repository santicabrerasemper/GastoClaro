package com.santiago.gastoclaro.core.util

import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private val locale = Locale.forLanguageTag("es-AR")
private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy", locale)

fun LocalDate.formatDate(): String = format(dateFormatter)

fun YearMonth.displayName(): String {
    val month = this.month.getDisplayName(TextStyle.FULL, locale).replaceFirstChar { it.uppercase(locale) }
    return "$month $year"
}
