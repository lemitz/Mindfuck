package com.mindfuck.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mindfuck.app.MindfuckApp
import com.mindfuck.app.charts.ChartPalette
import com.mindfuck.app.charts.ChartRenderer
import com.mindfuck.app.charts.LegendMark
import com.mindfuck.app.ui.theme.AppFonts
import com.mindfuck.app.ui.theme.LocalMoodColors

@Composable
fun app(): MindfuckApp = LocalContext.current.applicationContext as MindfuckApp

val LEVEL_LABELS = listOf("None", "Mild", "Moderate", "Severe")

/** None / Mild / Moderate / Severe. Tapping the selected option clears it. */
@Composable
fun LevelSelector(value: Int?, onChange: (Int?) -> Unit, modifier: Modifier = Modifier) {
    val mood = LocalMoodColors.current
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth()) {
        LEVEL_LABELS.forEachIndexed { i, label ->
            SegmentedButton(
                selected = value == i,
                onClick = { onChange(if (value == i) null else i) },
                shape = SegmentedButtonDefaults.itemShape(i, LEVEL_LABELS.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = mood.levels[i],
                    activeContentColor = mood.onLevels[i],
                ),
                icon = {},
                label = {
                    Text(
                        label.uppercase(),
                        maxLines = 1,
                        overflow = TextOverflow.Visible,
                        fontSize = 12.sp,
                        letterSpacing = 0.3.sp,
                        fontWeight = if (value == i) FontWeight.SemiBold else FontWeight.Normal,
                    )
                },
            )
        }
    }
}

/** No / Yes. Tapping the selected option clears it. */
@Composable
fun YesNoSelector(value: Boolean?, onChange: (Boolean?) -> Unit, modifier: Modifier = Modifier) {
    val mood = LocalMoodColors.current
    val options = listOf(false to "NO", true to "YES")
    SingleChoiceSegmentedButtonRow(modifier.fillMaxWidth(0.55f)) {
        options.forEachIndexed { i, (v, label) ->
            SegmentedButton(
                selected = value == v,
                onClick = { onChange(if (value == v) null else v) },
                shape = SegmentedButtonDefaults.itemShape(i, options.size),
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = if (v) mood.levels[2] else mood.levels[0],
                    activeContentColor = if (v) mood.onLevels[2] else mood.onLevels[0],
                ),
                icon = {},
                label = { Text(label, fontSize = 12.sp) },
            )
        }
    }
}

@Composable
fun Question(
    title: String,
    modifier: Modifier = Modifier,
    horizontalPadding: Dp = 20.dp,
    content: @Composable () -> Unit,
) {
    Column(modifier.padding(horizontal = horizontalPadding, vertical = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Normal)
        content()
    }
}

/** ‹ title › header used by Log, Calendar and Graph. */
@Composable
fun PagerHeader(
    title: String,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    nextEnabled: Boolean,
    titleColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    onTitleClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onPrev) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Previous", Modifier.height(32.dp))
        }
        val clickable = if (onTitleClick != null) {
            Modifier.clickableNoRipple(onTitleClick)
        } else Modifier
        Text(
            title,
            Modifier
                .weight(1f)
                .then(clickable),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.headlineSmall,
            color = titleColor,
        )
        IconButton(onClick = onNext, enabled = nextEnabled) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Next", Modifier.height(32.dp))
        }
    }
}

@Composable
fun Modifier.clickableNoRipple(onClick: () -> Unit): Modifier {
    val source = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(interactionSource = source, indication = null, onClick = onClick)
}

@Composable
fun rememberChartRenderer(): ChartRenderer {
    val context = LocalContext.current
    val density = LocalDensity.current.density
    val mood = LocalMoodColors.current
    return remember(density, mood) {
        ChartRenderer(
            u = density,
            typeface = AppFonts.typeface(context),
            p = ChartPalette(
                text = mood.chartText.toArgb(),
                grid = mood.grid.toArgb(),
                missing = mood.missing.toArgb(),
                sleepBar = mood.sleepBar.toArgb(),
                avgLine = mood.chartText.toArgb(),
                levels = mood.levels.map { it.toArgb() }.toIntArray(),
                yes = mood.yes.toArgb(),
                no = mood.chartText.copy(alpha = 0.7f).toArgb(),
            ),
        )
    }
}

/**
 * A titled card that hosts a [ChartRenderer] drawing of [heightPx] pixels,
 * with an optional legend line drawn under it by the same renderer.
 */
@Composable
fun ChartCard(
    title: String,
    heightPx: Float,
    chart: ChartRenderer,
    legend: List<Pair<LegendMark, String>> = emptyList(),
    subtitle: String? = null,
    draw: (android.graphics.Canvas, Float) -> Unit,
) {
    val mood = LocalMoodColors.current
    val density = LocalDensity.current
    Surface(color = mood.chartBackground, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(start = 12.dp, end = 16.dp, top = 14.dp, bottom = 10.dp)) {
            Text(
                title,
                Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = mood.chartText,
            )
            if (subtitle != null) {
                Text(
                    subtitle,
                    Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                    color = mood.chartText,
                )
            }
            BoxWithConstraints(Modifier.fillMaxWidth().padding(top = 10.dp)) {
                val widthPx = with(density) { maxWidth.toPx() }
                val legendH = remember(legend, widthPx, chart) { chart.legendHeight(legend, widthPx) }
                val h: Dp = with(density) { (heightPx + legendH).toDp() }
                Canvas(Modifier.fillMaxWidth().height(h)) {
                    drawIntoCanvas {
                        draw(it.nativeCanvas, size.width)
                        if (legend.isNotEmpty()) chart.drawLegend(it.nativeCanvas, 0f, heightPx, size.width, legend)
                    }
                }
            }
        }
    }
}
