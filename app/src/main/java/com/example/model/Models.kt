package com.example.model

import java.util.Locale

enum class TransactionType {
    EXPENSE, INCOME, TRANSFER
}

enum class AppThemeMode {
    DARK, LIGHT, AUTO
}

enum class DatePeriod(val label: String) {
    THIS_MONTH("This Month"),
    LAST_MONTH("Last Month"),
    CUSTOM("Custom")
}

enum class NavDestination(val label: String) {
    HOME("Home"),
    TRANSACTIONS("Transactions"),
    ADD("Add"),
    PROFILE("Profile"),
    SETTINGS("Settings")
}

object CurrencyFormatter {
    fun format(
        amount: Double,
        currencyUnit: String = "INR (₹)",
        includeDecimals: Boolean = true
    ): String {
        val symbol = when {
            currencyUnit.contains("INR") || currencyUnit.contains("₹") -> "₹"
            currencyUnit.contains("USD") || currencyUnit.contains("$") -> "$"
            currencyUnit.contains("EUR") || currencyUnit.contains("€") -> "€"
            currencyUnit.contains("GBP") || currencyUnit.contains("£") -> "£"
            currencyUnit.contains("JPY") || currencyUnit.contains("¥") -> "¥"
            else -> "₹"
        }

        val isIndian = symbol == "₹" || currencyUnit.contains("INR")
        val isNegative = amount < 0
        val absAmount = kotlin.math.abs(amount)
        val intPart = absAmount.toLong()
        val decimalPart = String.format(Locale.US, "%.2f", absAmount - intPart).substring(1) // e.g. ".80"

        val intStr = intPart.toString()
        val formattedInt = if (isIndian) {
            if (intStr.length <= 3) {
                intStr
            } else {
                val lastThree = intStr.takeLast(3)
                val rest = intStr.dropLast(3)
                val groups = mutableListOf<String>()
                var i = rest.length
                while (i > 0) {
                    val start = (i - 2).coerceAtLeast(0)
                    groups.add(0, rest.substring(start, i))
                    i -= 2
                }
                groups.joinToString(",") + "," + lastThree
            }
        } else {
            String.format(Locale.US, "%,d", intPart)
        }

        val formattedNum = if (includeDecimals) "$formattedInt$decimalPart" else formattedInt
        val withSymbol = "$symbol$formattedNum"
        return if (isNegative) "-$withSymbol" else withSymbol
    }
}

data class UserProfile(
    val uid: String = "",
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val currencyUnit: String = "INR (₹)",
    val themeMode: String = "DARK",
    val totalLiquidity: Double = 0.0,
    val protectedVault: Double = 0.0,
    val netWorth: Double = 0.0,
    val avatarUri: String? = null
)

data class Transaction(
    val id: String = "",
    val title: String = "",
    val category: String = "General",
    val timestamp: String = "",
    val dateGroup: String = "Today",
    val accountName: String = "HDFC Checking",
    val amount: Double = 0.0,
    val status: String = "Settled",
    val type: TransactionType = if (amount < 0) TransactionType.EXPENSE else TransactionType.INCOME,
    val note: String = "",
    val iconName: String = "payments",
    val isSplit: Boolean = false,
    val isRecurring: Boolean = false
)

data class AccountItem(
    val id: String = "",
    val name: String = "",
    val mask: String = "••••",
    val subtext: String = "Primary Account",
    val balance: Double = 0.0,
    val isCredit: Boolean = false,
    val apy: String? = null,
    val icon: String = "payments"
)

data class FinancialMilestone(
    val id: String = "",
    val name: String = "",
    val currentAmount: Double = 0.0,
    val targetAmount: Double = 0.0,
    val isCyan: Boolean = true
) {
    val percentage: Int get() = if (targetAmount > 0) ((currentAmount / targetAmount) * 100).toInt().coerceIn(0, 100) else 0
    val remaining: Double get() = (targetAmount - currentAmount).coerceAtLeast(0.0)
}

data class StructuralAllocation(
    val category: String = "",
    val percentage: Int = 0,
    val amount: Double = 0.0,
    val icon: String = "payments"
)

data class NetFlowDay(
    val day: String = "",
    val netAmount: Double = 0.0,
    val isHighlight: Boolean = false
)

data class MonthlyTrendPoint(
    val month: String = "",
    val fullMonth: String = "",
    val inflow: Double = 0.0,
    val outflow: Double = 0.0,
    val net: Double = inflow - outflow
)
