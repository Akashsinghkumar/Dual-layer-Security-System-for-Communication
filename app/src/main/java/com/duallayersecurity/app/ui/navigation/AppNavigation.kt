package com.duallayersecurity.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.duallayersecurity.app.ui.screens.DecryptScreen
import com.duallayersecurity.app.ui.screens.EncryptScreen
import com.duallayersecurity.app.ui.screens.HomeScreen
import com.duallayersecurity.app.ui.screens.LoginScreen
import com.duallayersecurity.app.ui.screens.RegisterScreen
import com.duallayersecurity.app.ui.screens.FileEncryptScreen
import com.duallayersecurity.app.ui.screens.FileDecryptScreen
import com.duallayersecurity.app.ui.viewmodels.CryptoStegoViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val context = LocalContext.current
    
    // Fixed Compose ViewModel lifecycle anti-pattern
    val viewModel: CryptoStegoViewModel = viewModel {
        CryptoStegoViewModel(context.applicationContext)
    }

    NavHost(navController = navController, startDestination = "login") {
        composable("login") {
            LoginScreen(navController, viewModel)
        }
        composable("register") {
            RegisterScreen(navController, viewModel)
        }
        composable("home") {
            HomeScreen(navController, viewModel)
        }
        composable("encrypt") {
            EncryptScreen(navController, viewModel)
        }
        composable("decrypt") {
            DecryptScreen(navController, viewModel)
        }
        composable("file-encrypt") {
            FileEncryptScreen(navController, viewModel)
        }
        composable("file-decrypt") {
            FileDecryptScreen(navController, viewModel)
        }
    }
}
