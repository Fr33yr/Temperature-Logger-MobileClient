package com.example.nodemcuclient.ui.servers

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.nodemcuclient.AppRoutes
import com.example.nodemcuclient.ui.components.AppTopBar

@Composable
fun ServerFormScreen(
    navController: NavHostController,
    viewModel: ServersViewModel = viewModel()
) {
    var nameText by remember { mutableStateOf("") }
    var urlText by remember { mutableStateOf("") }

    val context = LocalContext.current

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(text = "Name", style = TextStyle(fontSize = 20.sp))
            TextField(value = nameText, onValueChange = { newName -> nameText = newName })
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = "Url", style = TextStyle(fontSize = 20.sp))
            TextField(value = urlText, onValueChange = { newUrl -> urlText = newUrl })
            Spacer(modifier = Modifier.height(32.dp))
            TextButton(onClick = {


                viewModel.addNewServer(context, ServersViewModel.MCUServer(nameText, urlText))
                nameText = ""
                urlText = ""
                navController.navigate(AppRoutes.HOME.route)
            }) {
                Text(text = "Add Server", style = TextStyle(fontSize = 18.sp))
            }
        }
    }
}