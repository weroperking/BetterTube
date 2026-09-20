package com.bettertube.app.ui.screens.home.model

data class TrendingItem(
    val id: String,
    val title: String,
    val channel: String,
    val viewsText: String,
    val durationText: String,
    val thumbnailUrl: String,
    val rank: Int
)
