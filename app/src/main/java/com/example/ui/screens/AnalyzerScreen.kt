package com.example.ui.screens

import android.Manifest
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ContentPaste
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Sms
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.parser.ParsedSmsResult
import com.example.parser.SampleSmsData
import com.example.ui.ExpenseViewModel
import com.example.ui.components.CategoryManagerDialog
import com.example.ui.components.getIconForName
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldPrimary
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnalyzerScreen(
    viewModel: ExpenseViewModel,
    onNavigateToDashboard: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val smsText by viewModel.smsInputText.collectAsState()
    val parsedResult by viewModel.lastParsedResult.collectAsState()
    val isScanning by viewModel.isScanningInbox.collectAsState()
    val scanNotice by viewModel.inboxScanResultNotice.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var customNotes by remember { mutableStateOf("") }
    var selectedCategoryOverrideName by remember { mutableStateOf<String?>(null) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }
    var showCategoryManager by remember { mutableStateOf(false) }

    // Runtime Permission for Reading Device SMS
    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            viewModel.scanInboxSms(context)
        } else {
            viewModel.inboxScanResultNotice.value = "SMS permission denied. You can still paste or use sample messages!"
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            snackbarMessage?.let { msg ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = { snackbarMessage = null }) {
                            Text("OK", color = MaterialTheme.colorScheme.inversePrimary)
                        }
                    }
                ) {
                    Text(msg)
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with Keystore Security badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Sms,
                            contentDescription = "SMS",
                            tint = EmeraldPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "SMS Expense Analyzer",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Auto-extracts & filters duplicates securely",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Manage Categories Button
                IconButton(onClick = { showCategoryManager = true }) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Manage categories",
                        tint = EmeraldPrimary
                    )
                }
            }

            // Security assurance pill
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Protected by Android Keystore hardware-backed AES-256 encryption",
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Quick Samples Bar
            Text(
                text = "Tap a sample bank SMS to test:",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SampleSmsData.samples.forEach { sample ->
                    SuggestionChip(
                        onClick = {
                            viewModel.setSampleSms(sample.message)
                            selectedCategoryOverrideName = null
                        },
                        label = { Text(sample.title) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.FlashOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = EmeraldPrimary
                            )
                        }
                    )
                }
            }

            // Input Card
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Paste or Type Text Message",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row {
                            IconButton(
                                onClick = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    val clip = clipboard.primaryClip
                                    if (clip != null && clip.itemCount > 0) {
                                        val text = clip.getItemAt(0).text?.toString() ?: ""
                                        viewModel.smsInputText.value = text
                                        viewModel.analyzeCurrentSms()
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentPaste,
                                    contentDescription = "Paste from clipboard",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (smsText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        viewModel.smsInputText.value = ""
                                        viewModel.lastParsedResult.value = null
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear text",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = smsText,
                        onValueChange = {
                            viewModel.smsInputText.value = it
                            if (it.length > 5) {
                                viewModel.analyzeCurrentSms()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp)
                            .testTag("sms_input_field"),
                        placeholder = {
                            Text("e.g., Your A/c ending 1234 is debited for $45.20 on 10-Oct by Starbucks...")
                        },
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { viewModel.analyzeCurrentSms() },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("analyze_sms_button"),
                            enabled = smsText.isNotBlank(),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Analyze Text")
                        }

                        OutlinedButton(
                            onClick = {
                                smsPermissionLauncher.launch(Manifest.permission.READ_SMS)
                            },
                            shape = RoundedCornerShape(10.dp),
                            enabled = !isScanning,
                            modifier = Modifier.testTag("scan_inbox_button")
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Inbox,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Scan Inbox")
                        }
                    }

                    scanNotice?.let { notice ->
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = notice,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Parsed Result Display
            parsedResult?.let { result ->
                AnalysisResultCard(
                    result = result,
                    categories = categories,
                    customNotes = customNotes,
                    onNotesChange = { customNotes = it },
                    selectedCategoryName = selectedCategoryOverrideName ?: result.categoryName,
                    onCategoryChange = { selectedCategoryOverrideName = it },
                    onManageCategories = { showCategoryManager = true },
                    onSave = {
                        val finalCategory = selectedCategoryOverrideName ?: result.categoryName
                        val finalResult = result.copy(categoryName = finalCategory)

                        viewModel.saveParsedResult(finalResult, customNotes)
                        snackbarMessage = "Encrypted expense saved! $${finalResult.amount} at ${finalResult.merchant}"
                        customNotes = ""
                        selectedCategoryOverrideName = null
                    }
                )
            }
        }
    }

    if (showCategoryManager) {
        CategoryManagerDialog(
            categories = categories,
            onAddCategory = { name, colorHex, iconName, keywords ->
                viewModel.addCustomCategory(name, colorHex, iconName, keywords)
            },
            onEditCategory = { viewModel.editCustomCategory(it) },
            onDeleteCategory = { viewModel.deleteCustomCategory(it) },
            onDismiss = { showCategoryManager = false }
        )
    }
}

@Composable
fun AnalysisResultCard(
    result: ParsedSmsResult,
    categories: List<CategoryEntity>,
    customNotes: String,
    onNotesChange: (String) -> Unit,
    selectedCategoryName: String,
    onCategoryChange: (String) -> Unit,
    onManageCategories: () -> Unit,
    onSave: () -> Unit
) {
    val activeCategory = categories.firstOrNull { it.name.equals(selectedCategoryName, ignoreCase = true) }
    val categoryColor = activeCategory?.composeColor ?: EmeraldPrimary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("analysis_result_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (result.isTransaction) {
                if (result.isDuplicate) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
            } else {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
            }
        ),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (result.isTransaction) {
                            if (result.isDuplicate) Icons.Default.Warning else Icons.Default.CheckCircle
                        } else Icons.Default.Warning,
                        contentDescription = null,
                        tint = if (result.isTransaction && !result.isDuplicate) EmeraldPrimary else AmberWarning
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (result.isTransaction) {
                            if (result.isDuplicate) "Duplicate SMS Alert" else "Valid Transaction Detected"
                        } else "Not a Transaction",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (result.isTransaction && !result.isDuplicate) EmeraldPrimary else if (result.isDuplicate) AmberWarning else CoralRed
                    )
                }

                if (result.isTransaction) {
                    Surface(
                        color = (if (result.isDuplicate) AmberWarning else EmeraldPrimary).copy(alpha = 0.15f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (result.isDuplicate) "Already Stored" else "${(result.confidence * 100).toInt()}% Match",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (result.isDuplicate) AmberWarning else EmeraldPrimary
                        )
                    }
                }
            }

            // Duplicate Warning Banner
            if (result.isDuplicate) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberWarning.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = null,
                            tint = AmberWarning,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = result.duplicateMatchReason ?: "This exact transaction message was already saved previously.",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (!result.isTransaction) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = result.rejectionReason ?: "Could not extract transaction details from this text.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                return@Column
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(14.dp))

            // Extracted Details Grid
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "AMOUNT",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "$${String.format(Locale.US, "%.2f", result.amount)}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = EmeraldPrimary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "TYPE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Surface(
                        color = if (result.type.name == "INCOME") EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = result.type.name,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Merchant & Account
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "MERCHANT / PAYEE",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = result.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                if (result.accountInfo.isNotBlank()) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "ACCOUNT / CARD",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = result.accountInfo,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Category Selector with Custom Category Support
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CATEGORY (AUTO-ASSIGNED OR USER DEFINED)",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(
                    onClick = onManageCategories,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Text("+ Custom Categories", fontSize = 11.sp, color = EmeraldPrimary)
                }
            }

            var categoryDropdownOpen by remember { mutableStateOf(false) }
            Box {
                OutlinedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { categoryDropdownOpen = true },
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = getIconForName(activeCategory?.iconName ?: "category"),
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = selectedCategoryName,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = categoryColor,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = null
                        )
                    }
                }

                DropdownMenu(
                    expanded = categoryDropdownOpen,
                    onDismissRequest = { categoryDropdownOpen = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = getIconForName(cat.iconName),
                                        contentDescription = null,
                                        tint = cat.composeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(cat.name)
                                    if (!cat.isDefault) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("(Custom)", fontSize = 10.sp, color = EmeraldPrimary)
                                    }
                                }
                            },
                            onClick = {
                                onCategoryChange(cat.name)
                                categoryDropdownOpen = false
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = customNotes,
                onValueChange = onNotesChange,
                label = { Text("Notes (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_expense_button"),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (result.isDuplicate) "Save Anyway (Duplicate)" else "Save Encrypted Expense",
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
