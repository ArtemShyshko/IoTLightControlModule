package com.example.lightsapp.ui.mainScreen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Tab
import androidx.compose.material.TabPosition
import androidx.compose.material.TabRow
import androidx.compose.material.TabRowDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.lightsapp.ui.gasControl.GasControlScreen
import com.example.lightsapp.ui.lightControl.LightControlScreen
import com.example.lightsapp.ui.theme.LightsAppTheme
import com.google.accompanist.pager.ExperimentalPagerApi
import com.google.accompanist.pager.HorizontalPager
import com.google.accompanist.pager.pagerTabIndicatorOffset
import com.google.accompanist.pager.rememberPagerState
import kotlinx.coroutines.launch

@OptIn(ExperimentalPagerApi::class)
@Composable
fun MainScreen() {
    val animationScope = rememberCoroutineScope()
    val pagerState = rememberPagerState()
    val pages = listOf("Air","Light")
    val defaultIndicator = @Composable { tabPositions: List<TabPosition> ->
        TabRowDefaults.Indicator(
            Modifier.pagerTabIndicatorOffset(pagerState, tabPositions)
        )
    }

    TabRow(
        modifier = Modifier.height(50.dp),
        selectedTabIndex = pagerState.currentPage,
        indicator = defaultIndicator
    ) {
        pages.forEachIndexed { index, title ->
            Tab(
                selected = pagerState.currentPage == index,
                onClick = {
                    animationScope.launch {
                        pagerState.scrollToPage(index)
                    }
                },
                text = { Text(text = title) }
            )
        }
    }

    HorizontalPager(
        modifier = Modifier.fillMaxSize(),
        count = pages.size,
        state = pagerState,
    ) { page ->
        when(page) {
            0 -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 62.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    GasControlScreen()
                }
            }
            1 -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(top = 62.dp, start = 24.dp, end = 24.dp, bottom = 12.dp)
                ) {
                    LightControlScreen()
                }
            }
        }
    }
}

@Composable
fun ConnectionFailedWindow(callback: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Nothing to do here */ },
        title = { Text("Connection failed") },
        text = { Text("Check the server") },
        confirmButton = {
            Button(onClick = callback) {
                Text("Close")
            }
        }
    )
}

@Preview(showBackground = true)
@Composable
fun MainScreenPreview() {
    LightsAppTheme {
        MainScreen()
    }
}