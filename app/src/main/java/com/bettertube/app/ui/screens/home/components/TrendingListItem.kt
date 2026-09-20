package com.bettertube.app.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bettertube.app.ui.screens.home.HomeMockData
import com.bettertube.app.ui.screens.home.model.TrendingItem
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.Divider
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary

@Composable
fun TrendingListItem(
    item: TrendingItem,
    onClick: (TrendingItem) -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val placeholderBg = if (isDark) SurfaceDark else BgSecondary

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onClick(item) }
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag("trending_item_${item.id}"),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank number on left: 24sp Bold, BrandPrimary, 32dp wide box
            Box(
                modifier = Modifier.width(32.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = "${item.rank}",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary,
                    modifier = Modifier.testTag("trending_rank_${item.rank}")
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Thumbnail: 100dp x 56dp, 8dp corner radius
            Box(
                modifier = Modifier
                    .size(width = 100.dp, height = 56.dp)
                    .clip(RoundedCornerShape(8.dp))
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

            Spacer(modifier = Modifier.width(12.dp))

            // Column (weight 1f): Title 14sp Medium max 2 lines, then views & duration 11sp
            Column(
                modifier = Modifier.weight(1f)
            ) {
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
                    text = "${item.viewsText} · ${item.durationText}",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Trailing: IconButton with Icons.Default.MoreVert 20dp tint TextSecondary
            IconButton(
                onClick = { /* More options */ },
                modifier = Modifier
                    .size(36.dp)
                    .testTag("trending_more_${item.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More",
                    modifier = Modifier.size(20.dp),
                    tint = TextSecondary
                )
            }
        }

        // Divider between rows: 1dp height, Divider color, 16dp start padding, 0 end padding
        HorizontalDivider(
            modifier = Modifier.padding(start = 16.dp),
            thickness = 1.dp,
            color = Divider
        )
    }
}

@Preview(showBackground = true)
@Composable
fun TrendingListItemPreview() {
    BetterTubeTheme {
        TrendingListItem(
            item = HomeMockData.trending.first(),
            onClick = {}
        )
    }
}
