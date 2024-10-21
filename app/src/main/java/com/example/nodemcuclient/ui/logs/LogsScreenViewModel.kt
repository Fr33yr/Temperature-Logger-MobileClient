package com.example.nodemcuclient.ui.logs


import android.content.ContentValues.TAG
import android.content.Context
import android.nfc.Tag
import android.text.method.TextKeyListener.clear
import android.util.Log
import androidx.compose.material3.Switch
import androidx.compose.runtime.remember
import androidx.core.view.VelocityTrackerCompat.clear
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
import kotlinx.coroutines.coroutineScope
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
        val temperature: Float,
        val created_at: String
    )

    private var _weeklylogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )

    private var _dalylogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )

    private var _hourlylogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )

    private var _livelogs: MutableStateFlow<MutableList<TemperatureLog>> = MutableStateFlow(
        mutableListOf()
    )

    // Chart Sheets
    private val _modelProducer = MutableStateFlow(CartesianChartModelProducer())
    val modelProducer: StateFlow<CartesianChartModelProducer> = _modelProducer
    private var _isLiveChart = MutableStateFlow(false)
    var isLiveChart: StateFlow<Boolean> = _isLiveChart

    suspend fun getWeeklyLogs() {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${_serverUrl.value}week")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val result = response.body?.string()
                    Log.d(TAG, "Raw JSON response: $result")
                    val gson = Gson()
                    val logs = gson.fromJson(result, Array<TemperatureLog>::class.java).toList()
                    Log.d(TAG, "Deserialized JSON response $logs")
                    _weeklylogs.value = logs.toMutableList()
                    Log.d(TAG, "Weekly logs ${_weeklylogs.value}")
                    if (_weeklylogs.value.isNotEmpty()) {
                        val chartData: MutableMap<ZonedDateTime, Float> = mutableMapOf()

                        for (item in _weeklylogs.value) {
                            val dateKey = ZonedDateTime.parse(item.created_at)
                            chartData[dateKey] = item.temperature
                        }

                        val xToDates = chartData.keys.associateBy { it.toEpochSecond().toDouble() }
                        val xToDateMapKey = ExtraStore.Key<Map<Double, ZonedDateTime>>()

                        _modelProducer.value.runTransaction {
                            lineSeries { series(xToDates.keys, chartData.values) }
                            extras { it[xToDateMapKey] = xToDates }
                        }

                    } else {
                        _modelProducer.value.runTransaction { }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null // Return null in case of error
            }
        }
    }

    suspend fun getDalyLogs() {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${_serverUrl.value}day")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val result = response.body?.string()
                    val gson = Gson()
                    val logs = gson.fromJson(result, Array<TemperatureLog>::class.java).toList()
                    _dalylogs.value = logs.toMutableList()

                    if (_dalylogs.value.isNotEmpty()) {
                        val chartData: MutableMap<ZonedDateTime, Float> = mutableMapOf()

                        for (item in _dalylogs.value) {
                            val dateKey = ZonedDateTime.parse(item.created_at)
                            chartData[dateKey] = item.temperature
                        }

                        val xToDates = chartData.keys.associateBy { it.toEpochSecond().toDouble() }
                        val xToDateMapKey = ExtraStore.Key<Map<Double, ZonedDateTime>>()

                        _modelProducer.value.runTransaction {
                            lineSeries { series(xToDates.keys, chartData.values) }
                            extras { it[xToDateMapKey] = xToDates }
                        }

                    } else {
                        _modelProducer.value.runTransaction { }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null // Return null in case of error
            }
        }
    }

    suspend fun getHourlyLogs() {
        withContext(Dispatchers.IO) {
            val request = Request.Builder()
                .url("${_serverUrl.value}hour")
                .build()
            try {
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val result = response.body?.string()
                    val gson = Gson()
                    val logs = gson.fromJson(result, Array<TemperatureLog>::class.java).toList()
                    _hourlylogs.value = logs.toMutableList()

                    if (_hourlylogs.value.isNotEmpty()) {
                        val chartData: MutableMap<ZonedDateTime, Float> = mutableMapOf()
                        Log.d(TAG, "Hourly logs ${_hourlylogs.value}")
                        for (item in _hourlylogs.value) {
                            val dateKey = ZonedDateTime.parse(item.created_at)
                            chartData[dateKey] = item.temperature
                        }

                        val xToDates = chartData.keys.associateBy { it.toEpochSecond().toDouble() }
                        val xToDateMapKey = ExtraStore.Key<Map<Double, ZonedDateTime>>()

                        _modelProducer.value.runTransaction {
                            lineSeries { series(xToDates.keys, chartData.values) }
                            extras { it[xToDateMapKey] = xToDates }
                        }

                    } else {
                        _modelProducer.value.runTransaction { }
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null // Return null in case of error
            }
        }
    }

    //Live logs
    private suspend fun getLiveLogs() {
        Log.d(TAG, "getLiveLogs ${_livelogs.value}")
        withContext(Dispatchers.IO) {
            try {
                if (_livelogs.value.isNotEmpty()) {
                    val chartData: MutableMap<ZonedDateTime, Float> = mutableMapOf()

                    for (item in _livelogs.value) {
                        val dateKey = ZonedDateTime.parse(item.created_at)
                        chartData[dateKey] = item.temperature
                    }

                    val xToDates = chartData.keys.associateBy { it.toEpochSecond().toDouble() }
                    val xToDateMapKey = ExtraStore.Key<Map<Double, ZonedDateTime>>()

                    _modelProducer.value.runTransaction {
                        lineSeries { series(xToDates.keys, chartData.values) }
                        extras { it[xToDateMapKey] = xToDates }
                    }

                } else {
                    Log.e(TAG, "No live logs have arrived yet")
                    _modelProducer.value.runTransaction { }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                null // Return null in case of error
            }
        }
    }

    suspend fun setChartToLive(isLive: Boolean) {
        _modelProducer.value.runTransaction { }
        _isLiveChart.value = isLive
    }

    fun loadLogs(context: Context) {
        val file = File(context.filesDir, "data.json")

        viewModelScope.launch(Dispatchers.IO) {
            if (isConnecting) return@launch

            if (System.currentTimeMillis() - lastConnectTime < connectDelay) return@launch
            lastConnectTime = System.currentTimeMillis()

            if (!file.exists()) {
                Log.d(TAG, "File does not exist")
                return@launch
            }

            val jsonString = file.readText()

            if (jsonString.isBlank()) {
                Log.d(TAG, "File is empty or contains only whitespace")
                return@launch
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
                    // Connection events
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
                    // Live logs event
                    on("logsupdate") { data ->
                        //exact shape of data {"id":187,"name":"sensor_1","temperature":27.13,"created_at":"2024-10-16T22:10:00.309Z"}
                        if (data.isNotEmpty()) {
                            Log.d(TAG, "$data data")
                            val gson = Gson()
                            val tempLog = gson.fromJson(data, TemperatureLog::class.java)
                            //if(_livelogs.value.size > 20){}
                            //Log.d(TAG, "$tempLog templog")
                            _livelogs.value.add(tempLog)
                            if (isLiveChart.value) {
                                viewModelScope.launch {
                                    getLiveLogs()
                                }
                            }
                        }
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

}