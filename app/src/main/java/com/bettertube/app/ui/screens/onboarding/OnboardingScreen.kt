package com.bettertube.app.ui.screens.onboarding

import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.HighQuality
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.bettertube.app.ui.navigation.Screen
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary
import com.bettertube.app.ui.theme.TextSecondary
import com.bettertube.app.ui.theme.TextTertiary
import com.bettertube.app.R
import kotlinx.coroutines.launch

data class OnboardingPageData(
    val icon: ImageVector,
    val titleRes: Int,
    val subtitleRes: Int
)

@Composable
fun OnboardingScreen(
    navController: NavController = rememberNavController(),
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pages = listOf(
        OnboardingPageData(
            icon = Icons.Default.Download,
            titleRes = R.string.onboarding_page1_title,
            subtitleRes = R.string.onboarding_page1_sub
        ),
        OnboardingPageData(
            icon = Icons.Default.HighQuality,
            titleRes = R.string.onboarding_page2_title,
            subtitleRes = R.string.onboarding_page2_sub
        ),
        OnboardingPageData(
            icon = Icons.Default.Lock,
            titleRes = R.string.onboarding_page3_title,
            subtitleRes = R.string.onboarding_page3_sub
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    val completeOnboarding = {
        viewModel.markCompleted()
        navController.navigate(Screen.Home.route) {
            popUpTo(Screen.Onboarding.route) { inclusive = true }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp)
            .testTag("onboarding_screen"),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button top right
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = completeOnboarding,
                modifier = Modifier.testTag("onboarding_skip_button")
            ) {
                Text(
                    text = stringResource(R.string.onboarding_skip),
                    color = TextSecondary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Pager
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(3f)
                .testTag("onboarding_pager")
        ) { pageIndex ->
            val page = pages[pageIndex]
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(130.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = page.icon,
                        contentDescription = stringResource(page.titleRes),
                        tint = BrandPrimary,
                        modifier = Modifier.size(72.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Text(
                    text = stringResource(page.titleRes),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.testTag("onboarding_title_$pageIndex")
                )

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = stringResource(page.subtitleRes),
                    fontSize = 15.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                    modifier = Modifier.testTag("onboarding_subtitle_$pageIndex")
                )
            }
        }

        // Pager indicator dots
        Row(
            modifier = Modifier
                .padding(vertical = 24.dp)
                .testTag("onboarding_indicator_row"),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(pages.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) BrandPrimary else TextTertiary)
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.5f))

        // Bottom action button
        val isLastPage = pagerState.currentPage == pages.size - 1
        Button(
            onClick = {
                if (isLastPage) {
                    completeOnboarding()
                } else {
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(pagerState.currentPage + 1)
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("onboarding_action_button")
        ) {
            Text(
                text = if (isLastPage) stringResource(R.string.onboarding_get_started) else stringResource(R.string.onboarding_next),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Preview(showBackground = true)
@Composable
fun OnboardingScreenPreview() {
    BetterTubeTheme {
        OnboardingScreen()
    }
}
