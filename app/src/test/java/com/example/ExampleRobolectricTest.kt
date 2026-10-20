package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.model.AppThemeMode
import com.example.model.CurrencyFormatter
import com.example.model.NavDestination
import com.example.model.TransactionType
import com.example.viewmodel.FinPulseViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Kharcha Book", appName)
    }

    @Test
    fun `test Indian currency formatting logic`() {
        // Thousands
        assertEquals("₹1,245.50", CurrencyFormatter.format(1245.50, "INR (₹)"))
        assertEquals("₹12,450.80", CurrencyFormatter.format(12450.80, "INR (₹)"))

        // Lakhs (Indian grouping: 1,40,750.00)
        assertEquals("₹1,40,750.00", CurrencyFormatter.format(140750.00, "INR (₹)"))
        assertEquals("₹45,00,000.00", CurrencyFormatter.format(4500000.00, "INR (₹)"))

        // Crores (Indian grouping: 1,00,00,000.00)
        assertEquals("₹1,00,00,000.00", CurrencyFormatter.format(10000000.00, "INR (₹)"))

        // Negative values
        assertEquals("-₹845.00", CurrencyFormatter.format(-845.00, "INR (₹)"))
    }

    @Test
    fun `test add bank account by name and balance`() {
        val viewModel = FinPulseViewModel()
        val initialAccountsCount = viewModel.uiState.value.accounts.size
        val initialLiquidity = viewModel.uiState.value.totalLiquidity
        val initialNetWorth = viewModel.uiState.value.netWorth

        viewModel.addBankAccount(
            bankName = "Kotak Mahindra Bank",
            balance = 75000.0,
            isCredit = false,
            accountCategory = "Savings Account"
        )

        val updatedState = viewModel.uiState.value
        assertEquals(initialAccountsCount + 1, updatedState.accounts.size)
        assertEquals(initialLiquidity + 75000.0, updatedState.totalLiquidity, 0.01)
        assertEquals(initialNetWorth + 75000.0, updatedState.netWorth, 0.01)

        val addedAccount = updatedState.accounts.last()
        assertEquals("Kotak Mahindra Bank", addedAccount.name)
        assertEquals(75000.0, addedAccount.balance, 0.01)
        assertEquals("Savings Account", addedAccount.subtext)
    }

    @Test
    fun `test theme switching logic`() {
        val viewModel = FinPulseViewModel()
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.themeMode)

        viewModel.toggleDarkLight()
        assertEquals(AppThemeMode.LIGHT, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(AppThemeMode.AUTO)
        assertEquals(AppThemeMode.AUTO, viewModel.uiState.value.themeMode)

        viewModel.setThemeMode(AppThemeMode.DARK)
        assertEquals(AppThemeMode.DARK, viewModel.uiState.value.themeMode)
    }

    @Test
    fun `test add transaction updates list and liquidity`() {
        val viewModel = FinPulseViewModel()
        val initialCount = viewModel.uiState.value.transactions.size
        val initialLiquidity = viewModel.uiState.value.totalLiquidity

        viewModel.addTransaction(
            title = "Specialty Espresso",
            category = "Dining",
            amount = 650.0,
            type = TransactionType.EXPENSE,
            accountName = "HDFC Checking (••8421)",
            note = "Single origin",
            isSplit = false,
            isRecurring = false
        )

        val updatedState = viewModel.uiState.value
        assertEquals(initialCount + 1, updatedState.transactions.size)
        assertEquals(initialLiquidity - 650.0, updatedState.totalLiquidity, 0.01)
        assertEquals("Specialty Espresso", updatedState.transactions.first().title)
        assertEquals(NavDestination.TRANSACTIONS, updatedState.currentDestination)
    }

    @Test
    fun `test delete transaction dynamically fixes dashboard liquidity`() {
        val viewModel = FinPulseViewModel()
        val initialLiquidity = viewModel.uiState.value.totalLiquidity

        // Add expense of 1000
        viewModel.addTransaction(
            title = "Grocery Basket",
            category = "Dining",
            amount = 1000.0,
            type = TransactionType.EXPENSE,
            accountName = "HDFC Checking",
            note = "Weekly groceries",
            isSplit = false,
            isRecurring = false
        )
        val addedTxId = viewModel.uiState.value.transactions.first().id
        assertEquals(initialLiquidity - 1000.0, viewModel.uiState.value.totalLiquidity, 0.01)

        // Delete the expense -> liquidity should be restored (+1000 back)
        viewModel.deleteTransaction(addedTxId)
        assertEquals(initialLiquidity, viewModel.uiState.value.totalLiquidity, 0.01)
    }

    @Test
    fun `test clear all transactions resets transactions and updates dashboard`() {
        val viewModel = FinPulseViewModel()
        viewModel.clearAllTransactions()
        assertTrue(viewModel.uiState.value.transactions.isEmpty())
        val baselineLiquidity = viewModel.uiState.value.accounts.filter { !it.isCredit }.sumOf { it.balance }
        assertEquals(baselineLiquidity, viewModel.uiState.value.totalLiquidity, 0.01)
    }

    @Test
    fun `test discreet balance masking toggle`() {
        val viewModel = FinPulseViewModel()
        assertFalse(viewModel.uiState.value.discreetMasking)

        viewModel.toggleDiscreetMasking()
        assertTrue(viewModel.uiState.value.discreetMasking)
        assertFalse(viewModel.uiState.value.isMaskRevealed)

        viewModel.toggleMaskRevealed()
        assertTrue(viewModel.uiState.value.isMaskRevealed)
    }

    @Test
    fun `test navigation destinations`() {
        val viewModel = FinPulseViewModel()
        assertEquals(NavDestination.HOME, viewModel.uiState.value.currentDestination)

        viewModel.navigateTo(NavDestination.TRANSACTIONS)
        assertEquals(NavDestination.TRANSACTIONS, viewModel.uiState.value.currentDestination)

        viewModel.navigateTo(NavDestination.ADD)
        assertEquals(NavDestination.ADD, viewModel.uiState.value.currentDestination)

        viewModel.navigateBack()
        assertEquals(NavDestination.TRANSACTIONS, viewModel.uiState.value.currentDestination)
    }
}
