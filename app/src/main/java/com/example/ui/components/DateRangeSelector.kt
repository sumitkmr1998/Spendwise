package com.example.ui.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.DateRangeFilter
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeSelector(
    selectedFilter: DateRangeFilter,
    customRange: Pair<Long, Long>,
    onFilterSelected: (DateRangeFilter) -> Unit,
    onCustomRangeSelected: (Long, Long) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDatePickerDialog by remember { mutableStateOf(false) }
    val datePickerState = rememberDateRangePickerState(
        initialSelectedStartDateMillis = customRange.first,
        initialSelectedEndDateMillis = customRange.second
    )

    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            DateRangeFilter.entries.forEach { filter ->
                val isSelected = filter == selectedFilter
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (filter == DateRangeFilter.CUSTOM) {
                            showDatePickerDialog = true
                        } else {
                            onFilterSelected(filter)
                        }
                    },
                    label = {
                        Text(
                            text = if (filter == DateRangeFilter.CUSTOM && isSelected) {
                                "${dateFormat.format(Date(customRange.first))} - ${dateFormat.format(Date(customRange.second))}"
                            } else {
                                filter.displayName
                            }
                        )
                    },
                    leadingIcon = if (filter == DateRangeFilter.CUSTOM) {
                        {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Custom range",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else null,
                    modifier = Modifier.testTag("filter_chip_${filter.name.lowercase()}")
                )
            }
        }

        // Custom Date Range Dialog
        if (showDatePickerDialog) {
            DatePickerDialog(
                onDismissRequest = { showDatePickerDialog = false },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val start = datePickerState.selectedStartDateMillis
                            val end = datePickerState.selectedEndDateMillis
                            if (start != null && end != null) {
                                onCustomRangeSelected(start, end)
                            }
                            showDatePickerDialog = false
                        },
                        enabled = datePickerState.selectedStartDateMillis != null &&
                                datePickerState.selectedEndDateMillis != null
                    ) {
                        Text("Apply Filter")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDatePickerDialog = false }) {
                        Text("Cancel")
                    }
                }
            ) {
                DateRangePicker(
                    state = datePickerState,
                    title = {
                        Text(
                            text = "Select Expense Date Range",
                            modifier = Modifier.padding(start = 24.dp, top = 16.dp),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
