package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.ui.components.*
import com.example.ui.theme.PrimaryVibrant
import com.example.viewmodel.FinanceViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OverviewScreen(viewModel: FinanceViewModel) {
    val totalBalance by viewModel.totalBalance.collectAsState()
    
    val formattedBalance = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(totalBalance)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopAppBar(title = "Overview")
        Spacer(modifier = Modifier.height(32.dp))
        BalanceSection(title = "Total Balance", value = formattedBalance)
        Spacer(modifier = Modifier.height(48.dp))
        ChartSection(mainColor = PrimaryVibrant)
        Spacer(modifier = Modifier.height(32.dp))
        WarningsBanner()
        Spacer(modifier = Modifier.height(24.dp))
        Box(modifier = Modifier.weight(1f)) {
            BentoGridSection()
        }
    }
}
