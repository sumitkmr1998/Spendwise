package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CategoryEntity
import com.example.ui.theme.CoralRed
import com.example.ui.theme.EmeraldPrimary

val CATEGORY_PRESET_COLORS = listOf(
    "#0D9488", // Teal/Emerald
    "#10B981", // Mint Green
    "#F97316", // Orange
    "#EC4899", // Pink
    "#8B5CF6", // Purple
    "#3B82F6", // Blue
    "#06B6D4", // Cyan
    "#EAB308", // Amber
    "#EF4444", // Red
    "#6366F1", // Indigo
    "#14B8A6", // Turquoise
    "#64748B"  // Slate
)

val CATEGORY_PRESET_ICONS = listOf(
    "category" to Icons.Default.Category,
    "fitness" to Icons.Default.FitnessCenter,
    "pets" to Icons.Default.Pets,
    "school" to Icons.Default.School,
    "card_giftcard" to Icons.Default.CardGiftcard,
    "home" to Icons.Default.Home,
    "computer" to Icons.Default.Computer,
    "spa" to Icons.Default.Spa,
    "coffee" to Icons.Default.Coffee,
    "sports_esports" to Icons.Default.SportsEsports,
    "build" to Icons.Default.Build,
    "child_care" to Icons.Default.ChildCare
)

@Composable
fun CategoryManagerDialog(
    categories: List<CategoryEntity>,
    onAddCategory: (name: String, colorHex: String, iconName: String, keywords: String) -> Unit,
    onEditCategory: (CategoryEntity) -> Unit,
    onDeleteCategory: (CategoryEntity) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreateOrEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<CategoryEntity?>(null) }
    var categoryToDelete by remember { mutableStateOf<CategoryEntity?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Manage Categories", fontWeight = FontWeight.Bold)
                IconButton(onClick = {
                    categoryToEdit = null
                    showCreateOrEditDialog = true
                }) {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "Add custom category",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Add custom categories and keywords to automatically classify your bank SMS messages.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(categories, key = { it.id }) { cat ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(cat.composeColor.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getIconForName(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = cat.composeColor,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        if (cat.isDefault) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Surface(
                                                color = MaterialTheme.colorScheme.surfaceVariant,
                                                shape = RoundedCornerShape(4.dp)
                                            ) {
                                                Text(
                                                    text = "Default",
                                                    fontSize = 9.sp,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    if (cat.keywords.isNotBlank()) {
                                        Text(
                                            text = "Keywords: ${cat.keywords}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }

                                if (!cat.isDefault) {
                                    IconButton(
                                        onClick = {
                                            categoryToEdit = cat
                                            showCreateOrEditDialog = true
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Edit,
                                            contentDescription = "Edit category",
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    IconButton(
                                        onClick = { categoryToDelete = cat },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.Delete,
                                            contentDescription = "Delete category",
                                            modifier = Modifier.size(16.dp),
                                            tint = CoralRed
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Edit/Create dialog
    if (showCreateOrEditDialog) {
        CreateOrEditCategoryDialog(
            existing = categoryToEdit,
            onDismiss = { showCreateOrEditDialog = false },
            onSave = { name, colorHex, iconName, keywords ->
                if (categoryToEdit != null) {
                    onEditCategory(
                        categoryToEdit!!.copy(
                            name = name,
                            colorHex = colorHex,
                            iconName = iconName,
                            keywords = keywords
                        )
                    )
                } else {
                    onAddCategory(name, colorHex, iconName, keywords)
                }
                showCreateOrEditDialog = false
            }
        )
    }

    // Delete confirmation
    categoryToDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = { categoryToDelete = null },
            title = { Text("Delete '${cat.name}'?") },
            text = { Text("Any existing transactions under this category will be reassigned to 'Other'.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteCategory(cat)
                        categoryToDelete = null
                    }
                ) {
                    Text("Delete", color = CoralRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun CreateOrEditCategoryDialog(
    existing: CategoryEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, colorHex: String, iconName: String, keywords: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var selectedColor by remember { mutableStateOf(existing?.colorHex ?: "#0D9488") }
    var selectedIcon by remember { mutableStateOf(existing?.iconName ?: "fitness") }
    var keywords by remember { mutableStateOf(existing?.keywords ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing != null) "Edit Category" else "New Custom Category") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Category Name") },
                    placeholder = { Text("e.g. Fitness & Gym, Pets, Subscriptions") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    text = "Matching SMS Keywords (comma-separated):",
                    style = MaterialTheme.typography.labelSmall
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    placeholder = { Text("e.g. gym, fitness, workout, planet fitness") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(text = "Choose Color:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORY_PRESET_COLORS.forEach { hex ->
                        val isSelected = selectedColor.equals(hex, ignoreCase = true)
                        val color = Color(hex.removePrefix("#").toLong(16) or 0x00000000FF000000)
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(color)
                                .clickable { selectedColor = hex }
                                .then(
                                    if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Text(text = "Choose Icon:", style = MaterialTheme.typography.labelSmall)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CATEGORY_PRESET_ICONS.forEach { (iconId, iconVec) ->
                        val isSelected = selectedIcon == iconId
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) EmeraldPrimary.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { selectedIcon = iconId }
                                .then(
                                    if (isSelected) Modifier.border(1.5.dp, EmeraldPrimary, CircleShape)
                                    else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVec,
                                contentDescription = iconId,
                                tint = if (isSelected) EmeraldPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onSave(name.trim(), selectedColor, selectedIcon, keywords.trim()) },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun getIconForName(name: String): ImageVector {
    return when (name.lowercase()) {
        "restaurant" -> Icons.Default.Restaurant
        "shopping_cart" -> Icons.Default.ShoppingCart
        "shopping_bag" -> Icons.Default.ShoppingBag
        "directions_car" -> Icons.Default.DirectionsCar
        "receipt_long" -> Icons.Default.ReceiptLong
        "movie" -> Icons.Default.Movie
        "local_hospital" -> Icons.Default.LocalHospital
        "flight" -> Icons.Default.Flight
        "account_balance_wallet" -> Icons.Default.AccountBalanceWallet
        "fitness", "fitness_center" -> Icons.Default.FitnessCenter
        "pets" -> Icons.Default.Pets
        "school" -> Icons.Default.School
        "card_giftcard" -> Icons.Default.CardGiftcard
        "home" -> Icons.Default.Home
        "computer" -> Icons.Default.Computer
        "spa" -> Icons.Default.Spa
        "coffee" -> Icons.Default.Coffee
        "sports_esports" -> Icons.Default.SportsEsports
        "build" -> Icons.Default.Build
        "child_care" -> Icons.Default.ChildCare
        else -> Icons.Default.Category
    }
}
