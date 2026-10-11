package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.export.GoogleSheetsExporter
import com.example.ui.ExpenseViewModel
import com.example.ui.components.DateRangeSelector
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ExportScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedFilter by viewModel.selectedFilter.collectAsState()
    val customDateRange by viewModel.customDateRange.collectAsState()
    val filteredExpenses by viewModel.filteredExpenses.collectAsState()
    val summaryReport by viewModel.summaryReport.collectAsState()

    var showCopiedNotice by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) } // 0: Export Hub, 1: Live Spreadsheet Preview, 2: Summary Report

    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            if (showCopiedNotice) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { showCopiedNotice = false }) {
                            Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text("Copied formatted table to clipboard! Paste directly into Google Sheets.")
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Date Filter Bar
            DateRangeSelector(
                selectedFilter = selectedFilter,
                customRange = customDateRange,
                onFilterSelected = { viewModel.setDateFilter(it) },
                onCustomRangeSelected = { start, end -> viewModel.setCustomDateRange(start, end) }
            )

            // Sub tabs
            TabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth()
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Export Actions") },
                    icon = { Icon(Icons.Outlined.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Sheet Preview") },
                    icon = { Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Summary Text") },
                    icon = { Icon(Icons.Default.Summarize, contentDescription = null, modifier = Modifier.size(18.dp)) }
                )
            }

            when (selectedTab) {
                0 -> {
                    // Export Actions
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Status
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldPrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Report Ready for Google Sheets",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "${filteredExpenses.size} transactions selected in filter (${selectedFilter.displayName}) with total $${String.format(Locale.US, "%.2f", summaryReport.totalSpent)}.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        // Primary Action: Open / Export to Google Sheets
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Share,
                                            contentDescription = null,
                                            tint = EmeraldPrimary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Export Google Sheets File (.csv)",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Creates structured sheet file with summary & details",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        val intent = GoogleSheetsExporter.exportAndShareGoogleSheets(
                                            context = context,
                                            expenses = filteredExpenses,
                                            dateRangeLabel = selectedFilter.displayName
                                        )
                                        context.startActivity(intent)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("export_sheets_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = filteredExpenses.isNotEmpty()
                                ) {
                                    Icon(Icons.Outlined.FileDownload, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open in Google Sheets / Share", fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Secondary Action: 1-Tap Copy for Google Sheets
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ContentCopy,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "1-Tap Copy for Google Sheets",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = "Paste directly into cells A1-J1 on web or app",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                OutlinedButton(
                                    onClick = {
                                        val tsv = GoogleSheetsExporter.generateTsvForClipboard(filteredExpenses)
                                        GoogleSheetsExporter.copyToClipboard(context, tsv)
                                        showCopiedNotice = true
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                        .testTag("copy_tsv_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    enabled = filteredExpenses.isNotEmpty()
                                ) {
                                    Icon(Icons.Outlined.ContentCopy, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Copy Formatted Table", fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }

                        // Instructions Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "How to Import into Google Sheets:",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "1. Tap 'Open in Google Sheets / Share' above and choose Google Sheets or Google Drive.\n" +
                                            "2. Or tap 'Copy Formatted Table', open sheets.google.com, and press Ctrl+V (or paste on phone).\n" +
                                            "3. The exported report includes both an Executive Summary Block and column-wise transaction rows with original SMS verification.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // Live Spreadsheet Table Preview
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                    ) {
                        Text(
                            text = "Google Sheets Table View (${filteredExpenses.size} Rows)",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )

                        // Scrollable table
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .horizontalScroll(rememberScrollState())
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxHeight(),
                                verticalArrangement = Arrangement.spacedBy(1.dp)
                            ) {
                                // Header row
                                item {
                                    Row(
                                        modifier = Modifier
                                            .background(MaterialTheme.colorScheme.primaryContainer)
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    ) {
                                        TableCell("Date", 100.dp, isHeader = true)
                                        TableCell("Merchant", 140.dp, isHeader = true)
                                        TableCell("Category", 120.dp, isHeader = true)
                                        TableCell("Type", 90.dp, isHeader = true)
                                        TableCell("Amount", 90.dp, isHeader = true)
                                        TableCell("Account", 110.dp, isHeader = true)
                                        TableCell("Reference", 100.dp, isHeader = true)
                                    }
                                }

                                items(filteredExpenses) { item ->
                                    val bg = if (filteredExpenses.indexOf(item) % 2 == 0) {
                                        MaterialTheme.colorScheme.surface
                                    } else {
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                    }
                                    Row(
                                        modifier = Modifier
                                            .background(bg)
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        TableCell(dateFormat.format(Date(item.timestamp)), 100.dp)
                                        TableCell(item.merchant, 140.dp)
                                        TableCell(item.category, 120.dp)
                                        TableCell(item.type, 90.dp)
                                        TableCell("$${String.format(Locale.US, "%.2f", item.amount)}", 90.dp, isBold = true)
                                        TableCell(item.accountInfo.ifEmpty { "-" }, 110.dp)
                                        TableCell(item.referenceId.ifEmpty { "-" }, 100.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // Executive Summary Text Report
                    val summaryText = remember(filteredExpenses, summaryReport) {
                        GoogleSheetsExporter.generateCsvContent(filteredExpenses, selectedFilter.displayName)
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Raw CSV & Summary Report",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            IconButton(onClick = {
                                GoogleSheetsExporter.copyToClipboard(context, summaryText, "CSV Report")
                                showCopiedNotice = true
                            }) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy text")
                            }
                        }

                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = summaryText,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .verticalScroll(rememberScrollState()),
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    isHeader: Boolean = false,
    isBold: Boolean = false
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        style = if (isHeader) MaterialTheme.typography.labelMedium else MaterialTheme.typography.bodySmall,
        fontWeight = if (isHeader || isBold) FontWeight.Bold else FontWeight.Normal,
        maxLines = 1,
        color = if (isHeader) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
    )
}
