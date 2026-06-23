package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.InsertChartOutlined
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.theme.*

@Composable
fun TopAppBar(title: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.ArrowBack, contentDescription = null, tint = TextPrimary)
        Text(
            text = title,
            style = Typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(GlassWhite)
                .border(1.dp, GlassBorder, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun BalanceSection(title: String, value: String, color: Color = TextPrimary) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = Typography.labelMedium,
                color = TextSecondary
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "₫ ",
                style = Typography.displayMedium,
                color = TextSecondary
            )
            Text(
                text = value,
                style = Typography.displayLarge,
                color = color
            )
        }
    }
}

@Composable
fun ChartSection(mainColor: Color) {
    val heights = listOf(30, 40, 35, 50, 70, 45, 90, 60, 100, 120, 50, 70, 80, 40, 20, 45, 30, 50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        heights.forEachIndexed { index, height ->
            Box(
                modifier = Modifier
                    .width(6.dp)
                    .height(height.dp)
                    .clip(CircleShape)
                    .background(if (index == 9) mainColor else GlassBorder)
            )
        }
    }

    Spacer(modifier = Modifier.height(24.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        val times = listOf("1D", "1W", "1M", "6M", "1Y")
        times.forEach { time ->
            Text(
                text = time,
                style = Typography.labelMedium,
                color = if (time == "1W") mainColor else TextSecondary,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
fun WarningsBanner() {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PrimaryVibrant.copy(alpha = 0.05f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Savings, contentDescription = null, tint = PrimaryVibrant)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Savings goal on track",
                    style = Typography.bodyLarge,
                    color = TextPrimary,
                    fontWeight = FontWeight.Medium
                )
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = TextSecondary)
        }
    }
}

@Composable
fun BentoGridSection() {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(title = "Cash", amount = "₫ 1.2M", icon = Icons.Default.AccountBalanceWallet)
            BentoCard(title = "Earnings", amount = "₫ 500K", icon = Icons.Default.TrendingUp)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BentoCard(title = "Investments", amount = "₫ 8.5M", icon = Icons.Default.PieChart)
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp),
                borderDashed = true
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(GlassWhite),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "New Card", style = Typography.labelMedium, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun BentoCard(title: String, amount: String, icon: ImageVector) {
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(112.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(text = title, style = Typography.labelMedium, color = TextSecondary)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(AppBackground), // subtle contrast
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = TextPrimary, modifier = Modifier.size(16.dp))
                }
            }
            Text(text = amount, style = Typography.headlineMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
        }
    }
}

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    borderDashed: Boolean = false,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(24.dp))
            .background(GlassWhite)
            .border(
                1.dp,
                if (borderDashed) GlassBorder else GlassBorder.copy(alpha = 0.5f),
                RoundedCornerShape(24.dp)
            )
    ) {
        content()
    }
}

@Composable
fun FloatingBottomNav(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .width(280.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .background(GlassWhite)
            .border(1.dp, GlassBorder, RoundedCornerShape(32.dp))
            .padding(horizontal = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            NavIcon(
                icon = Icons.Default.Home,
                isActive = currentRoute == "overview",
                onClick = { onNavigate("overview") }
            )
            NavIcon(
                icon = Icons.Outlined.ArrowDownward, // represent income
                isActive = currentRoute == "income",
                onClick = { onNavigate("income") }
            )
            NavIcon(
                icon = Icons.Outlined.ArrowUpward, // represent spend
                isActive = currentRoute == "spend",
                onClick = { onNavigate("spend") }
            )
            NavIcon(
                icon = Icons.Outlined.InsertChartOutlined, // represent history/chart
                isActive = currentRoute == "history",
                onClick = { onNavigate("history") }
            )
        }
    }
}

@Composable
fun NavIcon(icon: ImageVector, isActive: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(if (isActive) PrimaryVibrant else Color.Transparent)
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) Color.White else TextSecondary
        )
    }
}
