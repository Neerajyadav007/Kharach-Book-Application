package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.model.AppThemeMode
import com.example.model.CurrencyFormatter
import com.example.model.NavDestination
import com.example.ui.components.AddBankAccountDialog
import com.example.ui.components.DeleteBankAccountDialog
import com.example.ui.components.EditBankAccountDialog
import com.example.ui.components.FinPulseBottomNav
import com.example.ui.components.FinPulseTopBar
import com.example.ui.components.NotificationsDrawerSheet
import com.example.ui.components.OcrReceiptScannerDialog
import com.example.ui.components.ToastNotification
import com.example.ui.components.TransactionInspectorSheet
import com.example.ui.components.TransferDialog
import com.example.ui.screens.AddExpenseScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.ProfileAndSettingsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.FinPulseTheme
import com.example.util.NotificationHelper
import com.example.viewmodel.FinPulseViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: FinPulseViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsState()

            val isDark = when (uiState.themeMode) {
                AppThemeMode.DARK -> true
                AppThemeMode.LIGHT -> false
                AppThemeMode.AUTO -> isSystemInDarkTheme()
            }

            FinPulseTheme(darkTheme = isDark) {
                FinPulseApp(
                    viewModel = viewModel,
                    isDark = isDark
                )
            }
        }
    }
}

@Composable
fun FinPulseApp(
    viewModel: FinPulseViewModel,
    isDark: Boolean
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Request Android 13+ Notification permission so alerts appear in mobile notification bar
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationHelper.sendNotification(
                context,
                "Kharcha Book Active",
                "Mobile status bar notifications are enabled for transactions and balance alerts."
            )
        }
    }

    LaunchedEffect(Unit) {
        NotificationHelper.initNotificationChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    // If profile was deleted or user logged out, display full-screen AuthScreen
    if (!uiState.isAuthenticated) {
        AuthScreen(
            onAuthSuccess = { name, email ->
                viewModel.onAuthSuccess(name, email, context)
            },
            onGuestLogin = {
                viewModel.onGuestLogin()
            }
        )
        return
    }

    // Back handling: If on secondary screen, navigate back to previous screen or Home
    BackHandler(enabled = uiState.currentDestination != NavDestination.HOME) {
        viewModel.navigateBack()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (uiState.currentDestination != NavDestination.ADD) {
                    FinPulseTopBar(
                        themeMode = uiState.themeMode,
                        isDark = isDark,
                        onToggleTheme = { viewModel.toggleDarkLight() },
                        onNotificationsClick = { viewModel.setShowNotificationDrawer(true) },
                        onProfileClick = { viewModel.navigateTo(NavDestination.PROFILE) },
                        avatarUri = uiState.avatarUri
                    )
                }
            },
            bottomBar = {
                if (uiState.currentDestination != NavDestination.ADD) {
                    FinPulseBottomNav(
                        currentDestination = uiState.currentDestination,
                        onNavigate = { destination ->
                            viewModel.navigateTo(destination)
                        }
                    )
                }
            },
            modifier = Modifier.fillMaxSize()
        ) { innerPadding ->
            Crossfade(
                targetState = uiState.currentDestination,
                animationSpec = tween(180),
                label = "ScreenTransition"
            ) { destination ->
                when (destination) {
                    NavDestination.HOME -> {
                        HomeScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            isDark = isDark,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    NavDestination.TRANSACTIONS -> {
                        LedgerScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                    NavDestination.ADD -> {
                        AddExpenseScreen(
                            viewModel = viewModel,
                            isDark = isDark,
                            onBack = { viewModel.navigateBack() }
                        )
                    }
                    NavDestination.PROFILE, NavDestination.SETTINGS -> {
                        ProfileAndSettingsScreen(
                            uiState = uiState,
                            viewModel = viewModel,
                            isDark = isDark,
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }

        // Floating Toast Notification
        ToastNotification(
            message = uiState.toastMessage,
            modifier = Modifier.align(Alignment.TopCenter)
        )

        // Internal Transfer Dialog
        TransferDialog(
            isOpen = uiState.isTransferDialogOpen,
            accounts = uiState.accounts,
            currencyUnit = uiState.currencyUnit,
            onDismiss = { viewModel.setTransferDialogOpen(false) },
            onConfirmTransfer = { from, to, amt ->
                val formatted = CurrencyFormatter.format(amt, uiState.currencyUnit)
                viewModel.showToast("Transferred $formatted from $from to $to")
            }
        )

        // Add Bank Account Dialog
        AddBankAccountDialog(
            isOpen = uiState.isAddBankDialogOpen,
            currencyUnit = uiState.currencyUnit,
            onDismiss = { viewModel.setAddBankDialogOpen(false) },
            onAddBank = { bankName, balance, isCredit, accountType ->
                viewModel.addBankAccount(bankName, balance, isCredit, accountType)
            }
        )

        // Edit Bank Account Dialog
        EditBankAccountDialog(
            account = uiState.accountToEdit,
            currencyUnit = uiState.currencyUnit,
            onDismiss = { viewModel.dismissEditAccount() },
            onConfirmEdit = { updatedAccount ->
                viewModel.updateBankAccount(updatedAccount)
            }
        )

        // Delete Bank Account Dialog
        DeleteBankAccountDialog(
            account = uiState.accountToDelete,
            onDismiss = { viewModel.dismissDeleteAccount() },
            onConfirmDelete = { accountToDelete ->
                viewModel.confirmDeleteAccount(accountToDelete, context)
            }
        )

        // OCR Receipt Scanner Dialog
        OcrReceiptScannerDialog(
            isOpen = uiState.isReceiptScannerOpen,
            onDismiss = { viewModel.setReceiptScannerOpen(false) },
            onSimulateScan = { payee, amt, cat ->
                viewModel.navigateTo(NavDestination.ADD)
                viewModel.showToast("Receipt parsed: $payee (₹$amt)")
            }
        )

        // Transaction Inspector Sheet
        TransactionInspectorSheet(
            transaction = uiState.selectedTransactionForInspect,
            currencyUnit = uiState.currencyUnit,
            onDismiss = { viewModel.selectTransactionForInspect(null) },
            onDelete = { id -> viewModel.deleteTransaction(id, context) }
        )

        // Notifications Signals Drawer
        NotificationsDrawerSheet(
            isOpen = uiState.showNotificationDrawer,
            onDismiss = { viewModel.setShowNotificationDrawer(false) }
        )
    }
}
