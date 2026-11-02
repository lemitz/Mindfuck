package com.mindfuck.app.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.mindfuck.app.ui.LEVEL_LABELS
import com.mindfuck.app.ui.PagerHeader
import com.mindfuck.app.ui.app
import com.mindfuck.app.ui.theme.LocalMoodColors
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

private val MONTH_FMT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH)
private val WEEKDAYS = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

@Composable
fun CalendarScreen(onOpenDay: (Long) -> Unit) {
    val now = YearMonth.now()
    var monthIndex by rememberSaveable { mutableIntStateOf(now.year * 12 + now.monthValue - 1) }
    val month = YearMonth.of(monthIndex / 12, monthIndex % 12 + 1)
    val mood = LocalMoodColors.current

    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
        PagerHeader(
            title = month.format(MONTH_FMT),
            onPrev = { monthIndex-- },
            onNext = { monthIndex++ },
            nextEnabled = month < now,
        )
        HorizontalDivider()
        Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            WEEKDAYS.forEach {
                Text(
                    it, Modifier.weight(1f), textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        AnimatedContent(
            targetState = monthIndex,
            transitionSpec = {
                val dir = if (targetState > initialState) 1 else -1
                (slideInHorizontally(tween(260)) { dir * it / 3 } + fadeIn(tween(200))) togetherWith
                    (slideOutHorizontally(tween(260)) { -dir * it / 3 } + fadeOut(tween(140)))
            },
            label = "month",
        ) { idx ->
            MonthGrid(YearMonth.of(idx / 12, idx % 12 + 1), onOpenDay)
        }

        Spacer(Modifier.height(20.dp))
        Text(
            "Color = most severe symptom of the day. Dot = has notes.",
            Modifier.padding(horizontal = 20.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LEVEL_LABELS.forEachIndexed { i, label ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(14.dp).clip(RoundedCornerShape(4.dp)).background(mood.levels[i]))
                    Text(" $label", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun MonthGrid(month: YearMonth, onOpenDay: (Long) -> Unit) {
    val repo = app().repository
    val first = month.atDay(1)
    val last = month.atEndOfMonth()
    val today = LocalDate.now()
    val mood = LocalMoodColors.current
    val range by remember(month) { repo.observeRange(first.toEpochDay(), last.toEpochDay()) }
        .collectAsState(initial = null)

    Column {
        val lead = first.dayOfWeek.value - 1
        val weeks = (lead + month.lengthOfMonth() + 6) / 7
        Column(Modifier.padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            for (w in 0 until weeks) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (d in 0 until 7) {
                        val idx = w * 7 + d - lead
                        Box(Modifier.weight(1f).aspectRatio(0.9f)) {
                            if (idx in 0 until month.lengthOfMonth()) {
                                val date = first.plusDays(idx.toLong())
                                val entry = range?.entry(date.toEpochDay())
                                val level = entry?.maxLevel
                                val future = date > today
                                val bg by animateColorAsState(
                                    when {
                                        level != null -> mood.levels[level]
                                        entry?.hasAnyData == true -> mood.grid
                                        else -> Color.Transparent
                                    },
                                    tween(220), label = "cell",
                                )
                                val fg = if (level != null) mood.onLevels[level] else MaterialTheme.colorScheme.onSurface
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .then(
                                            if (date == today) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(10.dp))
                                            else Modifier,
                                        )
                                        .clickable(enabled = !future) { onOpenDay(date.toEpochDay()) },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        date.dayOfMonth.toString(),
                                        color = if (future) fg.copy(alpha = 0.35f) else fg,
                                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyLarge,
                                    )
                                    if (entry?.notes?.isNotBlank() == true) {
                                        Box(
                                            Modifier
                                                .align(Alignment.BottomCenter)
                                                .padding(bottom = 5.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(fg),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        val logged = range?.entries?.values?.count { it.hasAnyData }
        Text(
            if (logged == null) " " else "Logged $logged of ${month.lengthOfMonth()} days",
            Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
