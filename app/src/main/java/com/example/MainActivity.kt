package com.example

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Build
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.detail.MemoryDetailScreen
import com.example.ui.home.HomeScreen
import com.example.ui.profile.SettingsScreen
import com.example.ui.remember.RememberScreen
import com.example.ui.reminders.RemindersScreen
import com.example.ui.search.SearchScreen
import com.example.ui.theme.ForgotTheme
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.utils.LocalResponsiveMetrics
import com.example.ui.utils.ProvideResponsiveMetrics
import com.example.ui.viewmodel.MemoryViewModel

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MemoryViewModel = viewModel()
            val updateViewModel: com.example.ui.viewmodel.UpdateViewModel = viewModel()
            val themeKey by viewModel.themeKey.collectAsState()
            val language by viewModel.language.collectAsState()
            val windowSizeClass = calculateWindowSizeClass(this)

            ForgotTheme(themeKey = themeKey) {
                ProvideResponsiveMetrics(widthSizeClass = windowSizeClass.widthSizeClass) {
                    MainAppCoordinator(
                        viewModel = viewModel,
                        updateViewModel = updateViewModel,
                        language = language
                    )
                }
            }
        }
    }
}

@Composable
fun MainAppCoordinator(
    viewModel: MemoryViewModel,
    updateViewModel: com.example.ui.viewmodel.UpdateViewModel,
    language: String
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val metrics = LocalResponsiveMetrics.current
    val isCompact = metrics.widthSizeClass == androidx.compose.material3.windowsizeclass.WindowWidthSizeClass.Compact

    var showQuickAddDialog by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    var isSplashActive by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1800)
        isSplashActive = false
    }

    LaunchedEffect(Unit) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }

        val activity = context as? ComponentActivity
        val targetMemoryId = activity?.intent?.getIntExtra(com.example.data.repository.ReminderReceiver.EXTRA_MEMORY_ID, 0) ?: 0
        if (targetMemoryId != 0) {
            activity?.intent?.removeExtra(com.example.data.repository.ReminderReceiver.EXTRA_MEMORY_ID)
            navController.navigate("remember?memoryId=$targetMemoryId")
        }
    }
    val isUpdateAvailable by updateViewModel.isUpdateAvailable.collectAsState()

    // ... (rest of the setup logic remains same)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.performExportBackup(uri)
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            viewModel.performImportBackup(uri)
        }
    }

    val activeMemories by viewModel.activeMemories.collectAsState()
    val categoryUsageCounts: Map<String, Int> = remember(activeMemories) {
        activeMemories.groupBy { it.memory.category }.mapValues { it.value.size }
    }
    val sortedCategories = remember(categoryUsageCounts) {
        CategoryRegistry.categories.sortedByDescending { categoryUsageCounts[it.name] ?: 0 }
    }

    if (isSplashActive) {
        SplashScreen()
    } else {
        Row(modifier = Modifier.fillMaxSize()) {
        // 1. Adaptive Navigation Rail for non-compact screens
        if (!isCompact && currentRoute in listOf("home", "search", "reminders", "settings")) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
                header = {
                    FloatingActionButton(
                        onClick = { showQuickAddDialog = true },
                        elevation = FloatingActionButtonDefaults.bottomAppBarFabElevation()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add")
                    }
                }
            ) {
                Spacer(Modifier.weight(1f))
                NavigationRailItem(
                    selected = currentRoute == "home",
                    onClick = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text(LanguageUtils.getString("home_tab", language)) }
                )
                NavigationRailItem(
                    selected = currentRoute == "search",
                    onClick = { navController.navigate("search") { popUpTo("home") } },
                    icon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    label = { Text(LanguageUtils.getString("search_tab", language)) }
                )
                NavigationRailItem(
                    selected = currentRoute == "reminders",
                    onClick = { navController.navigate("reminders") { popUpTo("home") } },
                    icon = { Icon(Icons.Default.Notifications, contentDescription = "Reminders") },
                    label = { Text(LanguageUtils.getString("reminders_tab", language)) }
                )
                NavigationRailItem(
                    selected = currentRoute == "settings",
                    onClick = { navController.navigate("settings") { popUpTo("home") } },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text(LanguageUtils.getString("settings_tab", language)) }
                )
                Spacer(Modifier.weight(1f))
            }
        }

        Scaffold(
            modifier = Modifier.weight(1f),
            bottomBar = {
                val isPrimaryScreen = currentRoute in listOf("home", "search", "reminders", "settings")
                if (isPrimaryScreen && isCompact) {
                    NavigationBar(
                        modifier = Modifier.testTag("app_bottom_navigation"),
                        tonalElevation = 4.dp,
                        containerColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.onSurface,
                        windowInsets = NavigationBarDefaults.windowInsets
                    ) {
                        // Home
                        NavigationBarItem(
                            selected = currentRoute == "home",
                            onClick = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                            icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                            label = {
                                Text(
                                    text = LanguageUtils.getString("home_tab", language),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = metrics.labelFontSize,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    textAlign = TextAlign.Center
                                )
                            },
                            alwaysShowLabel = true
                        )

                        // Search
                        NavigationBarItem(
                            selected = currentRoute == "search",
                            onClick = { navController.navigate("search") { popUpTo("home") } },
                            icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                            label = {
                                Text(
                                    text = LanguageUtils.getString("search_tab", language),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = metrics.labelFontSize,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    textAlign = TextAlign.Center
                                )
                            },
                            alwaysShowLabel = true
                        )

                        // Quick Add (Standardized Slot)
                        NavigationBarItem(
                            selected = false,
                            onClick = { showQuickAddDialog = true },
                            icon = {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Quick Add",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            },
                            label = {
                                Text(
                                    text = LanguageUtils.getString("quick_add", language),
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = metrics.labelFontSize,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    textAlign = TextAlign.Center
                                )
                            },
                            alwaysShowLabel = true
                        )

                        // Reminders
                        NavigationBarItem(
                            selected = currentRoute == "reminders",
                            onClick = { navController.navigate("reminders") { popUpTo("home") } },
                            icon = { Icon(imageVector = Icons.Default.Notifications, contentDescription = "Reminders") },
                            label = {
                                Text(
                                    text = LanguageUtils.getString("reminders_tab", language),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = metrics.labelFontSize,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    textAlign = TextAlign.Center
                                )
                            },
                            alwaysShowLabel = true
                        )

                        // Settings
                        NavigationBarItem(
                            selected = currentRoute == "settings",
                            onClick = { navController.navigate("settings") { popUpTo("home") } },
                            icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                            label = {
                                Text(
                                    text = LanguageUtils.getString("settings_tab", language),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = metrics.labelFontSize,
                                    letterSpacing = (-0.3).sp,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Visible,
                                    textAlign = TextAlign.Center
                                )
                            },
                            alwaysShowLabel = true
                        )
                    }
                }
            }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = "home",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "detail?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    },
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToReminders = { navController.navigate("reminders") }
                )
            }

            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "detail?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    }
                )
            }

            composable("reminders") {
                RemindersScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "detail?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    }
                )
            }

            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    updateViewModel = updateViewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "detail?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    },
                    onExportBackup = {
                        createDocumentLauncher.launch("forgot_backup_${System.currentTimeMillis()}.json")
                    },
                    onImportBackup = {
                        openDocumentLauncher.launch(arrayOf("application/json", "*/*"))
                    }
                )
            }

            composable(
                route = "detail?memoryId={memoryId}",
                arguments = listOf(
                    navArgument("memoryId") {
                        type = NavType.IntType
                        defaultValue = 0
                    }
                )
            ) { backStackEntry ->
                val memoryId = backStackEntry.arguments?.getInt("memoryId") ?: 0
                MemoryDetailScreen(
                    memoryId = memoryId,
                    viewModel = viewModel,
                    onNavigateBack = { navController.popBackStack() },
                    onEditMemory = { id -> navController.navigate("remember?memoryId=$id") }
                )
            }

            composable(
                route = "remember?memoryId={memoryId}&category={category}",
                arguments = listOf(
                    navArgument("memoryId") {
                        type = NavType.IntType
                        defaultValue = 0
                    },
                    navArgument("category") {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val memoryId = backStackEntry.arguments?.getInt("memoryId")?.takeIf { it != 0 }
                val category = backStackEntry.arguments?.getString("category")

                RememberScreen(
                    viewModel = viewModel,
                    memoryId = memoryId,
                    initialCategory = category,
                    onSaveComplete = {
                        navController.popBackStack()
                    },
                    onCancel = {
                        navController.popBackStack()
                    }
                )
            }
        }
    }

    // REDESIGNED BILINGUAL QUICK ADD MODAL
    if (showQuickAddDialog) {
        Dialog(onDismissRequest = { showQuickAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
                    .testTag("quick_add_dialog"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = LanguageUtils.getString("quick_add", language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Choose memory type to save in 5 seconds:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(290.dp)
                    ) {
                        items(sortedCategories) { catItem ->
                            val count = categoryUsageCounts[catItem.name] ?: 0
                            Card(
                                onClick = {
                                    showQuickAddDialog = false
                                    navController.navigate("remember?category=${catItem.name}")
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(1f)
                                    .testTag("quick_add_category_${catItem.name.lowercase()}"),
                                colors = CardDefaults.cardColors(
                                    containerColor = catItem.color.copy(alpha = 0.1f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize()) {
                                    // Circular Count Badge on top-right for premium stat touch
                                    if (count > 0) {
                                        Box(
                                            modifier = Modifier
                                                .padding(6.dp)
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(catItem.color)
                                                .align(Alignment.TopEnd),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "$count",
                                                color = Color.White,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Black
                                            )
                                        }
                                    }

                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(4.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(catItem.color.copy(alpha = 0.2f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = catItem.icon,
                                                contentDescription = catItem.name,
                                                tint = catItem.color,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = LanguageUtils.getString(catItem.name, language),
                                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip,
                                            softWrap = false,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                    }
                                }
                            }
                        }
                    }

                    TextButton(
                        onClick = { showQuickAddDialog = false },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = LanguageUtils.getString("cancel_button", language),
                            fontWeight = FontWeight.Black,
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
        }
    }
        }
    }

    if (isUpdateAvailable) {
        com.example.ui.components.UpdateDialog(
            viewModel = updateViewModel,
            onDismiss = { updateViewModel.dismissUpdateDialog() }
        )
    }
}

@Composable
fun SplashScreen() {
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
    )
    val scaleAnim by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0.82f,
        animationSpec = androidx.compose.animation.core.tween(durationMillis = 800)
    )

    LaunchedEffect(Unit) {
        startAnimation = true
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.graphicsLayer(
                alpha = alphaAnim,
                scaleX = scaleAnim,
                scaleY = scaleAnim
            )
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_app_logo),
                contentDescription = "Forgot App Logo",
                modifier = Modifier
                    .size(130.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "Forgot",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Save it once. Forget nothing.",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f)
            )
        }
    }
}
