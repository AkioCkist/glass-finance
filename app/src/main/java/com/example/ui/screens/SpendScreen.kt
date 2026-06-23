package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.StickyNote2
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
// Giữ đúng 2 màu mà bản gốc đã dùng cho dấu +/-, định nghĩa cục bộ để không phụ thuộc
// vào việc theme có sẵn IncomeGreen/SpendRed hay không.
private val AmountIncomeColor = Color(0xFF4CAF50)
private val AmountSpendColor = Color.Red

@Composable
fun SpendScreen(viewModel: FinanceViewModel) {
    var amount by remember { mutableStateOf("0") }
    var isIncome by remember { mutableStateOf(false) }
    var note by remember { mutableStateOf("") }
    var showNoteDialog by remember { mutableStateOf(false) }

    // Màu chủ đạo đổi theo Income / Spend, dùng chung cho nhiều chỗ (số tiền, nút Confirm, note chip...)
    // Dùng cùng 2 màu mà bản gốc đã dùng cho dấu +/- để không phụ thuộc vào màu chưa có trong theme.
    val accentColor by animateColorAsState(
        targetValue = if (isIncome) AmountIncomeColor else AmountSpendColor,
        animationSpec = tween(durationMillis = 350),
        label = "accentColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = if (isIncome) "Income" else "Spend")
        Spacer(modifier = Modifier.height(24.dp))

        // 1. Toggle Spend / Income — "viên thuốc" trượt mượt giữa 2 lựa chọn
        SpendIncomeToggle(
            isIncome = isIncome,
            onToggle = { isIncome = it }
        )

        Spacer(modifier = Modifier.height(40.dp))

        // 2. Amount Entry — căn giữa, scale đồng bộ giữa số tiền và "VND"
        AmountEntry(
            amount = amount,
            isIncome = isIncome,
            accentColor = accentColor
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text("From Wallet", style = Typography.bodyLarge, color = TextSecondary)

        Spacer(modifier = Modifier.height(8.dp))

        // Note đã lưu hiện ngay dưới số tiền, dạng chip nhỏ, tap để sửa lại
        AnimatedVisibility(
            visible = note.isNotBlank(),
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
            exit = fadeOut(tween(150)) + shrinkVertically(tween(150))
        ) {
            Row(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(accentColor.copy(alpha = 0.10f))
                    .clickable { showNoteDialog = true }
                    .padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.StickyNote2,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = note,
                    style = Typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

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

            val isConfirmEnabled = (amount.toDoubleOrNull() ?: 0.0) > 0.0
            val confirmAlpha by animateFloatAsState(
                targetValue = if (isConfirmEnabled) 1f else 0.4f,
                animationSpec = tween(200),
                label = "confirmAlpha"
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(accentColor.copy(alpha = confirmAlpha))
                    .clickable(enabled = isConfirmEnabled) {
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
            },
            onDeleteAll = {
                amount = "0"
            }
        )
        Spacer(modifier = Modifier.height(64.dp))
    }

    // 4. Popup Add Note
    if (showNoteDialog) {
        AddNoteDialog(
            initialNote = note,
            accentColor = accentColor,
            onDismiss = { showNoteDialog = false },
            onSave = { newNote ->
                note = newNote
                showNoteDialog = false
            },
            onClear = {
                note = ""
                showNoteDialog = false
            }
        )
    }
}

// --- CÁC COMPONENT CON ---

/**
 * Toggle Spend / Income dạng "viên thuốc" (pill) trượt mượt theo lựa chọn (spring bouncy),
 * thay cho việc chỉ đổi background tĩnh như bản cũ.
 */
@Composable
private fun SpendIncomeToggle(
    isIncome: Boolean,
    onToggle: (Boolean) -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.LightGray.copy(alpha = 0.2f))
    ) {
        val halfWidth = maxWidth / 2

        // Vị trí pill trượt mượt giữa 2 nửa, hơi nảy (bold) cho cảm giác rõ rệt
        val pillOffset by animateDpAsState(
            targetValue = if (isIncome) halfWidth else 0.dp,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pillOffset"
        )
        val pillColor by animateColorAsState(
            targetValue = if (isIncome) AmountIncomeColor else AmountSpendColor,
            animationSpec = tween(300),
            label = "pillColor"
        )

        // Viên pill nền, trượt qua lại
        Box(
            modifier = Modifier
                .offset(x = pillOffset)
                .width(halfWidth)
                .fillMaxHeight()
                .padding(4.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(pillColor)
        )

        Row(modifier = Modifier.fillMaxSize()) {
            ToggleLabel(
                text = "Spend",
                selected = !isIncome,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(false) }
            )
            ToggleLabel(
                text = "Income",
                selected = isIncome,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onToggle(true) }
            )
        }
    }
}

@Composable
private fun ToggleLabel(text: String, selected: Boolean, modifier: Modifier = Modifier) {
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        animationSpec = tween(250),
        label = "toggleTextColor"
    )
    Box(contentAlignment = Alignment.Center, modifier = modifier) {
        Text(
            text = text,
            color = textColor,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
        )
    }
}

/**
 * Khu vực hiển thị số tiền:
 * - Dấu +/- và "VND" cùng scale theo số tiền (đồng bộ tỉ lệ với phần số chính)
 * - Số tiền có animation trượt + mờ dần mượt khi gõ thêm/xóa từng ký tự
 * - Sau khi vượt 6 chữ số, cỡ chữ tiếp tục giảm dần theo mỗi chữ số thêm vào
 *   (áp dụng đồng thời cho số tiền và chữ "VND")
 */
@Composable
private fun AmountEntry(
    amount: String,
    isIncome: Boolean,
    accentColor: Color
) {
    val formatted = formatAmount(amount)

    // Đếm số chữ số thực (bỏ dấu phẩy, dấu chấm) để tính tỉ lệ scale.
    val digitCount = amount.count { it.isDigit() }

    // Cỡ chữ gốc cho phần số, tỉ lệ này dùng chung cho dấu +/- và "VND".
    val baseFontSize = 40.sp
    val minScale = 0.45f

    // Từ chữ số thứ 7 trở đi (>6 số), giảm dần 6% mỗi số thêm, có giới hạn dưới.
    val overflowDigits = (digitCount - 6).coerceAtLeast(0)
    val targetScale = (1f - overflowDigits * 0.06f).coerceAtLeast(minScale)

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 200),
        label = "amountScale"
    )

    val signFontSize = baseFontSize * animatedScale
    val amountFontSize = baseFontSize * animatedScale
    val vndFontSize = (baseFontSize * 0.55f) * animatedScale

    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.widthIn(max = maxWidth),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Dấu +/- chuyển mượt theo Income/Spend (scale + fade)
            AnimatedContent(
                targetState = isIncome,
                transitionSpec = {
                    (fadeIn(tween(200)) + scaleIn(initialScale = 0.6f, animationSpec = tween(200))) togetherWith
                            (fadeOut(tween(150)) + scaleOut(targetScale = 0.6f, animationSpec = tween(150)))
                },
                label = "signSwitch"
            ) { income ->
                Text(
                    text = if (income) "+" else "-",
                    fontSize = signFontSize,
                    style = Typography.displayLarge,
                    color = accentColor
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Số tiền: mỗi lần thay đổi (gõ/xóa) trượt + mờ dần theo chiều tăng/giảm độ dài
            AnimatedContent(
                targetState = formatted,
                transitionSpec = {
                    val growing = targetState.length >= initialState.length
                    (slideInVertically(animationSpec = tween(180)) { h -> if (growing) h / 3 else -h / 3 } +
                            fadeIn(tween(180))) togetherWith
                            (slideOutVertically(animationSpec = tween(150)) { h -> if (growing) -h / 3 else h / 3 } +
                                    fadeOut(tween(120)))
                },
                label = "amountSwitch"
            ) { text ->
                Text(
                    text = text,
                    fontSize = amountFontSize,
                    lineHeight = amountFontSize,
                    style = Typography.displayLarge,
                    color = TextPrimary,
                    maxLines = 1,
                    softWrap = false
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Text(
                text = "VND",
                fontSize = vndFontSize,
                lineHeight = vndFontSize,
                style = Typography.displayLarge,
                color = TextSecondary
            )
        }
    }
}

/**
 * Popup Add Note thiết kế lại: bo góc lớn hơn, icon, character counter, vai trò nút rõ ràng hơn.
 */
@Composable
private fun AddNoteDialog(
    initialNote: String,
    accentColor: Color,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onClear: () -> Unit
) {
    var noteText by remember { mutableStateOf(initialNote) }
    val maxLength = 60

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add a note", style = Typography.bodyLarge, fontWeight = FontWeight.SemiBold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { if (it.length <= maxLength) noteText = it },
                    placeholder = { Text("What's this for?") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor,
                        cursorColor = accentColor
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${noteText.length}/$maxLength",
                    style = Typography.bodyMedium,
                    color = TextSecondary,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(noteText.trim()) }) {
                Text("Save", color = accentColor, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onClear) {
                Text("Clear", color = TextSecondary)
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

/**
 * Bàn phím số.
 * Nút xóa: bấm thường = xóa 1 ký tự, giữ (long-press) = xóa toàn bộ số đã gõ.
 */
@Composable
fun NumericKeypad(
    onNumber: (String) -> Unit,
    onDelete: () -> Unit,
    onDeleteAll: () -> Unit
) {
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
                    if (key == "delete") {
                        DeleteKey(onDelete = onDelete, onDeleteAll = onDeleteAll)
                    } else {
                        KeypadKey(key = key, onClick = { onNumber(key) })
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun KeypadKey(key: String, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "keyScale"
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .clickable(interactionSource = interactionSource, indication = null) { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(key, style = Typography.headlineMedium, color = TextPrimary)
    }
}

/**
 * Nút xóa: tap = xóa 1 số, giữ (long-press) = xóa toàn bộ ngay lập tức,
 * sau đó tiếp tục giữ sẽ không gây lỗi gì thêm vì amount đã về "0".
 */
@Composable
private fun DeleteKey(onDelete: () -> Unit, onDeleteAll: () -> Unit) {
    var isPressed by remember { mutableStateOf(false) }
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "deleteKeyScale"
    )

    Box(
        modifier = Modifier
            .size(64.dp)
            .scale(pressScale)
            .clip(RoundedCornerShape(16.dp))
            .pointerInput(onDelete, onDeleteAll) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    },
                    onTap = { onDelete() },
                    onLongPress = { onDeleteAll() }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(Icons.Default.Backspace, contentDescription = "Delete", tint = TextPrimary)
    }
}