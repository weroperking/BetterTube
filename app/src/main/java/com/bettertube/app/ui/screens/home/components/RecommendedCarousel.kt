package com.bettertube.app.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bettertube.app.ui.screens.home.HomeMockData
import com.bettertube.app.ui.screens.home.model.RecommendedItem
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary

@Composable
fun RecommendedCarousel(
    items: List<RecommendedItem>,
    onItemClick: (RecommendedItem) -> Unit
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            RecommendedCard(item = item, onClick = { onItemClick(item) })
        }
    }
}

@Composable
private fun RecommendedCard(
    item: RecommendedItem,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val placeholderBg = if (isDark) SurfaceDark else BgSecondary

    Surface(
        color = Color.Transparent,
        modifier = Modifier
            .width(240.dp)
            .height(180.dp)
            .clickable(onClick = onClick)
            .testTag("recommended_card_${item.id}")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .size(width = 240.dp, height = 135.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = 0.dp,
                            bottomEnd = 0.dp
                        )
                    )
            ) {
                Surface(
                    color = placeholderBg,
                    modifier = Modifier.fillMaxSize()
                ) {}
                AsyncImage(
                    model = item.thumbnailUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.durationText} · ${item.sourcePlatform}",
                fontSize = 11.sp,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun RecommendedCarouselPreview() {
    BetterTubeTheme {
        RecommendedCarousel(
            items = HomeMockData.recommended,
            onItemClick = {}
        )
    }
}
