package com.example.nodemcuclient.ui.logs


import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nodemcuclient.ui.servers.ServersViewModel.SavedData
import com.google.gson.Gson
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import dev.icerock.moko.socket.Socket
import dev.icerock.moko.socket.SocketEvent
import dev.icerock.moko.socket.SocketOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.IOException
import java.io.File
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class LogsViewModel : ViewModel() {

    //Server Connection
    private var client = OkHttpClient()
    private var socket: Socket? = null

    // Server Connection Status
    private var _serverUrl: MutableStateFlow<String> = MutableStateFlow("")
    private var _connectionStatus: MutableStateFlow<ConnectionStatus> =
        MutableStateFlow(ConnectionStatus.AWAITING)
    var connectionStatus: StateFlow<ConnectionStatus> = _connectionStatus

    enum class ConnectionStatus(val status: String) {
        CONNECTED("Connection established"),
        AWAITING("Awaiting connection"),
        ERROR("Connection error")
    }


    private var isConnecting = false
    private var lastConnectTime: Long = 0
    private val connectDelay = 5000 // Adjust as needed


    //Logs
    data class TemperatureLog(
        val id: Int,
        val name: String,
        val value: Float,
        val created_at: String
    )

    private var _weeklylogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )
    var weeklylogs: StateFlow<MutableList<TemperatureLog>> = _weeklylogs

    private var _dalylogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )
    var dalylogs: StateFlow<MutableList<TemperatureLog>> = _dalylogs

    private var _hourlylogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )
    var hourlylogs: StateFlow<MutableList<TemperatureLog>> = _hourlylogs

    // Chart Sheets
    private val _modelProducer = MutableStateFlow(CartesianChartModelProducer())
    val modelProducer: StateFlow<CartesianChartModelProducer> = _modelProducer

    suspend fun getWeeklyLogs() {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${_serverUrl.value}week")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val result = response.body?.string()
                    val gson = Gson()
                    val logs = gson.fromJson(result, Array<TemperatureLog>::class.java).toList()
                    _weeklylogs.value = logs.toMutableList()

                    if (weeklylogs.value.isNotEmpty()) {
                        val chartData: MutableMap<ZonedDateTime, Float> = mutableMapOf()

                        for (item in weeklylogs.value) {
                            val dateKey = ZonedDateTime.parse(item.created_at)
                            chartData[dateKey] = item.value
                        }

                        val xToDates = chartData.keys.associateBy { it.toEpochSecond().toDouble() }
                        val xToDateMapKey = ExtraStore.Key<Map<Double, ZonedDateTime>>()

                        _modelProducer.value.runTransaction {
                            lineSeries { series(xToDates.keys, chartData.values) }
                            extras { it[xToDateMapKey] = xToDates }
                        }

                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null // Return null in case of error
            }
        }
    }

    fun loadLogs(context: Context) {
        val file = File(context.filesDir, "data.json") // Make sure the file name is included

        viewModelScope.launch(Dispatchers.IO) {
            if (isConnecting) return@launch

            // Debounce mechanism
            if (System.currentTimeMillis() - lastConnectTime < connectDelay) return@launch
            lastConnectTime = System.currentTimeMillis()

            if (!file.exists()) {
                Log.d(TAG, "File does not exist")
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
                if (fileContent.selectedServer.url.isEmpty()) {
                    Log.e(TAG, "No Selected Server")
                    _connectionStatus.value = ConnectionStatus.ERROR
                    return@launch
                }
                _serverUrl.value = "ws://${
                    fileContent.selectedServer.url.removePrefix("http://").removeSuffix("/")
                }/"

                // Close any previous socket before creating a new one
                socket?.disconnect()

                fun isServerReachable(url: String): Boolean {
                    return try {
                        val request = okhttp3.Request.Builder().url(url).build()
                        val response = client.newCall(request).execute()
                        Log.d(TAG, "Server reachable")
                        response.isSuccessful
                    } catch (e: Exception) {
                        _connectionStatus.value = ConnectionStatus.ERROR
                        Log.e(TAG, "Server not reachable", e)
                        false
                    }
                }

                if (!isServerReachable(fileContent.selectedServer.url)) {
                    Log.e(TAG, "Server is not reachable")
                    _connectionStatus.value = ConnectionStatus.ERROR
                    return@launch
                }

                socket = Socket(
                    endpoint = _serverUrl.value,
                    config = SocketOptions(
                        queryParams = mapOf("token" to "MySuperToken"),
                        transport = SocketOptions.Transport.WEBSOCKET
                    )
                ) {
                    on(SocketEvent.Connect) {
                        println("Connect")
                        Log.d(TAG, "Connect")
                        _connectionStatus.value = ConnectionStatus.CONNECTED
                        isConnecting = false
                    }
                    on(SocketEvent.Error) { error ->
                        Log.e(TAG, "Error" + error.message)
                        _connectionStatus.value = ConnectionStatus.ERROR
                        isConnecting = false
                    }
                    on(SocketEvent.Disconnect) {
                        Log.d(TAG, "Disconnected")
                        _connectionStatus.value = ConnectionStatus.AWAITING
                        isConnecting = false
                    }
                    on(SocketEvent.Reconnect) {
                        Log.d(TAG, "Reconnecting")
                        _connectionStatus.value = ConnectionStatus.AWAITING
                    }
                }
                // Attempt to connect
                try {
                    socket!!.connect()
                    Log.d(TAG, "Socket connection attempt started")
                } catch (e: Exception) {
                    Log.e(TAG, "Socket connection error: ${e.message}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error ", e) // Handle JSON parsing errors
            }
        }
    }

    private fun formatDateToHours(date: String): String? {
        // Parse the string into a ZonedDateTime
        val zonedDateTime = ZonedDateTime.parse(date)

        // Define the desired output format: time (e.g., 11:14 PM)
        val formatter = DateTimeFormatter.ofPattern("h:mm a")

        // Convert the ZonedDateTime to the local time zone and format it
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(formatter)
    }

    private fun formatDateToDaysAndHours(date: String): String? {
        val zonedDateTime = ZonedDateTime.parse(date)
        // Desired format
        val formatter = DateTimeFormatter.ofPattern("MMMM d, h:mm a")
        // Convert the ZonedDateTime to the local time zone and format it
        return zonedDateTime.withZoneSameInstant(ZoneId.systemDefault()).format(formatter)
    }
}