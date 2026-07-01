package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import com.example.ui.components.KeypadDialog
import com.example.ui.components.formatAmountWithCommas
import com.example.ui.theme.GlassBorder
import com.example.ui.theme.GlassWhite
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingFormScreen(
    isEdit: Boolean,
    initialTitle: String,
    initialIcon: String,
    initialNote: String,
    initialTargetAmount: String,
    initialInitialAmount: String,
    initialDeadline: Long?,
    isLoading: Boolean,
    error: String?,
    onSave: (
        title: String,
        icon: String,
        note: String,
        targetAmount: Double,
        initialAmount: Double,
        deadline: Long?
    ) -> Unit,
    onNavigateBack: () -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var icon by remember { mutableStateOf(initialIcon.ifBlank { "🎯" }) }
    var note by remember { mutableStateOf(initialNote) }
    var targetRaw by remember { mutableStateOf(initialTargetAmount.filter { it.isDigit() }.ifBlank { "0" }) }
    var initialRaw by remember { mutableStateOf(initialInitialAmount.filter { it.isDigit() }.ifBlank { "0" }) }
    var deadline by remember { mutableStateOf(initialDeadline) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTargetKeypad by remember { mutableStateOf(false) }
    var showInitialKeypad by remember { mutableStateOf(false) }

    val canSave = title.isNotBlank() && targetRaw.toDoubleOrNull()?.let { it > 0 } == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(GlassWhite)
                    .border(1.dp, GlassBorder, CircleShape)
                    .clickable { onNavigateBack() },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
            }

            Text(
                text = if (isEdit) "Edit Saving Goal" else "Add Saving Goal",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(20.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(14.dp),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Goal Name *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Buy MacBook") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = TextPrimary
                        )
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Icon/Emoji", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = icon,
                        onValueChange = { icon = it.take(2) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("🎯") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = TextPrimary
                        )
                    )
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target Amount (VND) *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showTargetKeypad = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${formatAmountWithCommas(targetRaw)} VND",
                                style = MaterialTheme.typography.bodyLarge,
                                color = TextPrimary
                            )
                            Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }

            if (!isEdit) {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Current Amount (optional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                        GlassCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showInitialKeypad = true }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${formatAmountWithCommas(initialRaw)} VND",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = TextPrimary
                                )
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Deadline (optional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val dateStr = deadline?.let {
                                SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(it))
                            }
                            Text(
                                text = dateStr ?: "No deadline",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (dateStr == null) TextSecondary else TextPrimary
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (deadline != null) {
                                    IconButton(onClick = { deadline = null }) {
                                        Text("Clear", color = TextSecondary)
                                    }
                                } else {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextSecondary)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Description", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        shape = RoundedCornerShape(16.dp),
                        placeholder = { Text("Optional note") },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = TextPrimary
                        )
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (canSave) TextPrimary else GlassBorder)
                        .clickable(enabled = canSave && !isLoading) {
                            val target = targetRaw.toDoubleOrNull() ?: return@clickable
                            val initial = if (isEdit) 0.0 else (initialRaw.toDoubleOrNull() ?: 0.0)
                            onSave(title, icon, note, target, initial, deadline)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isEdit) "Save Changes" else "Create Goal",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = if (canSave) Color.White else TextSecondary
                    )
                }
            }

            if (!error.isNullOrBlank()) {
                item {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFFFF3B30)
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = deadline ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    deadline = datePickerState.selectedDateMillis
                    showDatePicker = false
                }) {
                    Text("OK", color = TextPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTargetKeypad) {
        KeypadDialog(
            title = "Target Amount",
            initialAmount = targetRaw,
            onDismiss = { showTargetKeypad = false },
            onConfirm = {
                targetRaw = it.ifBlank { "0" }
                showTargetKeypad = false
            }
        )
    }

    if (showInitialKeypad) {
        KeypadDialog(
            title = "Current Amount",
            initialAmount = initialRaw,
            onDismiss = { showInitialKeypad = false },
            onConfirm = {
                initialRaw = it.ifBlank { "0" }
                showInitialKeypad = false
            }
        )
    }
}
