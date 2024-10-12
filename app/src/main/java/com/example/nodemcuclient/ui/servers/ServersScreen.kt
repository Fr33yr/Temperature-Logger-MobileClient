package com.example.nodemcuclient.ui.servers

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.nodemcuclient.AppRoutes
import com.example.nodemcuclient.ui.components.AppTopBar


@Composable
fun ServersScreen(
    navController: NavHostController,
    viewModel: ServersViewModel = viewModel() // Get the ViewModel
) {
    // Collect the server list state from the ViewModel
    val serverList = viewModel.serverList.collectAsState().value
    val selectedServer = viewModel.selectedServer.collectAsState().value
    val context = LocalContext.current

    Scaffold (
        topBar = { AppTopBar(title = "Servers") }
    ){ paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.size(24.dp))
            if (selectedServer.isNotEmpty() && selectedServer.values.first().url.isNotEmpty() && selectedServer.values.first().name.isNotEmpty()) {
                Column {
                    Text(selectedServer.values.first().name)
                    Text(selectedServer.values.first().url)
                }
            }

            // Sectional Divider
            Spacer(modifier = Modifier.size(32.dp))
            HorizontalDivider(thickness = 2.dp, color = Color.DarkGray)
            Spacer(modifier = Modifier.size(32.dp))

            // Scrollable List
            if (serverList.isNotEmpty()) ScrollableList(
                serverList,
                viewModel,
                context,
                selectedServer
            ) else Text(text = "Add a server...")


            Button(onClick = { navController.navigate(AppRoutes.SERVER_FORM.route) }) {
                Text("Add Server")
            }
        }
    }

    // Load servers when the screen is initialized
    viewModel.loadServers(context)
}

//Scrollable list
@Composable
fun ScrollableList(
    items: List<ServersViewModel.MCUServer>,
    viewModel: ServersViewModel,
    context: Context,
    selectedServer: MutableMap<String, ServersViewModel.MCUServer>
) {
    var itemToDelete by remember { mutableStateOf<ServersViewModel.MCUServer?>(null) }

    LazyColumn {
        items(items) { item ->
            Row {
                IconButton(onClick = {
                    viewModel.selectServer(context, ServersViewModel.MCUServer(item.name, item.url))
                }, modifier = Modifier.size(48.dp)) {
                    if (selectedServer.values.first().url == item.url){
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = "Favorite",
                            tint = Color.DarkGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }else{
                        Icon(
                            imageVector = Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            tint = Color.LightGray,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                }
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(text = item.name)
                    Text(text = "Url: " + item.url)
                }
                Spacer(modifier = Modifier.size(10.dp))
                IconButton(
                    onClick = { itemToDelete = item },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = LocalContentColor.current,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.size(30.dp))
        }
    }

    // Deletion dialog
    itemToDelete?.let { server ->
        AlertDialog(
            onDismissRequest = { itemToDelete = null },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete ${server.name}?") },
            confirmButton = {
                Button(onClick = {
                    viewModel.deleteServer(context, server)
                    itemToDelete = null // Reset the dialog state
                }) {
                    Text("Delete")
                }
            },
            dismissButton = {
                Button(onClick = { itemToDelete = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}
