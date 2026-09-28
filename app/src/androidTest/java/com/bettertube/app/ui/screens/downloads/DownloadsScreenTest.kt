package com.bettertube.app.ui.screens.downloads

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.bettertube.app.domain.model.DownloadStatus
import com.bettertube.app.domain.model.DownloadTask
import com.bettertube.app.domain.model.MediaType
import com.bettertube.app.ui.theme.BetterTubeTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DownloadsScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun downloadsScreen_rendersAllFilterChips() {
        composeTestRule.setContent {
            BetterTubeTheme {
                DownloadsContent(
                    tasks = emptyList(),
                    selectedFilter = "All",
                    onFilterSelected = {},
                    onSettingsClick = {},
                    onPause = {},
                    onResume = {},
                    onCancel = {},
                    onRetry = {},
                    onOpen = {},
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("filter_chip_all").assertExists()
        composeTestRule.onNodeWithTag("filter_chip_video").assertExists()
        composeTestRule.onNodeWithTag("filter_chip_audio").assertExists()
        composeTestRule.onNodeWithTag("filter_chip_images").assertExists()
    }

    @Test
    fun downloadsScreen_showsEmptyStateWhenNoTasks() {
        composeTestRule.setContent {
            BetterTubeTheme {
                DownloadsContent(
                    tasks = emptyList(),
                    selectedFilter = "All",
                    onFilterSelected = {},
                    onSettingsClick = {},
                    onPause = {},
                    onResume = {},
                    onCancel = {},
                    onRetry = {},
                    onOpen = {},
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("empty_downloads_view").assertExists()
        composeTestRule.onNodeWithText("No downloads yet").assertExists()
    }

    @Test
    fun downloadsScreen_filtersByVideo_whenVideoChipSelected() {
        val selectedFilterState = mutableStateOf("All")
        val sampleTasks = listOf(
            DownloadTask(
                id = "vid_1",
                url = "https://example.com/video",
                title = "Video Sample File",
                thumbnailUrl = "",
                formatId = "137",
                status = DownloadStatus.COMPLETE,
                progressPercent = 1f,
                downloadedBytes = 1000L,
                totalBytes = 1000L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = null,
                errorMessage = null,
                createdAtMillis = 1000L,
                mediaType = MediaType.VIDEO
            ),
            DownloadTask(
                id = "aud_1",
                url = "https://example.com/audio",
                title = "Audio Sample Track",
                thumbnailUrl = "",
                formatId = "140",
                status = DownloadStatus.COMPLETE,
                progressPercent = 1f,
                downloadedBytes = 500L,
                totalBytes = 500L,
                speedBytesPerSecond = 0L,
                etaSeconds = 0L,
                outputFilePath = null,
                errorMessage = null,
                createdAtMillis = 2000L,
                mediaType = MediaType.AUDIO
            )
        )

        composeTestRule.setContent {
            BetterTubeTheme {
                DownloadsContent(
                    tasks = sampleTasks,
                    selectedFilter = selectedFilterState.value,
                    onFilterSelected = { selectedFilterState.value = it },
                    onSettingsClick = {},
                    onPause = {},
                    onResume = {},
                    onCancel = {},
                    onRetry = {},
                    onOpen = {},
                    onMoveUp = {},
                    onMoveDown = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Video Sample File").assertExists()
        composeTestRule.onNodeWithText("Audio Sample Track").assertExists()

        composeTestRule.onNodeWithTag("filter_chip_video").performClick()

        composeTestRule.onNodeWithText("Video Sample File").assertExists()
        composeTestRule.onNodeWithText("Audio Sample Track").assertDoesNotExist()
    }
}
