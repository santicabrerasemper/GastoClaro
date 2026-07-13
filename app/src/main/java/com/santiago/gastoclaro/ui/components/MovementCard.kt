package com.santiago.gastoclaro.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.santiago.gastoclaro.core.util.formatCurrency
import com.santiago.gastoclaro.core.util.formatDate
import com.santiago.gastoclaro.core.util.formatUsdCurrency
import com.santiago.gastoclaro.data.local.entity.MovementType
import com.santiago.gastoclaro.data.local.model.MovementRow
import java.time.LocalDate

@Composable
fun MovementCard(
    movement: MovementRow,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
    onDelete: (() -> Unit)? = null
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                modifier = Modifier.size(46.dp),
                shape = CircleShape,
                color = Color(movement.categoryColorArgb).copy(alpha = 0.16f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Text(movement.categoryEmoji)
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    movement.note.ifBlank { movement.categoryName },
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val paymentLabel = movement.paymentMethodName?.let {
                    if (movement.paymentMethodLastDigits.isNullOrBlank()) it else "$it ****${movement.paymentMethodLastDigits}"
                }
                Text(
                    listOfNotNull(
                        movement.categoryName,
                        movement.subcategoryName.takeIf { it.isNotBlank() },
                        if (movement.currency == "USD") movement.currencyAmountCents.formatUsdCurrency() else null,
                        if (movement.isRecurringMonthly) "Mensual" else null,
                        paymentLabel,
                        if (movement.installmentCount > 1) "Cuota ${movement.installmentIndex}/${movement.installmentCount}" else null,
                        LocalDate.ofEpochDay(movement.occurredEpochDay).formatDate()
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(2.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = (if (movement.type == MovementType.EXPENSE) "-" else "+") + movement.amountCents.formatCurrency(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (movement.type == MovementType.EXPENSE) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                )
                if (movement.type == MovementType.EXPENSE && movement.annualizedMonths > 1) {
                    Text(
                        text = "${movement.monthlyImpactCents.formatCurrency()}/mes",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (onDelete != null) {
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = "Eliminar")
                }
            }
        }
    }
}
