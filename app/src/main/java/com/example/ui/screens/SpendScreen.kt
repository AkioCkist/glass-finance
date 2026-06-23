package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel

@Composable
fun SpendScreen(viewModel: FinanceViewModel) {
    var amount by remember { mutableStateOf("0") }
    var isIncome by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = if (isIncome) "Income" else "Spend")
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Toggle chọn Income / Spend
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(Color.LightGray.copy(alpha = 0.2f)),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (!isIncome) PrimaryVibrant else Color.Transparent)
                    .clickable { isIncome = false },
                contentAlignment = Alignment.Center
            ) {
                Text("Spend", color = if (!isIncome) Color.White else TextSecondary)
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (isIncome) PrimaryVibrant else Color.Transparent)
                    .clickable { isIncome = true },
                contentAlignment = Alignment.Center
            ) {
                Text("Income", color = if (isIncome) Color.White else TextSecondary)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 2. Amount Entry (ĐÃ SỬA LAYOUT ĐỂ BÁM SÁT VÀ SCALE NGAY LẬP TỨC)
        BoxWithConstraints(
            modifier = Modifier.fillMaxWidth(),
            contentAlignment = Alignment.Center // Căn giữa cả nhóm trên màn hình
        ) {
            val maxWidth = this.maxWidth // Lấy chiều rộng tối đa của màn hình

            Row(
                modifier = Modifier.widthIn(max = maxWidth), // Giới hạn Row không được vượt quá màn hình
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isIncome) "+" else "-",
                    style = Typography.displayLarge,
                    color = if (isIncome) Color(0xFF4CAF50) else Color.Red
                )
                Spacer(modifier = Modifier.width(4.dp)) // Khoảng cách nhỏ bám sát số

                // Sử dụng wrapContentWidth() để chữ bám sát nhau, không chiếm hết màn hình
                AutoResizeText(
                    text = formatAmount(amount),
                    modifier = Modifier.wrapContentWidth(),
                    maxFontSize = 40.sp,
                    minFontSize = 20.sp,
                    style = Typography.displayLarge,
                    color = TextPrimary,
                )

                Spacer(modifier = Modifier.width(8.dp)) // Khoảng cách nhỏ bám sát số
                Text("VND", style = Typography.displayLarge, color = TextSecondary)
            }
        }

        Text("From Wallet", style = Typography.bodyLarge, color = TextSecondary)

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            GlassCard(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clickable { showNoteDialog = true }
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (note.isNotEmpty()) "Edit note" else "Add note", style = Typography.bodyLarge, color = TextSecondary)
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(PrimaryVibrant)
                    .clickable {
                        val parsedAmount = amount.toDoubleOrNull() ?: 0.0
                        if (parsedAmount > 0) {
                            val finalNote = if (note.isNotBlank()) note else if (isIncome) "New Income" else "New Spend"
                            viewModel.addTransaction(parsedAmount, finalNote, isIncome = isIncome)
                            amount = "0"
                            note = ""
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text("Confirm", style = Typography.bodyLarge, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Numeric Keypad
        NumericKeypad(
            onNumber = { num ->
                if (num == "." && amount.contains(".")) return@NumericKeypad
                if (amount == "0" && num != ".") amount = num
                else amount += num
            },
            onDelete = {
                if (amount.length > 1) amount = amount.dropLast(1) else amount = "0"
            }
        )
        Spacer(modifier = Modifier.height(64.dp))
    }

    // 4. Popup Add Note
    if (showNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNoteDialog = false },
            title = { Text("Add Note") },
            text = {
                TextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Enter your note") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = { showNoteDialog = false }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    note = ""
                    showNoteDialog = false
                }) { Text("Clear") }
            }
        )
    }
}

// --- HÀM TIỆN ÍCH ---

/**
 * Composable tự động co giãn cỡ chữ khi nội dung quá dài
 * Đã tối ưu để update liền mạch không bị giật lag
 */
@Composable
fun AutoResizeText(
    text: String,
    modifier: Modifier = Modifier,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 14.sp,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified
) {
    var fontSize by remember { mutableStateOf(maxFontSize) }

    // Reset lại font size về max mỗi khi text thay đổi (để chữ tự phóng to lại khi xóa bớt số)
    LaunchedEffect(text) {
        fontSize = maxFontSize
    }

    Text(
        text = text,
        modifier = modifier,
        fontSize = fontSize,
        lineHeight = fontSize, // Giúp chữ co giãn cả theo chiều dọc, không bị lệch
        style = style,
        color = color,
        maxLines = 1,
        softWrap = false,
        onTextLayout = { result ->
            // Nếu chữ bị tràn ra ngoài không gian cho phép, giảm cỡ chữ xuống
            if (result.hasVisualOverflow) {
                val newFontSize = fontSize * 0.95f
                if (newFontSize > minFontSize) {
                    fontSize = newFontSize
                } else {
                    fontSize = minFontSize
                }
            }
        }
    )
}

private fun formatAmount(rawAmount: String): String {
    if (rawAmount.isEmpty() || rawAmount == "0") return "0"
    val parts = rawAmount.split(".")
    val intPart = parts[0]
    val decPart = if (parts.size > 1) ".${parts[1]}" else ""
    val formattedInt = intPart.reversed().chunked(3).joinToString(",").reversed()
    return formattedInt + decPart
}

@Composable
fun NumericKeypad(onNumber: (String) -> Unit, onDelete: () -> Unit) {
    val keys = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf(".", "0", "delete")
    )
    Column(modifier = Modifier.fillMaxWidth()) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable {
                                if (key == "delete") onDelete()
                                else onNumber(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (key == "delete") {
                            Icon(Icons.Default.Backspace, contentDescription = null, tint = TextPrimary)
                        } else {
                            Text(key, style = Typography.headlineMedium, color = TextPrimary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}