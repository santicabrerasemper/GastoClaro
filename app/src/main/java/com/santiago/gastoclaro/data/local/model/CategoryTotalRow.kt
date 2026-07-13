package com.santiago.gastoclaro.data.local.model

data class CategoryTotalRow(
    val categoryId: Long,
    val categoryName: String,
    val categoryEmoji: String,
    val colorArgb: Int,
    val amountCents: Long
)
