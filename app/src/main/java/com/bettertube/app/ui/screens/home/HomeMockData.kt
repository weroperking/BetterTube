package com.bettertube.app.ui.screens.home

import androidx.compose.ui.graphics.Color
import com.bettertube.app.ui.screens.home.model.Platform
import com.bettertube.app.ui.screens.home.model.RecommendedItem
import com.bettertube.app.ui.screens.home.model.TrendingItem
import com.bettertube.app.ui.theme.BrandPrimary
import com.example.R

object HomeMockData {
    val platforms: List<Platform> = listOf(
        Platform(
            name = "YouTube",
            iconRes = R.drawable.ic_platform_youtube,
            brandColor = Color(0xFFFF0000),
            urlScheme = "youtube.com"
        ),
        Platform(
            name = "Facebook",
            iconRes = R.drawable.ic_platform_facebook,
            brandColor = Color(0xFF1877F2),
            urlScheme = "facebook.com"
        ),
        Platform(
            name = "Instagram",
            iconRes = R.drawable.ic_platform_instagram,
            brandColor = Color(0xFFE4405F),
            urlScheme = "instagram.com"
        ),
        Platform(
            name = "Twitter",
            iconRes = R.drawable.ic_platform_twitter,
            brandColor = Color(0xFF1DA1F2),
            urlScheme = "twitter.com"
        ),
        Platform(
            name = "TikTok",
            iconRes = R.drawable.ic_platform_tiktok,
            brandColor = Color(0xFF000000),
            urlScheme = "tiktok.com"
        ),
        Platform(
            name = "Vimeo",
            iconRes = R.drawable.ic_platform_vimeo,
            brandColor = Color(0xFF1AB7EA),
            urlScheme = "vimeo.com"
        ),
        Platform(
            name = "Dailymotion",
            iconRes = R.drawable.ic_platform_dailymotion,
            brandColor = Color(0xFF0066DC),
            urlScheme = "dailymotion.com"
        ),
        Platform(
            name = "More",
            iconRes = R.drawable.ic_platform_more,
            brandColor = BrandPrimary,
            urlScheme = ""
        )
    )

    val recommended: List<RecommendedItem> = listOf(
        RecommendedItem(
            id = "rec_1",
            title = "The Most Beautiful Places in the World",
            channel = "Earth Voyager",
            durationText = "4:15",
            thumbnailUrl = "https://picsum.photos/seed/rec_1/400/225",
            sourcePlatform = "YouTube"
        ),
        RecommendedItem(
            id = "rec_2",
            title = "Best Hip Hop Mix 2024",
            channel = "BeatNation",
            durationText = "45:10",
            thumbnailUrl = "https://picsum.photos/seed/rec_2/400/225",
            sourcePlatform = "Vimeo"
        ),
        RecommendedItem(
            id = "rec_3",
            title = "Funny Moments Caught on Camera",
            channel = "LaughHub",
            durationText = "12:05",
            thumbnailUrl = "https://picsum.photos/seed/rec_3/400/225",
            sourcePlatform = "TikTok"
        ),
        RecommendedItem(
            id = "rec_4",
            title = "Top 10 Goals of the Season 2024",
            channel = "SportCenter",
            durationText = "8:20",
            thumbnailUrl = "https://picsum.photos/seed/rec_4/400/225",
            sourcePlatform = "Facebook"
        ),
        RecommendedItem(
            id = "rec_5",
            title = "Cooking Masterclass: Italian Pasta",
            channel = "ChefBella",
            durationText = "18:32",
            thumbnailUrl = "https://picsum.photos/seed/rec_5/400/225",
            sourcePlatform = "Dailymotion"
        )
    )

    val trending: List<TrendingItem> = listOf(
        TrendingItem(
            id = "trend_1",
            title = "Epic Drone Footage of Iceland",
            channel = "WildVisions",
            viewsText = "1.8M views",
            durationText = "14:22",
            thumbnailUrl = "https://picsum.photos/seed/trend_1/400/225",
            rank = 1
        ),
        TrendingItem(
            id = "trend_2",
            title = "Lo-Fi Beats to Study To",
            channel = "ChillHop Cafe",
            viewsText = "3.4M views",
            durationText = "1:20:15",
            thumbnailUrl = "https://picsum.photos/seed/trend_2/400/225",
            rank = 2
        ),
        TrendingItem(
            id = "trend_3",
            title = "How to Build a Gaming PC",
            channel = "TechForge",
            viewsText = "890K views",
            durationText = "22:40",
            thumbnailUrl = "https://picsum.photos/seed/trend_3/400/225",
            rank = 3
        ),
        TrendingItem(
            id = "trend_4",
            title = "Nature Documentary: Ocean Life",
            channel = "DeepBlue Planet",
            viewsText = "2.1M views",
            durationText = "35:10",
            thumbnailUrl = "https://picsum.photos/seed/trend_4/400/225",
            rank = 4
        ),
        TrendingItem(
            id = "trend_5",
            title = "Stand-Up Comedy Special 2024",
            channel = "ComedyStage",
            viewsText = "750K views",
            durationText = "28:45",
            thumbnailUrl = "https://picsum.photos/seed/trend_5/400/225",
            rank = 5
        )
    )
}
