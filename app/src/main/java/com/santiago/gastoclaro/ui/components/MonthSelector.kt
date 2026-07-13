package com.santiago.gastoclaro.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.santiago.gastoclaro.core.util.displayName
import java.time.YearMonth

@Composable
fun MonthSelector(
    period: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier,
    canGoNext: Boolean = period.isBefore(YearMonth.now().plusMonths(24))
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.Rounded.ChevronLeft, contentDescription = "Mes anterior")
        }
        Text(period.displayName(), style = MaterialTheme.typography.titleMedium)
        IconButton(onClick = onNext, enabled = canGoNext) {
            Icon(Icons.Rounded.ChevronRight, contentDescription = "Mes siguiente")
        }
    }
}
