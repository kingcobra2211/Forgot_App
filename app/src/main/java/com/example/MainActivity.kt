package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.home.HomeScreen
import com.example.ui.profile.SettingsScreen
import com.example.ui.remember.RememberScreen
import com.example.ui.reminders.RemindersScreen
import com.example.ui.search.SearchScreen
import com.example.ui.theme.ForgotTheme
import com.example.ui.utils.CategoryRegistry
import com.example.ui.utils.LanguageUtils
import com.example.ui.viewmodel.MemoryViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MemoryViewModel = viewModel()
            val themeKey by viewModel.themeKey.collectAsState()
            val language by viewModel.language.collectAsState()

            ForgotTheme(themeKey = themeKey) {
                MainAppCoordinator(viewModel = viewModel, language = language)
            }
        }
    }
}

@Composable
fun MainAppCoordinator(
    viewModel: MemoryViewModel,
    language: String
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    var showQuickAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            // Render Bottom Navigation only for primary screens
            val isPrimaryScreen = currentRoute in listOf("home", "search", "reminders", "settings")
            if (isPrimaryScreen) {
                NavigationBar(
                    modifier = Modifier.testTag("app_bottom_navigation")
                ) {
                    // 1. Home
                    NavigationBarItem(
                        selected = currentRoute == "home",
                        onClick = { navController.navigate("home") { popUpTo("home") { inclusive = false } } },
                        icon = { Icon(imageVector = Icons.Default.Home, contentDescription = "Home") },
                        label = { Text(LanguageUtils.getString("home_tab", language)) }
                    )

                    // 2. Search
                    NavigationBarItem(
                        selected = currentRoute == "search",
                        onClick = { navController.navigate("search") { popUpTo("home") } },
                        icon = { Icon(imageVector = Icons.Default.Search, contentDescription = "Search") },
                        label = { Text(LanguageUtils.getString("search_tab", language)) }
                    )

                    // 3. Quick Add (Center button acting as popup trigger)
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
                                    tint = Color.White
                                )
                            }
                        },
                        label = { Text(LanguageUtils.getString("quick_add", language)) },
                        modifier = Modifier.testTag("bottom_quick_add_tab")
                    )

                    // 4. Reminders
                    NavigationBarItem(
                        selected = currentRoute == "reminders",
                        onClick = { navController.navigate("reminders") { popUpTo("home") } },
                        icon = { Icon(imageVector = Icons.Default.Notifications, contentDescription = "Reminders") },
                        label = { Text(LanguageUtils.getString("reminders_tab", language)) }
                    )

                    // 5. Settings
                    NavigationBarItem(
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") { popUpTo("home") } },
                        icon = { Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings") },
                        label = { Text(LanguageUtils.getString("settings_tab", language)) }
                    )
                }
            }
        },
        floatingActionButton = {
            // Render FAB only on Home screen for quick 5s capture flow
            if (currentRoute == "home") {
                FloatingActionButton(
                    onClick = { showQuickAddDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier
                        .padding(bottom = 16.dp)
                        .testTag("home_quick_add_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Quick Add FAB")
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
            // Home Screen route
            composable("home") {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "remember?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    },
                    onNavigateToSearch = { navController.navigate("search") },
                    onNavigateToReminders = { navController.navigate("reminders") }
                )
            }

            // Search Screen route
            composable("search") {
                SearchScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "remember?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    }
                )
            }

            // Reminders Screen route
            composable("reminders") {
                RemindersScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "remember?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    }
                )
            }

            // Settings/Profile Screen route
            composable("settings") {
                SettingsScreen(
                    viewModel = viewModel,
                    onNavigateToRemember = { id, category ->
                        val route = if (id != null) "remember?memoryId=$id" else "remember?category=$category"
                        navController.navigate(route)
                    }
                )
            }

            // Remember Screen (Add/Edit Form) route
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

    // GORGEOUS BILINGUAL QUICK ADD MODAL
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
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = LanguageUtils.getString("quick_add", language),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    Text(
                        text = "Choose memory type to save in 5 seconds:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                    ) {
                        items(CategoryRegistry.categories) { catItem ->
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
                                    containerColor = catItem.color.copy(alpha = 0.12f)
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(catItem.color.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = catItem.icon,
                                            contentDescription = catItem.name,
                                            tint = catItem.color,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = LanguageUtils.getString(catItem.name, language),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
