package com.example.nodemcuclient.ui.components

import androidx.compose.material3.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import com.example.nodemcuclient.AppRoutes

data class BottomNavigationItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: String
)

@Composable
fun AppBottomBar(navController: NavHostController) {

    val items = listOf(
        BottomNavigationItem(
            title = "Servers",
            selectedIcon = Icons.Filled.Search,
            unselectedIcon = Icons.Outlined.Search,
            route = AppRoutes.HOME.route
        ),
        BottomNavigationItem(
            title = "Logs",
            selectedIcon = Icons.Filled.DateRange,
            unselectedIcon = Icons.Outlined.DateRange,
            route = AppRoutes.LOGS.route
        ),
        BottomNavigationItem(
            title = "Config",
            selectedIcon = Icons.Filled.Settings,
            unselectedIcon = Icons.Outlined.Settings,
            route = AppRoutes.HOME.route
        )
    )

    var selectedItemIndex by rememberSaveable {
        mutableStateOf(0)
    }

    NavigationBar {
        items.forEachIndexed { index, bottomNavigationItem ->
            NavigationBarItem(
                selected = selectedItemIndex == index,
                onClick = {
                    selectedItemIndex = index
                    navController.navigate(bottomNavigationItem.route)
                },
                icon = {
                    if (selectedItemIndex == index) {
                        Icon(
                            imageVector = bottomNavigationItem.selectedIcon,
                            contentDescription = "Navigation icon"
                        )
                    } else {
                        Icon(
                            imageVector = bottomNavigationItem.unselectedIcon,
                            contentDescription = "Navigation icon"
                        )
                    }
                },
                label = {
                    Text(bottomNavigationItem.title)
                }
            )
        }
    }
}
