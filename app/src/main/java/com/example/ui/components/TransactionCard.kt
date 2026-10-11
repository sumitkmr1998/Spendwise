package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.data.model.ExpenseEntity
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TransactionCard(
    expense: ExpenseEntity,
    onDelete: (Long) -> Unit,
    onToggleFlag: (ExpenseEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.US) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
            .testTag("transaction_card_${expense.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon badge
                val cat = expense.expenseCategory
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(cat.color.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = getCategoryIcon(cat),
                        contentDescription = cat.displayName,
                        tint = cat.color,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Merchant & Meta
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = expense.merchant,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.bodySmall,
                            color = cat.color,
                            fontWeight = FontWeight.Medium
                        )
                        if (expense.accountInfoDecrypted.isNotBlank()) {
                            Text(
                                text = "•  ${expense.accountInfoDecrypted}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Amount
                Column(horizontalAlignment = Alignment.End) {
                    val amountColor = if (expense.isExpense) MaterialTheme.colorScheme.onSurface else EmeraldPrimary
                    val prefix = if (expense.isExpense) "-" else "+"
                    Text(
                        text = "$prefix$${String.format(Locale.US, "%.2f", expense.amount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = amountColor
                    )
                    Text(
                        text = SimpleDateFormat("MMM dd", Locale.US).format(Date(expense.timestamp)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Expanded view with Raw SMS message, full timestamp, and actions
            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Recorded: ${dateFormat.format(Date(expense.timestamp))}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        // Security Indicator
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = EmeraldPrimary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Keystore AES-256",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = EmeraldPrimary
                                )
                            }
                        }
                    }

                    if (expense.referenceIdDecrypted.isNotBlank()) {
                        Text(
                            text = "Reference: ${expense.referenceIdDecrypted}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (expense.rawMessageDecrypted.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    text = "ORIGINAL TEXT MESSAGE (STORED ENCRYPTED):",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = expense.rawMessageDecrypted,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { onToggleFlag(expense) }) {
                            Icon(
                                imageVector = if (expense.isFlagged) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                contentDescription = "Flag transaction",
                                tint = if (expense.isFlagged) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        IconButton(onClick = { showDeleteConfirm = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Delete transaction",
                                tint = CoralRed
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Transaction?") },
            text = { Text("Are you sure you want to delete the expense of $${expense.amount} at ${expense.merchant}?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete(expense.id)
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = CoralRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

fun getCategoryIcon(category: ExpenseCategory): ImageVector {
    return when (category) {
        ExpenseCategory.FOOD_DINING -> Icons.Default.Restaurant
        ExpenseCategory.GROCERIES -> Icons.Default.ShoppingCart
        ExpenseCategory.SHOPPING -> Icons.Default.ShoppingBag
        ExpenseCategory.TRANSPORTATION -> Icons.Default.DirectionsCar
        ExpenseCategory.BILLS_UTILITIES -> Icons.Default.ReceiptLong
        ExpenseCategory.ENTERTAINMENT -> Icons.Default.Movie
        ExpenseCategory.HEALTH_MEDICAL -> Icons.Default.LocalHospital
        ExpenseCategory.TRAVEL -> Icons.Default.Flight
        ExpenseCategory.SALARY_INCOME -> Icons.Default.AccountBalanceWallet
        ExpenseCategory.OTHER -> Icons.Default.Category
    }
}
