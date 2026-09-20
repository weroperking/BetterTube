package com.bettertube.app.ui.screens.home.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.bettertube.app.ui.theme.BetterTubeTheme
import com.bettertube.app.ui.theme.BrandPrimary

@Composable
fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier
                .semantics { heading() }
                .testTag("section_header_title_${title.lowercase().replace(" ", "_")}")
        )
        Text(
            text = "See All >",
            fontSize = 12.sp,
            fontWeight = FontWeight.Normal,
            color = BrandPrimary,
            modifier = Modifier
                .clickable(onClick = onSeeAllClick)
                .padding(4.dp)
                .testTag("section_header_see_all_${title.lowercase().replace(" ", "_")}")
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SectionHeaderPreview() {
    BetterTubeTheme {
        SectionHeader(
            title = "Recommended",
            onSeeAllClick = {}
        )
    }
}
