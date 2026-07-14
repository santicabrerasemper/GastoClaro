package com.santiago.gastoclaro.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.data.local.model.CategoryTotalRow

@Composable
fun DonutChart(
    items: List<CategoryTotalRow>,
    modifier: Modifier = Modifier
) {
    val total = items.sumOf { it.amountCents }
    if (total <= 0) {
        Box(modifier = modifier.fillMaxWidth().height(180.dp), contentAlignment = Alignment.Center) {
            Text("Todavía no hay gastos para graficar", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(190.dp)) {
                val stroke = 28.dp.toPx()
                val inset = stroke / 2
                var start = -90f
                items.forEach { item ->
                    val sweep = item.amountCents.toFloat() / total.toFloat() * 360f
                    drawArc(
                        color = Color(item.colorArgb),
                        startAngle = start,
                        sweepAngle = sweep.coerceAtLeast(1f),
                        useCenter = false,
                        topLeft = Offset(inset, inset),
                        size = Size(size.width - stroke, size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Butt)
                    )
                    start += sweep
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Gastado", style = MaterialTheme.typography.bodySmall)
                Text(total.formatCurrency(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(16.dp))
        items.take(6).forEach { item ->
            val percent = item.amountCents.toDouble() / total.toDouble() * 100.0
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(modifier = Modifier.size(10.dp), shape = CircleShape, color = Color(item.colorArgb)) {}
                    Spacer(Modifier.width(8.dp))
                    Text("${item.categoryEmoji} ${item.categoryName}")
                }
                Text("${percent.formatPercent()} · ${item.amountCents.formatCurrency()}", fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

private fun Double.formatPercent(): String =
    if (this >= 10.0) "${toInt()}%" else "%.1f%%".format(this)
