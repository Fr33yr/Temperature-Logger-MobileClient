package com.example.nodemcuclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.nodemcuclient.ui.components.AppBottomBar
import com.example.nodemcuclient.ui.logs.LogsScreen
import com.example.nodemcuclient.ui.logs.LogsViewModel
import com.example.nodemcuclient.ui.servers.ServerFormScreen
import com.example.nodemcuclient.ui.servers.ServersScreen
import com.example.nodemcuclient.ui.servers.ServersViewModel
import com.example.nodemcuclient.ui.theme.NodemcuclientTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NodemcuclientTheme {
                MyApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyApp() {
    // Navigation controller
    val navController = rememberNavController()
    val serversViewModel: ServersViewModel = viewModel()
    val logsViewModel: LogsViewModel = viewModel()

    Scaffold(
        bottomBar = { AppBottomBar(navController = navController) }
    ) {innerPadding ->
        // Setup NavHost
        NavHost(
            navController = navController,
            startDestination = AppRoutes.HOME.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(AppRoutes.HOME.route) {
                ServersScreen(navController, serversViewModel)
            }
            composable(AppRoutes.SERVER_FORM.route){
                ServerFormScreen(navController, serversViewModel)
            }
            composable(AppRoutes.LOGS.route) {
                LogsScreen(navController, logsViewModel)
            }
        }
    }
}
