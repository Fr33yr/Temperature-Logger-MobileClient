package com.example.nodemcuclient.ui.logs

import android.annotation.SuppressLint
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
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.ColumnCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.common.data.ExtraStore
import kotlinx.coroutines.launch

@ExperimentalMaterial3Api
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun LogsScreen(
    navController: NavHostController,
    viewModel: LogsViewModel = viewModel(),
) {

    val context = LocalContext.current
    val connectionStatus = viewModel.connectionStatus.collectAsState().value

    // Datasheet
    data class TemperatureData(val time: Long, val temperature: Double)

    val modelProducer = remember { CartesianChartModelProducer() }
    val coroutineScope = rememberCoroutineScope()

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
                                viewModel.getWeeklyLogs()
                                modelProducer.runTransaction {
                                    lineSeries {
                                        series(
                                            x = listOf(1f, 2f, 3f, 4f),
                                            y = listOf(5f, 3f, 8f, 2f)
                                        )
                                    }
                                }
                            }
                        }, shape = RectangleShape) {
                            Text("Week")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                        Button(onClick = { print("") }, shape = RectangleShape) {
                            Text("Day")
                        }
                        Spacer(modifier = Modifier.padding(horizontal = 1.dp))
                        Button(onClick = { print("") }, shape = RectangleShape) {
                            Text("Live")
                        }
                    }
                    Card(
                        elevation = CardDefaults.elevatedCardElevation(4.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp)
                    ) {
                        CartesianChartHost(
                            chart = rememberCartesianChart(
                                rememberLineCartesianLayer()
                            ),
                            modelProducer = modelProducer,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        })

    viewModel.loadLogs(context)
}
