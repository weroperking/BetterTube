package com.bettertube.app.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bettertube.app.ui.screens.home.HomeMockData
import com.bettertube.app.ui.screens.home.model.Platform
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BgSecondary
import com.bettertube.app.ui.theme.SurfaceDark
import com.bettertube.app.ui.theme.TextSecondary

@Composable
fun PlatformGrid(
    platforms: List<Platform>,
    onPlatformClick: (Platform) -> Unit
) {
    val row1 = platforms.take(4)
    val row2 = platforms.drop(4).take(4)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            row1.forEach { platform ->
                PlatformCell(platform = platform, onClick = { onPlatformClick(platform) })
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            row2.forEach { platform ->
                PlatformCell(platform = platform, onClick = { onPlatformClick(platform) })
            }
        }
    }
}

@Composable
private fun PlatformCell(
    platform: Platform,
    onClick: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val containerBg = if (isDark) SurfaceDark else BgSecondary

    Column(
        modifier = Modifier
            .size(width = 64.dp, height = 80.dp)
            .clickable(onClick = onClick)
            .testTag("platform_cell_${platform.name.lowercase()}"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = containerBg,
            shadowElevation = 4.dp
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(id = platform.iconRes),
                    contentDescription = platform.name,
                    modifier = Modifier.size(24.dp),
                    tint = platform.brandColor
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = platform.name,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PlatformGridPreview() {
    BetterTubeTheme {
        PlatformGrid(
            platforms = HomeMockData.platforms,
            onPlatformClick = {}
        )
    }
}
