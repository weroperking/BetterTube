package com.bettertube.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.bettertube.app.ui.theme.BetterTubeTheme

fun Modifier.shimmer(): Modifier = composed {
    val transition = rememberInfiniteTransition(label = "shimmerTransition")
    val translateAnim by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )

    val isDark = isSystemInDarkTheme()
    val shimmerColors = if (isDark) {
        listOf(
            Color(0xFF2C2C2C),
            Color(0xFF3D3D3D),
            Color(0xFF2C2C2C)
        )
    } else {
        listOf(
            Color(0xFFE0E0E0),
            Color(0xFFEEEEEE),
            Color(0xFFE0E0E0)
        )
    }

    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnim - 500f, translateAnim - 500f),
        end = Offset(translateAnim, translateAnim)
    )

    background(brush)
}

@Composable
fun RecommendedCardShimmer(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .width(160.dp)
            .padding(end = 12.dp)
            .testTag("recommended_shimmer_card")
    ) {
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(100.dp)
                .clip(RoundedCornerShape(12.dp))
                .shimmer()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )
        Spacer(modifier = Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(80.dp)
                .height(12.dp)
                .clip(RoundedCornerShape(4.dp))
                .shimmer()
        )
    }
}

@Composable
fun TrendingRowShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .testTag("trending_shimmer_row"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(width = 90.dp, height = 60.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmer()
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.5f)
                    .height(12.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
        }
    }
}

@Composable
fun DownloadItemShimmer(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .testTag("download_shimmer_item"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(8.dp))
                .shimmer()
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(14.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmer()
            )
            Spacer(modifier = Modifier.height(6.dp))
            Box(
                modifier = Modifier
                    .width(100.dp)
                    .height(10.dp)
                    .clip(RoundedCornerShape(3.dp))
                    .shimmer()
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ShimmerPreview() {
    BetterTubeTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            RecommendedCardShimmer()
            Spacer(modifier = Modifier.height(16.dp))
            TrendingRowShimmer()
            Spacer(modifier = Modifier.height(16.dp))
            DownloadItemShimmer()
        }
    }
}
