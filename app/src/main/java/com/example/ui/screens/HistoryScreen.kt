package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(viewModel: FinanceViewModel) {
    val transactions by viewModel.transactions.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = "History")
        Spacer(modifier = Modifier.height(16.dp))

        // History Chart Line visualization
        Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
            ChartSection(mainColor = TextPrimary)
        }

        Spacer(modifier = Modifier.height(24.dp))

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("Recent Transactions", style = Typography.bodyLarge, color = TextSecondary, modifier = Modifier.padding(bottom = 8.dp))
            }
            items(transactions) { tx ->
                val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
                val amountStr = (if (tx.isIncome) "+" else "-") + formatter.format(tx.amount)
                val dateStr = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(tx.timestamp))
                TransactionRow(
                    isIncome = tx.isIncome,
                    amount = amountStr,
                    title = tx.note,
                    dateStr = dateStr
                )
            }
            if (transactions.isEmpty()) {
                item {
                    Text("No transactions yet.", style = Typography.bodyMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun TransactionRow(isIncome: Boolean, amount: String, title: String, dateStr: String) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(if(isIncome) GainGreen.copy(alpha=0.1f) else ExpenseRed.copy(alpha=0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (isIncome) "↓" else "↑", color = if (isIncome) GainGreen else ExpenseRed, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(title, style = Typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Text(dateStr, style = Typography.labelMedium, color = TextSecondary)
                }
            }
            Text(amount, style = Typography.bodyLarge, fontWeight = FontWeight.Bold, color = if (isIncome) GainGreen else TextPrimary)
        }
    }
}
