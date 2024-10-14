package com.example.nodemcuclient.ui.servers

import android.content.ContentValues.TAG
import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nodemcuclient.ui.theme.NodemcuclientTheme
import com.google.gson.Gson
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import okio.IOException
import java.io.File
import java.io.FileWriter

class ServersViewModel : ViewModel() {

    // StateFlow to hold UI state
    private var _serverList = MutableStateFlow<MutableList<MCUServer>>(mutableListOf())
    private var _selectedServer = MutableStateFlow<MutableMap<String, MCUServer>>(mutableMapOf())
    var serverList: StateFlow<MutableList<MCUServer>> = _serverList
    var selectedServer: StateFlow<MutableMap<String, MCUServer>> = _selectedServer

    fun selectServer(context: Context, server: MCUServer) {
        // Assuming _serverList is a list of MCUServer
        val index = _serverList.value.indexOf(server)

        if (server.url == _selectedServer.value.values.first().url) {
            Log.d(TAG, "Server already selected")
            return
        }

        if (index != -1) {
            val selectedServer = _serverList.value[index]

            val currentMap = _selectedServer.value.toMutableMap()

            // Add or update the selected server in the map
            currentMap["selectedServer"] = selectedServer
            _selectedServer.value = currentMap // Update the state flow
            saveData(context, _selectedServer.value, _serverList.value)
            Log.d("SelectedServer", _selectedServer.value.toString())
        } else {
            Log.d("SelectedServer", "Server not found in the list")
        }
    }

    fun addNewServer(context: Context, server: MCUServer) {
        _serverList.value.add(server)
        saveData(context, _selectedServer.value, _serverList.value)
        Log.d(TAG, _serverList.value.joinToString(", "))
    }

    fun deleteServer(context: Context, server: MCUServer) {
        val removed = _serverList.value.remove(server)
        if (removed) {
            if (server.url == _selectedServer.value.values.first().url) {
                _selectedServer.value = mutableMapOf("selectedServer" to MCUServer("", ""))
                saveData(context, _selectedServer.value, _serverList.value)
                Log.d(TAG, "Server deleted successfully")
            }
            saveData(context, _selectedServer.value, _serverList.value)
            Log.d(TAG, "Server deleted successfully")
        } else {
            Log.e(TAG, "Server not found")
        }
    }

    private fun saveData(
        context: Context,
        server: MutableMap<String, MCUServer>,
        serverList: MutableList<MCUServer>,
        filename: String = "data.json"
    ) {

        if (selectedServer.value.isEmpty()) {
            println("Error: No server available to save.")
            return // Exit the function if there is no server
        }

        // Get the file in the internal storage directory
        val file = File(context.filesDir, filename)

        try {
            FileWriter(file).use { writer ->
                val save = SavedData(selectedServer = server.values.first(), serverList = serverList)
                val gson = Gson()
                val jsonString = gson.toJson(save)
                writer.write(jsonString)
            }
            println("data.json already exists")
        } catch (e: IOException) {
            e.printStackTrace() // Handle the error
        }
    }


    fun loadServers(context: Context) {
        val file = File(context.filesDir, "data.json") // Make sure the file name is included

        viewModelScope.launch {
            if (!file.exists()) {
                Log.d(TAG, "File does not exist")
                FileWriter(file).use { writer ->
                    val emptyServer = MCUServer(name = "", url = "")
                    val emptyData =
                        SavedData(serverList = emptyList(), selectedServer = emptyServer)
                    val gson = Gson()
                    val jsonString = gson.toJson(emptyData)
                    writer.write(jsonString)
                    println("data.json created with empty values")
                }
                return@launch // Exit the function if the file doesn't exist
            }

            val jsonString = file.readText()

            if (jsonString.isBlank()) {
                Log.d(TAG, "File is empty or contains only whitespace")
                return@launch // Exit the function if the file is empty
            }

            try {
                // Deserialize the JSON string into the SavedData object
                val fileContent: SavedData = Gson().fromJson(jsonString, SavedData::class.java)

                // Update selected server and server list from the file content
                Log.d(TAG, "Reading file content $jsonString")
                _serverList.value = fileContent.serverList.toMutableList()
                _selectedServer.value = mutableMapOf("selectedServer" to fileContent.selectedServer)

            } catch (e: Exception) {
                Log.e(TAG, "Error reading JSON data", e) // Handle JSON parsing errors
            }
        }
    }

    data class MCUServer(val name: String, val url: String)

    data class ServerLists(
        val selectedServer: MCUServer,
        val serverLists: MutableStateFlow<MutableList<MCUServer>>
    )

    data class SavedData(val serverList: List<MCUServer>, val selectedServer: MCUServer)

    @Serializable
    data class SavedServers(val selectedServer: MCUServer, val serverList: ServerLists)
}
