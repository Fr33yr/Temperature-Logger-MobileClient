package com.example.nodemcuclient.ui.logs

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.nodemcuclient.ui.components.AppTopBar
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlinx.coroutines.launch
import java.text.DateFormatSymbols
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LogsScreen(
    navController: NavHostController,
    viewModel: LogsViewModel = viewModel(),
) {

    val context = LocalContext.current
    val connectionStatus = viewModel.connectionStatus.collectAsState().value
    val modelProducer = viewModel.modelProducer.collectAsState().value


    val coroutineScope = rememberCoroutineScope()
    // Date formats
    val monthNames = DateFormatSymbols.getInstance(Locale.US).shortMonths
    val bottomAxisValueFormatter = CartesianValueFormatter { _, x, _ ->
        val date = LocalDateTime.ofEpochSecond(
            x.toLong(),
            0,
            ZoneOffset.UTC
        ) // Convert the x value back to a LocalDate
        val monthName = monthNames[date.monthValue - 1] // Get the month name
        "${date.dayOfMonth} $monthName, ${date.hour}:${date.minute}" // Format as short year
    }

    Scaffold(
        topBar = { AppTopBar(title = "Logs Screen") },
        content = { paddingValues ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.padding(vertical = 24.dp))
                if (connectionStatus.status == LogsViewModel.ConnectionStatus.AWAITING.status) {
                    Text(text = "Waiting for connection...")
                } else if (connectionStatus.status == LogsViewModel.ConnectionStatus.CONNECTED.status) {
                    Text(text = "Connected")
                } else {
                    Text(text = "Error")
                }
                Spacer(modifier = Modifier.padding(vertical = 16.dp))
                Column() {
                    Row() {
                        Button(onClick = {
                            coroutineScope.launch {
                                viewModel.setChartToLive(false)
                                viewModel.getWeeklyLogs()
                            }
                        }, shape = RectangleShape) {
                            Text("Week")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                        Button(onClick = {
                            coroutineScope.launch {
                                viewModel.setChartToLive(false)
                                viewModel.getDalyLogs()
                            }
                        }, shape = RectangleShape) {
                            Text("Day")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                        Button(onClick = {
                            coroutineScope.launch {
                                viewModel.setChartToLive(false)
                                viewModel.getHourlyLogs()
                            }
                        }, shape = RectangleShape) {
                            Text("Hourly")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                        Button(onClick = {
                            coroutineScope.launch {
                                viewModel.setChartToLive(true)
                            }
                        }, shape = RectangleShape) {
                            Text("Live")
                        }
                    }
                    Card(
                        elevation = CardDefaults.elevatedCardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    ) {
                        val dateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
                        val xToDateMapKey = ExtraStore.Key<Map<Float, LocalDate>>()
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer(),
                                startAxis = VerticalAxis.rememberStart(),
                                bottomAxis = HorizontalAxis.rememberBottom(
                                    valueFormatter = bottomAxisValueFormatter
                                ),
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 20.dp),
                            zoomState = rememberVicoZoomState(zoomEnabled = true),
                        )
                    }
                }
            }
        })

    viewModel.loadLogs(context)
}
