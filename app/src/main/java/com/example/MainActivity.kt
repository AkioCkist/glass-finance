package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.ui.components.FloatingBottomNav
import com.example.ui.screens.SummaryScreen
import com.example.ui.screens.OverviewScreen
import com.example.ui.screens.SpendScreen
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import com.example.viewmodel.FinanceViewModelFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FinanceTrackerApp()
            }
        }
    }
}

@Composable
fun FinanceTrackerApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "overview"

    val context = LocalContext.current
    val application = context.applicationContext as FinanceApplication
    val database = application.database
    val viewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModelFactory(database.transactionDao())
    )

    fun getRouteIndex(route: String?): Int {
        return when (route) {
            "overview" -> 0
            "spend" -> 1
            "summary" -> 2
            else -> 0
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {

        Scaffold(
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize(),
            bottomBar = {
                // Floating Bottom Nav inside a Box to center it securely
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        // lowered padding from 32.dp to 12.dp
                        .padding(bottom = 12.dp)
                        .navigationBarsPadding(),
                    contentAlignment = Alignment.Center
                ) {
                    FloatingBottomNav(
                        currentRoute = currentRoute,
                        onNavigate = { route ->
                            navController.navigate(route) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "overview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                enterTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    if (targetIndex > initialIndex) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
                    }
                },
                exitTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    if (targetIndex > initialIndex) {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
                    }
                },
                popEnterTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    if (targetIndex > initialIndex) {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                    } else {
                        slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
                    }
                },
                popExitTransition = {
                    val initialIndex = getRouteIndex(initialState.destination.route)
                    val targetIndex = getRouteIndex(targetState.destination.route)
                    if (targetIndex > initialIndex) {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300))
                    } else {
                        slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300))
                    }
                }
            ) {
                composable("overview") { OverviewScreen(viewModel) }
                composable("spend") { SpendScreen(viewModel) }
                composable("summary") { SummaryScreen(viewModel) }
            }
        }
    }
}
