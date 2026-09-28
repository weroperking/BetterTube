package com.bettertube.app.ui.screens.home.model

data class RecommendedItem(
    val id: String,
    val title: String,
    val channel: String,
    val durationText: String,
    val thumbnailUrl: String,
    val sourcePlatform: String
)
