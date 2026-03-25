package com.videonatics.smartcapture.feature.settings.presentation.view

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.core.ensureARCoreInstalled
import com.videonatics.smartcapture.core.presentation.viewmodel.SharedCaptureViewModel
import com.videonatics.smartcapture.feature.settings.presentation.viewmodel.SettingViewModel
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import com.videonatics.smartcapture.ui.theme.ThemeMode

@Composable
fun SettingsPage(navController: NavController,
                 captureViewModel: SharedCaptureViewModel,
                 modifier: Modifier = Modifier,
                 onThemeSwitch: (ThemeMode) -> Unit) {

    val viewModel: SettingViewModel = hiltViewModel()
    if (LocalInspectionMode.current) {
        SettingsPageComponent()
        return
    }

    SettingsPageComponent(navController, modifier, viewModel, captureViewModel,onThemeSwitch )

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsPageComponent(navController: NavController? = null,
                          modifier: Modifier = Modifier, viewModel: SettingViewModel? = null,
                          captureViewModel: SharedCaptureViewModel? = null,
                          onThemeSwitch: (ThemeMode) -> Unit = {}) {

    var isOfflineAnalysis by remember { mutableStateOf(false) }
    var isDarkTheme by remember { mutableStateOf(false) }
    var isBoundBoxEnabled by remember { mutableStateOf(true) }
    var isDepthDetectionOn by remember { mutableStateOf(false) }

    val context = LocalContext.current


    LaunchedEffect(Unit){
        isDepthDetectionOn = viewModel?.isDepthDetectionOn()?: false
        isOfflineAnalysis = captureViewModel?.isOfflineAnalysis()?: false
        isDarkTheme = viewModel?.isDarkTheme()?: false
        isBoundBoxEnabled = viewModel?.isBoundBoxEnabled()?: true
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Settings")
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController?.popBackStack()
                    },
                        content = {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back"
                            )
                        })
                }

            )
        }

    ) {paddingValues ->
        Box(modifier = modifier.padding(paddingValues)){
            Column() {
//                Card(
//                    modifier = Modifier.padding(10.dp),
//                    elevation = CardDefaults.cardElevation(
//                        defaultElevation = 5.dp
//                    )
//                ) {
//                    ListItem(
//                        headlineContent = { Text("Offline Analysis") },
//                        trailingContent = {
//                            Switch(
//                                onCheckedChange = {
//                                    captureViewModel?.setOfflineAnalysis(!it)
//                                    isOfflineAnalysis = !it
//                                },
//                                checked = !isOfflineAnalysis
//                            )
//                        }
//                    )
//                }

                Card(
                    modifier = Modifier.padding(10.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("Bound Box") },
                        trailingContent = {
                            Switch(
                                onCheckedChange = {
                                    viewModel?.setBoundBox(it)
                                    isBoundBoxEnabled = it
                                },
                                checked = isBoundBoxEnabled
                            )
                        }
                    )
                }

                Card(
                    modifier = Modifier.padding(10.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("Enable Depth Detection") },
                        trailingContent = {
                            Switch(
                                onCheckedChange = {
                                    val isARCoreInstalled = ensureARCoreInstalled(context as Activity)
                                    if(isARCoreInstalled){
                                    viewModel?.setDepthDetection(it)
                                    isDepthDetectionOn = it
                                    }else{
                                        if(isDepthDetectionOn){
                                            isDepthDetectionOn = false
                                        }
                                    }

                                },
                                checked = isDepthDetectionOn
                            )
                        }
                    )
                }

                Card(
                    modifier = Modifier.padding(10.dp),
                    elevation = CardDefaults.cardElevation(
                        defaultElevation = 5.dp
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("Dark Theme") },
                        trailingContent = {
                            Switch(
                                onCheckedChange = {
                                    viewModel?.setDerkTheme(it)
                                    isDarkTheme = it
                                    onThemeSwitch(if (it) ThemeMode.DARK else ThemeMode.LIGHT)
                                },
                                checked = isDarkTheme
                            )
                        }
                    )
                }

            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsPagePreview() {
    SmartCaptureTheme {
        SettingsPageComponent()
    }
}