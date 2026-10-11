package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.ExpenseViewModel
import com.example.ui.screens.AnalyzerScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ExportScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.MyApplicationTheme

enum class AppScreen(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    DASHBOARD("Trends", Icons.Default.PieChart, Icons.Outlined.PieChart),
    ANALYZER("SMS Parser", Icons.Default.Sms, Icons.Outlined.Sms),
    TRANSACTIONS("Expenses", Icons.Default.ReceiptLong, Icons.Outlined.ReceiptLong),
    EXPORT("Sheets Export", Icons.Default.TableChart, Icons.Outlined.TableChart)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainApp(viewModel: ExpenseViewModel = viewModel()) {
    var currentScreen by remember { mutableStateOf(AppScreen.DASHBOARD) }
    var showMenu by remember { mutableStateOf(false) }

    BackHandler(enabled = currentScreen != AppScreen.DASHBOARD) {
        currentScreen = AppScreen.DASHBOARD
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row {
                        Text(
                            text = "SpendWise",
                            fontWeight = FontWeight.ExtraBold,
                            color = EmeraldPrimary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "• ${currentScreen.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Manage Categories") },
                            onClick = {
                                viewModel.showCategoryManagerDialog.value = true
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Tune, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reset Sample Data") },
                            onClick = {
                                viewModel.resetDataWithSamples()
                                showMenu = false
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            NavigationBar(
                modifier = Modifier.testTag("bottom_nav_bar"),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 4.dp
            ) {
                AppScreen.entries.forEach { screen ->
                    val isSelected = currentScreen == screen
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { currentScreen = screen },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = screen.title
                            )
                        },
                        label = { Text(screen.title) },
                        modifier = Modifier.testTag("nav_item_${screen.name.lowercase()}")
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (currentScreen) {
                AppScreen.DASHBOARD -> DashboardScreen(
                    viewModel = viewModel,
                    onNavigateToExport = { currentScreen = AppScreen.EXPORT }
                )
                AppScreen.ANALYZER -> AnalyzerScreen(
                    viewModel = viewModel,
                    onNavigateToDashboard = { currentScreen = AppScreen.DASHBOARD }
                )
                AppScreen.TRANSACTIONS -> TransactionsScreen(
                    viewModel = viewModel,
                    onNavigateToAnalyzer = { currentScreen = AppScreen.ANALYZER }
                )
                AppScreen.EXPORT -> ExportScreen(
                    viewModel = viewModel
                )
            }
        }
    }

    val showCategoryDialog by viewModel.showCategoryManagerDialog.collectAsState()
    val categories by viewModel.categories.collectAsState()

    if (showCategoryDialog) {
        com.example.ui.components.CategoryManagerDialog(
            categories = categories,
            onAddCategory = { name, colorHex, iconName, keywords ->
                viewModel.addCustomCategory(name, colorHex, iconName, keywords)
            },
            onEditCategory = { viewModel.editCustomCategory(it) },
            onDeleteCategory = { viewModel.deleteCustomCategory(it) },
            onDismiss = { viewModel.showCategoryManagerDialog.value = false }
        )
    }
}
