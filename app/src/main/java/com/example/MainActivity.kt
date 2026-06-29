package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.example.data.DebtPersonRepository
import com.example.data.DebtRepository
import com.example.ui.components.FloatingBottomNav
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.viewmodel.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.firstOrNull

class MainActivity : ComponentActivity() {
    @Volatile
    private var contentReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Hold splash until all screens pre-warmed + first frame settled
        splashScreen.setKeepOnScreenCondition { !contentReady }

        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                FinanceTrackerApp(onContentReady = { contentReady = true })
            }
        }
    }
}

@Composable
fun FinanceTrackerApp(onContentReady: () -> Unit = {}) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "overview"

    val context = LocalContext.current
    val application = context.applicationContext as FinanceApplication
    val database = application.database

    // Debt repositories
    val debtRepository = remember { DebtRepository(database.debtDao(), database.debtTransactionDao(), database.transactionDao()) }

    // Main finance VM
    val financeViewModel: FinanceViewModel = viewModel(
        factory = FinanceViewModelFactory(
            application = application,
            transactionDao = database.transactionDao(),
            moneySourceDao = database.moneySourceDao(),
            debtRepository = debtRepository
        )
    )
    val personRepository = remember { DebtPersonRepository(database.debtPersonDao()) }

    // Debt ViewModels
    val debtListViewModel: DebtListViewModel = viewModel(
        factory = DebtListViewModelFactory(debtRepository)
    )
    val personViewModel: DebtPersonViewModel = viewModel(
        factory = DebtPersonViewModelFactory(personRepository)
    )

    // Collect persons for debt form
    val persons by personViewModel.uiState.collectAsState()

    // Sub-screens (debt detail, form, people) hide bottom nav
    val isSubScreen = currentRoute.startsWith("debt/") && currentRoute != "debt/list"

    fun getRouteIndex(route: String?): Int {
        return when {
            route == "overview" -> 0
            route == "spend" -> 1
            route == "summary" -> 2
            route == "debt/list" -> 3
            else -> 0
        }
    }

    // ── Start-up pre-warming: run behind splash screen ──────────────────────
    // Navigate each tab screen so Compose pre-loads all classes, compiles
    // animation shaders, blurs, icons, and layout code *before* user sees it.
    // Splash holds until this completes.
    LaunchedEffect(Unit) {
        // Ensure Room DB fully initialized (bg pre-warm started in Application.onCreate)
        withContext(Dispatchers.IO) {
            application.database.openHelper.writableDatabase
        }

        // Pre-warm: visit every tab to force first-time composition + shader compilation
        val tabs = listOf("spend", "summary", "debt/list")
        for (route in tabs) {
            navController.navigate(route) {
                popUpTo("overview") { saveState = false }
                launchSingleTop = true
                restoreState = false
            }
            // Let one full frame pass — composition, layout, draw all happen
            withFrameNanos { }
        }

        // Return to start destination (overview) with fresh state
        navController.navigate("overview") {
            popUpTo(navController.graph.startDestinationId) { inclusive = true }
            launchSingleTop = true
        }
        withFrameNanos { } // let overview recompose + settle

        onContentReady()
    }

    Box(modifier = Modifier.fillMaxSize().background(AppBackground)) {
        // Áp dụng đúng code mẫu của bạn cho backdrop
        val backdropState = rememberLayerBackdrop {
            drawRect(AppBackground) // Dùng màu nền của app thay vì trắng tinh để đồng bộ
            drawContent()
        }

        // Bọc NavHost bằng layerBackdrop
        Box(
            modifier = Modifier
                .fillMaxSize()
                .layerBackdrop(backdropState)
        ) {
            Scaffold(
                containerColor = Color.Transparent,
                modifier = Modifier.fillMaxSize()
            ) { innerPadding ->
                NavHost(
                    navController = navController,
                    startDestination = "overview",
                    modifier = Modifier
                        .fillMaxSize() // Bỏ padding bottom ở đây để nội dung tràn xuống dưới nav bar
                    ,
                    // Dùng fade + scale nhẹ thay cho slide ngang.
                    // Lý do: slide container phải tính toán full-width offset và composite
                    // 2 layer cạnh nhau trong lúc NavController còn đang restore lại state
                    // (do saveState/restoreState ở bottom nav) -> dễ rớt frame ở lần bấm đầu.
                    // Fade+scale rẻ hơn nhiều về compositing và không phụ thuộc hướng index,
                    // nên animation luôn ăn ngay từ lần bấm đầu tiên.
                    enterTransition = {
                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                    },
                    popEnterTransition = {
                        fadeIn(animationSpec = tween(220, easing = FastOutSlowInEasing)) +
                                scaleIn(
                                    initialScale = 0.96f,
                                    animationSpec = tween(220, easing = FastOutSlowInEasing)
                                )
                    },
                    popExitTransition = {
                        fadeOut(animationSpec = tween(150, easing = FastOutSlowInEasing))
                    }
                ) {
                    composable("overview") { OverviewScreen(financeViewModel) }
                    composable("spend") {
                        SpendScreen(
                            financeViewModel,
                            navController = navController
                        )
                    }
                    composable("summary") { SummaryScreen(financeViewModel) }

                    composable("debt/list") {
                        DebtListScreen(
                            viewModel = debtListViewModel,
                            onNavigateToDetail = { debtId -> navController.navigate("debt/detail/$debtId") },
                            onNavigateToAdd = { navController.navigate("debt/add") },
                            onNavigateToPeople = { navController.navigate("debt/people") }
                        )
                    }

                    composable(
                        route = "debt/detail/{debtId}",
                        arguments = listOf(navArgument("debtId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val debtId =
                            backStackEntry.arguments?.getLong("debtId") ?: return@composable
                        val detailViewModel: DebtDetailViewModel = viewModel(
                            factory = DebtDetailViewModelFactory(debtId, debtRepository)
                        )
                        DebtDetailScreen(
                            viewModel = detailViewModel,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToEdit = { id -> navController.navigate("debt/edit/$id") }
                        )
                    }

                    composable("debt/add") {
                        DebtAddScreen(
                            persons = persons.persons,
                            repository = debtRepository,
                            personRepository = personRepository,
                            onSaved = { navController.popBackStack() },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "debt/edit/{debtId}",
                        arguments = listOf(navArgument("debtId") { type = NavType.LongType })
                    ) { backStackEntry ->
                        val debtId =
                            backStackEntry.arguments?.getLong("debtId") ?: return@composable
                        DebtEditScreen(
                            debtId = debtId,
                            persons = persons.persons,
                            repository = debtRepository,
                            personRepository = personRepository,
                            onSaved = { navController.popBackStack() },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }

                    composable("debt/people") {
                        DebtPersonScreen(
                            viewModel = personViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }

        // Đặt Thanh Bottom Nav theo đúng cấu trúc của bạn
        if (!isSubScreen) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    // Dùng safeContentPadding giống hệt code mẫu của bạn để handle gesture line
                    .navigationBarsPadding()
                    .padding(bottom = 8.dp)
            ) {
                val routeIndex = getRouteIndex(currentRoute)
                FloatingBottomNav(
                    currentRouteIndex = routeIndex,
                    backdropState = backdropState,
                    onNavigate = { index ->
                        val targetRoute = when (index) {
                            0 -> "overview"
                            1 -> "spend"
                            2 -> "summary"
                            3 -> "debt/list"
                            else -> "overview"
                        }
                        // Quan trọng: so sánh với navController.currentDestination?.route
                        // (giá trị tức thời) thay vì biến `currentRoute` lấy từ
                        // currentBackStackEntryAsState(), vì biến đó chỉ cập nhật sau khi
                        // recomposition diễn ra. Nếu user bấm đúng lúc recomposition chưa
                        // kịp chạy, `currentRoute` cũ vẫn còn -> navigate() bị gọi lại lần 2
                        // một cách thừa, làm animation bị "nuốt" mất ở lần bấm đầu tiên.
                        val liveRoute = navController.currentDestination?.route
                        if (liveRoute != targetRoute) {
                            navController.navigate(targetRoute) {
                                popUpTo(navController.graph.startDestinationId) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    }
}

// Wrapper screens for add/edit with local state

@Composable
fun DebtAddScreen(
    persons: List<com.example.data.DebtPerson>,
    repository: DebtRepository,
    personRepository: DebtPersonRepository,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    DebtFormScreen(
        personId = persons.firstOrNull()?.id,
        persons = persons,
        isEdit = false,
        initialTitle = "",
        initialNote = "",
        initialAmount = "",
        initialDueDate = null,
        initialDirection = com.example.data.DebtDirection.OWED_TO_ME,
        isLoading = isLoading,
        error = error,
        onSave = { personId, title, note, amount, dueDate, direction ->
            isLoading = true
            coroutineScope.launch {
                val result = repository.createDebt(
                    personId = personId,
                    title = title,
                    note = note,
                    originalAmount = amount,
                    createdDate = System.currentTimeMillis(),
                    dueDate = dueDate,
                    direction = direction
                )
                isLoading = false
                if (result.isSuccess) {
                    onSaved()
                } else {
                    error = result.exceptionOrNull()?.message ?: "Failed to create debt"
                }
            }
        },
        onNavigateBack = onNavigateBack,
        onAddPerson = { name ->
            coroutineScope.launch {
                personRepository.addPerson(name)
            }
        }
    )
}

@Composable
fun DebtEditScreen(
    debtId: Long,
    persons: List<com.example.data.DebtPerson>,
    repository: DebtRepository,
    personRepository: DebtPersonRepository,
    onSaved: () -> Unit,
    onNavigateBack: () -> Unit
) {
    var debt by remember { mutableStateOf<com.example.data.Debt?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSaving by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(debtId) {
        isLoading = true
        val result = repository.observeDebtWithPerson(debtId)
            .firstOrNull()
        result?.let {
            debt = it.debt
        }
        isLoading = false
    }

    val d = debt
    if (isLoading || d == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = TextPrimary)
        }
        return
    }

    DebtFormScreen(
        personId = d.personId,
        persons = persons,
        isEdit = true,
        initialTitle = d.title,
        initialNote = d.note,
        initialAmount = d.originalAmount.toString(),
        initialDueDate = d.dueDate,
        initialDirection = d.direction,
        isLoading = isSaving,
        error = error,
        onSave = { personId, title, note, _, dueDate, direction ->
            isSaving = true
            coroutineScope.launch {
                val result = repository.updateDebt(
                    debtId = debtId,
                    personId = personId,
                    title = title,
                    note = note,
                    dueDate = dueDate,
                    direction = direction
                )
                isSaving = false
                if (result.isSuccess) {
                    onSaved()
                } else {
                    error = result.exceptionOrNull()?.message ?: "Failed to update debt"
                }
            }
        },
        onNavigateBack = onNavigateBack,
        onAddPerson = { name ->
            coroutineScope.launch {
                personRepository.addPerson(name)
            }
        }
    )
}