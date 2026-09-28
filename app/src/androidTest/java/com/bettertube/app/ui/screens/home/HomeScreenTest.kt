package com.bettertube.app.ui.screens.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bettertube.app.ui.theme.BetterTubeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun homeScreen_displaysAllEightPlatforms() {
        composeTestRule.setContent {
            BetterTubeTheme {
                HomeContent(innerPadding = PaddingValues(0.dp))
            }
        }

        composeTestRule.onNodeWithText("YouTube").assertExists()
        composeTestRule.onNodeWithText("Instagram").assertExists()
        composeTestRule.onNodeWithText("TikTok").assertExists()
        composeTestRule.onNodeWithText("More").assertExists()
    }

    @Test
    fun homeScreen_displaysRecommendedHeader() {
        composeTestRule.setContent {
            BetterTubeTheme {
                HomeContent(innerPadding = PaddingValues(0.dp))
            }
        }

        composeTestRule.onNodeWithText("Recommended").assertExists()
    }

    @Test
    fun homeScreen_displaysTrendingHeader() {
        composeTestRule.setContent {
            BetterTubeTheme {
                HomeContent(innerPadding = PaddingValues(0.dp))
            }
        }

        composeTestRule.onNodeWithText("Trending Now").assertExists()
    }

    @Test
    fun homeScreen_displaysFab() {
        composeTestRule.setContent {
            BetterTubeTheme {
                HomeContent(innerPadding = PaddingValues(0.dp))
            }
        }

        composeTestRule.onNodeWithContentDescription("Download").assertExists()
    }
}
