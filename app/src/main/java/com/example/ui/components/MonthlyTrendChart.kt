package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.CurrencyFormatter
import com.example.model.MonthlyTrendPoint
import com.example.ui.theme.PlusJakartaSansFamily

@Composable
fun MonthlyTrendChart(
    dataPoints: List<MonthlyTrendPoint>,
    currencyUnit: String,
    timeframe: String,
    chartMode: String,
    onTimeframeChange: (String) -> Unit,
    onChartModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember(dataPoints) { mutableStateOf<Int?>(dataPoints.size - 1) }

    val activeDataPoint = selectedIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.lastOrNull()

    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(14.dp),
        modifier = modifier
            .fillMaxWidth()
            .testTag("monthly_trend_chart_card")
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header with title and controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.TrendingUp,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "FINANCIAL VELOCITY",
                            style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Monthly Financial Trends",
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
                            val isSel = timeframe == tf
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(if (isSel) MaterialTheme.colorScheme.surfaceVariant else Color.Transparent)
                                    .clickable { onTimeframeChange(tf) }
                                    .padding(horizontal = 9.dp, vertical = 4.dp)
                                    .testTag("timeframe_$tf"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    color = if (isSel) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Sub-bar: Metric toggles [Net Savings | Inflow vs Outflow]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    MetricChip(
                        label = "Net Trend",
                        dotColor = MaterialTheme.colorScheme.primaryContainer,
                        isSelected = chartMode == "NET",
                        onClick = { onChartModeChange("NET") }
                    )
                    MetricChip(
                        label = "Inflow / Outflow",
                        dotColor = MaterialTheme.colorScheme.secondary,
                        isSelected = chartMode == "FLOW",
                        onClick = { onChartModeChange("FLOW") }
                    )
                }

                if (activeDataPoint != null) {
                    Text(
                        text = activeDataPoint.fullMonth,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Interactive Tooltip Callout
            activeDataPoint?.let { point ->
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "NET SAVINGS",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyFormatter.format(point.net, currencyUnit),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "INFLOW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(point.inflow, currencyUnit),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "OUTFLOW",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = CurrencyFormatter.format(point.outflow, currencyUnit),
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // D3/Recharts-Inspired Canvas Line Chart with Monotone Cubic Spline
            val primaryColor = MaterialTheme.colorScheme.primaryContainer
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val gridLineColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f)
            val surfaceColor = MaterialTheme.colorScheme.surface

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .pointerInput(dataPoints) {
                        detectTapGestures { offset ->
                            val n = dataPoints.size
                            if (n > 1) {
                                val step = size.width / (n - 1)
                                val idx = ((offset.x + step / 2) / step).toInt().coerceIn(0, n - 1)
                                selectedIndex = idx
                            }
                        }
                    }
                    .pointerInput(dataPoints) {
                        detectDragGestures { change, _ ->
                            val n = dataPoints.size
                            if (n > 1) {
                                val step = size.width / (n - 1)
                                val idx = ((change.position.x + step / 2) / step).toInt().coerceIn(0, n - 1)
                                selectedIndex = idx
                            }
                        }
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val n = dataPoints.size
                    if (n < 2) return@Canvas

                    val maxVal = dataPoints.maxOf { kotlin.math.max(it.inflow, it.net) } * 1.15
                    val minVal = 0.0

                    fun getY(value: Double): Float {
                        val norm = ((value - minVal) / (maxVal - minVal)).toFloat().coerceIn(0f, 1f)
                        return h - (norm * (h * 0.85f)) - (h * 0.05f)
                    }

                    // 1. Draw horizontal reference gridlines (25%, 50%, 75%)
                    val gridSteps = listOf(0.25f, 0.5f, 0.75f)
                    gridSteps.forEach { fraction ->
                        val y = h - (fraction * h * 0.85f)
                        drawLine(
                            color = gridLineColor,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )
                    }

                    // 2. Map coordinates for Net or Inflow/Outflow
                    val netPoints = dataPoints.mapIndexed { i, p ->
                        Offset(i * (w / (n - 1)), getY(p.net))
                    }
                    val inflowPoints = dataPoints.mapIndexed { i, p ->
                        Offset(i * (w / (n - 1)), getY(p.inflow))
                    }
                    val outflowPoints = dataPoints.mapIndexed { i, p ->
                        Offset(i * (w / (n - 1)), getY(p.outflow))
                    }

                    if (chartMode == "NET") {
                        // Area gradient below Net Curve
                        val areaPath = createMonotoneSplinePath(netPoints)
                        areaPath.lineTo(w, h)
                        areaPath.lineTo(0f, h)
                        areaPath.close()

                        drawPath(
                            path = areaPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.35f),
                                    primaryColor.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )

                        // Smooth Monotone Spline Curve
                        val linePath = createMonotoneSplinePath(netPoints)
                        drawPath(
                            path = linePath,
                            color = primaryColor,
                            style = Stroke(
                                width = 2.5.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        // Data point anchors
                        netPoints.forEachIndexed { i, pt ->
                            val isSelected = selectedIndex == i
                            if (isSelected) {
                                // Draw vertical indicator hairline
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.6f),
                                    start = Offset(pt.x, 0f),
                                    end = Offset(pt.x, h),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                drawCircle(color = surfaceColor, radius = 6.dp.toPx(), center = pt)
                                drawCircle(color = primaryColor, radius = 6.dp.toPx(), center = pt, style = Stroke(2.dp.toPx()))
                                drawCircle(color = primaryColor, radius = 3.dp.toPx(), center = pt)
                            } else {
                                drawCircle(color = surfaceColor, radius = 3.5.dp.toPx(), center = pt)
                                drawCircle(color = primaryColor, radius = 3.5.dp.toPx(), center = pt, style = Stroke(1.5.dp.toPx()))
                            }
                        }
                    } else {
                        // Dual Curve Mode: Inflow (Cyan) & Outflow (Hyper-violet)
                        val inflowLine = createMonotoneSplinePath(inflowPoints)
                        drawPath(
                            path = inflowLine,
                            color = primaryColor,
                            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        val outflowLine = createMonotoneSplinePath(outflowPoints)
                        drawPath(
                            path = outflowLine,
                            color = secondaryColor,
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Selected indicator
                        selectedIndex?.let { idx ->
                            val inPt = inflowPoints.getOrNull(idx)
                            val outPt = outflowPoints.getOrNull(idx)
                            if (inPt != null && outPt != null) {
                                drawLine(
                                    color = primaryColor.copy(alpha = 0.4f),
                                    start = Offset(inPt.x, 0f),
                                    end = Offset(inPt.x, h),
                                    strokeWidth = 1.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                                )
                                drawCircle(color = primaryColor, radius = 5.dp.toPx(), center = inPt)
                                drawCircle(color = secondaryColor, radius = 5.dp.toPx(), center = outPt)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Month Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                dataPoints.forEachIndexed { index, point ->
                    val isSelected = selectedIndex == index
                    Text(
                        text = point.month,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        ),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Computes a smooth monotonic cubic spline through points (identical to D3's curveMonotoneX / Recharts monotone).
 */
private fun createMonotoneSplinePath(points: List<Offset>): Path {
    val path = Path()
    if (points.isEmpty()) return path
    path.moveTo(points[0].x, points[0].y)
    if (points.size == 1) return path

    for (i in 0 until points.size - 1) {
        val p0 = points.getOrElse(i - 1) { points[i] }
        val p1 = points[i]
        val p2 = points[i + 1]
        val p3 = points.getOrElse(i + 2) { p2 }

        val controlPoint1 = Offset(
            x = p1.x + (p2.x - p0.x) / 6f,
            y = p1.y + (p2.y - p0.y) / 6f
        )
        val controlPoint2 = Offset(
            x = p2.x - (p3.x - p1.x) / 6f,
            y = p2.y - (p3.y - p1.y) / 6f
        )

        path.cubicTo(
            controlPoint1.x, controlPoint1.y,
            controlPoint2.x, controlPoint2.y,
            p2.x, p2.y
        )
    }
    return path
}

@Composable
private fun MetricChip(
    label: String,
    dotColor: Color,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                ),
                color = if (isSelected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
