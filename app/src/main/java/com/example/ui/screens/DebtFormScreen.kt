package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.DebtDirection
import com.example.data.DebtPerson
import com.example.ui.components.GlassCard
import com.example.ui.components.KeypadDialog
import com.example.ui.components.formatAmountWithCommas
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DebtFormScreen(
    personId: Long?,
    persons: List<DebtPerson>,
    isEdit: Boolean,
    initialTitle: String,
    initialNote: String,
    initialAmount: String,
    initialDueDate: Long?,
    initialDirection: DebtDirection = DebtDirection.OWED_TO_ME,
    isLoading: Boolean,
    error: String?,
    onSave: (personId: Long, title: String, note: String, amount: Double, dueDate: Long?, direction: DebtDirection) -> Unit,
    onNavigateBack: () -> Unit,
    onAddPerson: (String) -> Unit
) {
    var title by remember { mutableStateOf(initialTitle) }
    var note by remember { mutableStateOf(initialNote) }
    // Store raw digits only (no commas)
    var amountRaw by remember { mutableStateOf(initialAmount.filter { it.isDigit() }) }
    var dueDate by remember { mutableStateOf(initialDueDate) }
    var direction by remember { mutableStateOf(initialDirection) }
    var selectedPersonId by remember { mutableStateOf(personId) }
    var showPersonPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showAddPersonDialog by remember { mutableStateOf(false) }
    var showKeypadDialog by remember { mutableStateOf(false) }
    var newPersonName by remember { mutableStateOf("") }

    val amountValid = amountRaw.toLongOrNull()?.let { it > 0 } == true
    val formValid = title.isNotBlank() && amountValid && selectedPersonId != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Header
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
                text = if (isEdit) "Edit Debt" else "Add Debt",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.size(40.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Person selector
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Person *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPersonPicker = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val selectedPerson = persons.find { it.id == selectedPersonId }
                            if (selectedPerson != null) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GlassBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = selectedPerson.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(selectedPerson.name, style = MaterialTheme.typography.bodyLarge, color = TextPrimary)
                                }
                            } else {
                                Text("Select a person", style = MaterialTheme.typography.bodyLarge, color = TextSecondary)
                            }
                            Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary)
                        }
                    }
                }
            }

            // Direction toggle
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Debt type *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DirectionToggleButton(
                            label = "They owe me",
                            isSelected = direction == DebtDirection.OWED_TO_ME,
                            modifier = Modifier.weight(1f)
                        ) { direction = DebtDirection.OWED_TO_ME }

                        DirectionToggleButton(
                            label = "I owe them",
                            isSelected = direction == DebtDirection.I_OWE,
                            modifier = Modifier.weight(1f)
                        ) { direction = DebtDirection.I_OWE }
                    }
                }
            }

            // Title
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Title *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Debt title") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = TextPrimary
                        )
                    )
                }
            }

            // Amount
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Amount (VND) *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    GlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showKeypadDialog = true }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val formattedAmount = formatAmountWithCommas(amountRaw)
                            Text(
                                text = if (amountRaw.isNotEmpty()) "$formattedAmount VND" else "0 VND",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (amountRaw.isNotEmpty()) TextPrimary else TextSecondary
                            )
                            Icon(Icons.Default.Edit, contentDescription = "Edit amount", tint = TextSecondary)
                        }
                    }
                }
            }

            // Due date
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Due Date (optional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
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
                            val dateStr = dueDate?.let {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
                            }
                            Text(
                                text = dateStr ?: "Select due date",
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (dueDate != null) TextPrimary else TextSecondary
                            )
                            if (dueDate != null) {
                                IconButton(onClick = { dueDate = null }, modifier = Modifier.size(24.dp)) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                                }
                            } else {
                                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = TextSecondary)
                            }
                        }
                    }
                }
            }

            // Note
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Note", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        placeholder = { Text("Optional note") },
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = TextPrimary,
                            unfocusedBorderColor = GlassBorder,
                            cursorColor = TextPrimary
                        )
                    )
                }
            }

            // Save button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(if (formValid) TextPrimary else GlassBorder)
                        .clickable(enabled = formValid && !isLoading) {
                            val amount = amountRaw.toDoubleOrNull() ?: return@clickable
                            onSave(selectedPersonId!!, title, note, amount, dueDate, direction)
                        }
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text(
                            if (isEdit) "Save Changes" else "Add Debt",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = if (formValid) Color.White else TextSecondary
                        )
                    }
                }
            }

            // Error
            if (error != null) {
                item {
                    Text(error, color = ExpenseRed, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }

    // Person picker dialog
    if (showPersonPicker) {
        AlertDialog(
            onDismissRequest = { showPersonPicker = false },
            containerColor = AppBackground,
            title = { Text("Select Person", color = TextPrimary) },
            text = {
                Column {
                    // Add person button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(TextPrimary.copy(alpha = 0.1f))
                            .clickable {
                                showPersonPicker = false
                                showAddPersonDialog = true
                            }
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(18.dp))
                            Text("Add New Person", color = TextPrimary, fontWeight = FontWeight.Medium)
                        }
                    }
                    
                    if (persons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = GlassBorder)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    if (persons.isEmpty()) {
                        Text("No people yet. Add someone above.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
                    } else {
                        LazyColumn {
                            items(persons) { person ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedPersonId = person.id
                                            showPersonPicker = false
                                        }
                                        .padding(vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(GlassBorder),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = person.name.take(1).uppercase(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = TextPrimary
                                        )
                                    }
                                    Text(person.name, color = TextPrimary)
                                    if (person.id == selectedPersonId) {
                                        Spacer(modifier = Modifier.weight(1f))
                                        Icon(Icons.Default.Check, contentDescription = null, tint = GainGreen)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPersonPicker = false }) {
                    Text("Close", color = TextSecondary)
                }
            }
        )
    }

    // Add person dialog
    if (showAddPersonDialog) {
        AlertDialog(
            onDismissRequest = { showAddPersonDialog = false },
            containerColor = AppBackground,
            title = { Text("Add Person", color = TextPrimary) },
            text = {
                OutlinedTextField(
                    value = newPersonName,
                    onValueChange = { newPersonName = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = TextPrimary,
                        unfocusedBorderColor = GlassBorder,
                        cursorColor = TextPrimary
                    )
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPersonName.isNotBlank()) {
                            onAddPerson(newPersonName.trim())
                            newPersonName = ""
                            showAddPersonDialog = false
                        }
                    },
                    enabled = newPersonName.isNotBlank()
                ) {
                    Text("Add", color = if (newPersonName.isNotBlank()) TextPrimary else TextSecondary)
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    newPersonName = ""
                    showAddPersonDialog = false 
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Date picker
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = dueDate ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dueDate = it }
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

    // Keypad popup dialog
    if (showKeypadDialog) {
        KeypadDialog(
            initialAmount = amountRaw,
            onDismiss = { showKeypadDialog = false },
            onConfirm = { newAmount ->
                amountRaw = newAmount
                showKeypadDialog = false
            }
        )
    }
}

@Composable
private fun DirectionToggleButton(
    label: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val bgColor by animateColorAsState(
        targetValue = if (isSelected) TextPrimary else GlassWhite,
        animationSpec = tween(200),
        label = "directionBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else TextPrimary,
        animationSpec = tween(200),
        label = "directionText"
    )
    val descColor by animateColorAsState(
        targetValue = if (isSelected) Color.White.copy(alpha = 0.7f) else TextSecondary,
        animationSpec = tween(200),
        label = "directionDesc"
    )

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(bgColor)
            .border(1.dp, if (isSelected) TextPrimary else GlassBorder, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(vertical = 14.dp, horizontal = 16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = textColor
            )
        }
    }
}
