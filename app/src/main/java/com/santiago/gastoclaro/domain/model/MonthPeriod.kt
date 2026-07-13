package com.santiago.gastoclaro.domain.model

import java.time.LocalDate
import java.time.YearMonth

fun YearMonth.startEpochDay(): Long = atDay(1).toEpochDay()
fun YearMonth.endEpochDay(): Long = atEndOfMonth().toEpochDay()
fun LocalDate.toYearMonth(): YearMonth = YearMonth.of(year, monthValue)
