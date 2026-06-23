package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun IncomeScreen(viewModel: FinanceViewModel) {
    val monthlyIncome by viewModel.monthlyIncome.collectAsState()
    val formattedIncome = "+" + NumberFormat.getNumberInstance(Locale("vi", "VN")).format(monthlyIncome)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = "Income")
        Spacer(modifier = Modifier.height(32.dp))
        BalanceSection(title = "Monthly Income", value = formattedIncome, color = GainGreen)
        Spacer(modifier = Modifier.height(32.dp))

        // Progress Card (Allocation)
        GlassCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text("Sources", style = Typography.bodyLarge, fontWeight = FontWeight.Medium, color = TextPrimary)
                    Icon(Icons.Default.PieChart, contentDescription = null, tint = TextSecondary)
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(GlassBorder)
                ) {
                    Box(modifier = Modifier.weight(0.6f).fillMaxHeight().background(PrimaryVibrant))
                    Box(modifier = Modifier.weight(0.25f).fillMaxHeight().background(TextSecondary))
                    Box(modifier = Modifier.weight(0.15f).fillMaxHeight().background(GlassBorder))
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LegendItem("Salary", PrimaryVibrant)
                    LegendItem("Freelance", TextSecondary)
                    LegendItem("Other", GlassBorder)
                }
            }
        }
    }
}

@Composable
fun LegendItem(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(label, style = Typography.labelMedium, color = TextSecondary)
    }
}
