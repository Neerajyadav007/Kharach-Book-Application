package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrencyFormatter
import com.example.model.MonthlyTrendPoint
import com.example.ui.theme.PlusJakartaSansFamily
import kotlin.math.max

@Composable
fun MonthlyReportChart(
    dataPoints: List<MonthlyTrendPoint>,
    currencyUnit: String,
    timeframe: String,
    onTimeframeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(dataPoints) {
        mutableStateOf<Int?>(if (dataPoints.isNotEmpty()) dataPoints.size - 1 else null)
    }

    val activeDataPoint = selectedIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.lastOrNull()

    // Calculate maximum value across all inflows and outflows for bar scaling
    val maxBarValue = remember(dataPoints) {
        val peak = dataPoints.maxOfOrNull { max(it.inflow, it.outflow) } ?: 1.0
        if (peak <= 0.0) 100.0 else peak
    }

    val totalInflow = remember(dataPoints) { dataPoints.sumOf { it.inflow } }
    val totalOutflow = remember(dataPoints) { dataPoints.sumOf { it.outflow } }
    val netCapitalFlow = totalInflow - totalOutflow

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_financial_report_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with title and timeframe switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.BarChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MONTHLY REPORT",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Income vs. Expenses Capital Flow",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaSansFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Timeframe Selector [7D | 6M | 1Y]
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf("7D" to "7D", "6M" to "6M", "1Y" to "1Y").forEach { (tf, label) ->
                            val isSelected = timeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSelected) MaterialTheme.colorScheme.surfaceContainerHigh else Color.Transparent)
                                    .clickable { onTimeframeChange(tf) }
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Metric Summary Strip (Income, Expense, Net Flow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Inflow Chip
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF10B981))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOTAL INFLOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(totalInflow, currencyUnit, includeDecimals = false),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFF10B981)
                        )
                    }
                }

                // Outflow Chip
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF43F5E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "TOTAL OUTFLOW",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = CurrencyFormatter.format(totalOutflow, currencyUnit, includeDecimals = false),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color(0xFFF43F5E)
                        )
                    }
                }

                // Net Flow Chip
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (netCapitalFlow >= 0) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF43F5E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "NET CAPITAL",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        val sign = if (netCapitalFlow >= 0) "+" else ""
                        Text(
                            text = "$sign${CurrencyFormatter.format(netCapitalFlow, currencyUnit, includeDecimals = false)}",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (netCapitalFlow >= 0) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF43F5E)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas / Bar Area
            if (dataPoints.isEmpty()) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No Monthly Data Available",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Add transactions to generate live monthly capital flow charts",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Dual comparative bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    dataPoints.forEachIndexed { index, point ->
                        val isSelected = selectedIndex == index
                        val inflowFraction = (point.inflow / maxBarValue).toFloat().coerceIn(0.04f, 1f)
                        val outflowFraction = (point.outflow / maxBarValue).toFloat().coerceIn(0.04f, 1f)

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom,
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable { selectedIndex = index }
                                .padding(horizontal = 2.dp)
                        ) {
                            // Dual bar columns side-by-side
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.Bottom
                            ) {
                                // Inflow Bar (Green/Emerald)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(inflowFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFF34D399),
                                                    Color(0xFF059669)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )

                                Spacer(modifier = Modifier.width(3.dp))

                                // Outflow Bar (Rose/Coral)
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(outflowFraction)
                                        .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                                        .background(
                                            Brush.verticalGradient(
                                                colors = listOf(
                                                    Color(0xFFFB7185),
                                                    Color(0xFFE11D48)
                                                )
                                            )
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color.Transparent,
                                            shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)
                                        )
                                )
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // X-axis Month Label
                            Text(
                                text = point.month,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                ),
                                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Inspection Banner for selected month
            AnimatedVisibility(
                visible = activeDataPoint != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (activeDataPoint != null) {
                    val net = activeDataPoint.inflow - activeDataPoint.outflow
                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = activeDataPoint.fullMonth.ifBlank { activeDataPoint.month },
                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "In: ${CurrencyFormatter.format(activeDataPoint.inflow, currencyUnit, includeDecimals = false)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF10B981)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Out: ${CurrencyFormatter.format(activeDataPoint.outflow, currencyUnit, includeDecimals = false)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFF43F5E)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                val netSign = if (net >= 0) "+" else ""
                                Text(
                                    text = "Net: $netSign${CurrencyFormatter.format(net, currencyUnit, includeDecimals = false)}",
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (net >= 0) MaterialTheme.colorScheme.primaryContainer else Color(0xFFF43F5E)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
