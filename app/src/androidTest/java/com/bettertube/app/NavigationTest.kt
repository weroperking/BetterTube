package com.bettertube.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bettertube.app.ui.theme.BetterTubeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun bottomNavigation_displaysAllItems_andNavigatesToDownloads() {
        composeTestRule.setContent {
            BetterTubeTheme {
                MainAppScreen()
            }
        }

        // Verify that the Bottom Navigation bar displays the correct 4 items
        composeTestRule.onNodeWithText("Home").assertIsDisplayed()
        composeTestRule.onNodeWithText("Downloads").assertIsDisplayed()
        composeTestRule.onNodeWithText("Music").assertIsDisplayed()
        composeTestRule.onNodeWithText("Files").assertIsDisplayed()

        // Verify that the initial screen shows "Home Screen"
        composeTestRule.onNodeWithText("Home Screen").assertIsDisplayed()

        // Click "Downloads"
        composeTestRule.onNodeWithText("Downloads").performClick()

        // Verify that clicking "Downloads" navigates to the Downloads screen
        composeTestRule.onNodeWithText("Downloads Screen").assertIsDisplayed()
    }
}
