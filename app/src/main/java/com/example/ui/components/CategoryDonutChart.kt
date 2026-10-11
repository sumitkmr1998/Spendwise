package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.CategorySpendItem
import java.util.Locale
import kotlin.math.atan2

@Composable
fun CategoryDonutChart(
    categoryItems: List<CategorySpendItem>,
    modifier: Modifier = Modifier
) {
    if (categoryItems.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No expenses recorded for this date range.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        return
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val totalSpent = remember(categoryItems) { categoryItems.sumOf { it.totalAmount } }

    var animationPlayed by remember { mutableStateOf(false) }
    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "donut_anim"
    )

    LaunchedEffect(key1 = categoryItems) {
        animationPlayed = true
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(240.dp)
                .testTag("category_donut_chart"),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pointerInput(categoryItems) {
                        detectTapGestures { offset ->
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val dx = offset.x - center.x
                            val dy = offset.y - center.y
                            val radius = size.width / 2f
                            val dist = kotlin.math.sqrt(dx * dx + dy * dy)

                            if (dist in (radius * 0.45f)..(radius * 1.05f)) {
                                var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                if (angle < 0) angle += 360f
                                val adjustedAngle = (angle + 90f) % 360f

                                var cumulative = 0f
                                for (i in categoryItems.indices) {
                                    val sweep = (categoryItems[i].percentage / 100f) * 360f
                                    if (adjustedAngle in cumulative..(cumulative + sweep)) {
                                        selectedIndex = if (selectedIndex == i) null else i
                                        break
                                    }
                                    cumulative += sweep
                                }
                            } else {
                                selectedIndex = null
                            }
                        }
                    }
            ) {
                val strokeWidth = 36.dp.toPx()
                val diameter = size.minDimension - strokeWidth
                val topLeft = Offset(
                    (size.width - diameter) / 2f,
                    (size.height - diameter) / 2f
                )
                val arcSize = Size(diameter, diameter)

                var startAngle = -90f

                categoryItems.forEachIndexed { index, item ->
                    val sweepAngle = (item.percentage / 100f) * 360f * animatedProgress
                    val isSelected = selectedIndex == index
                    val currentStroke = if (isSelected) strokeWidth * 1.25f else strokeWidth

                    drawArc(
                        color = item.color,
                        startAngle = startAngle,
                        sweepAngle = sweepAngle.coerceAtLeast(1.5f),
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(
                            width = currentStroke,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweepAngle
                }
            }

            // Center Info
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(horizontal = 24.dp)
            ) {
                if (selectedIndex != null && selectedIndex!! < categoryItems.size) {
                    val selected = categoryItems[selectedIndex!!]
                    Text(
                        text = selected.categoryName,
                        style = MaterialTheme.typography.labelMedium,
                        color = selected.color,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", selected.totalAmount)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", selected.percentage)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Total Spent",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", totalSpent)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap slice for details",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Legend chips row
        FlowLegend(
            categoryItems = categoryItems,
            selectedIndex = selectedIndex,
            onSelect = { idx ->
                selectedIndex = if (selectedIndex == idx) null else idx
            }
        )
    }
}

@Composable
private fun FlowLegend(
    categoryItems: List<CategorySpendItem>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val chunked = categoryItems.chunked(2)
        chunked.forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowItems.forEach { item ->
                    val index = categoryItems.indexOf(item)
                    val isSelected = selectedIndex == index
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelect(index) },
                        shape = MaterialTheme.shapes.small,
                        color = if (isSelected) item.color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        tonalElevation = if (isSelected) 4.dp else 0.dp
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = item.color,
                                modifier = Modifier.size(10.dp)
                            ) {}
                            Spacer(modifier = Modifier.width(6.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.categoryName,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                                Text(
                                    text = "$${String.format(Locale.US, "%.2f", item.totalAmount)} (${String.format(Locale.US, "%.0f", item.percentage)}%)",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                if (rowItems.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}
