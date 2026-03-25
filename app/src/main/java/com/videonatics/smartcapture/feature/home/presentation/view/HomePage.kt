package com.videonatics.smartcapture.feature.home.presentation.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.core.EmptyStateWithIcon
import com.videonatics.smartcapture.feature.home.presentation.viewmodel.HomeViewModel
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.LocalTextPrimaryColor
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme

@Composable
fun HomePage(navController: NavController) {
    return HomePageComponent(onAddNewCapture = {
        navController.navigate(PageRoute.CAPTURE.name)
    },
        onSettingsClick = {
            navController.navigate(PageRoute.SETTINGS.name)
        })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomePageComponent(onAddNewCapture: () -> Unit = {}, onSettingsClick: () -> Unit = {}) {
    val viewModel = hiltViewModel<HomeViewModel>()
    return Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Home")
                },
                actions = {
                    IconButton(onClick = {
                        onSettingsClick.invoke()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = LocalTextPrimaryColor.current
                        )
                    }
                }

            )
        },
        floatingActionButton = {
            AnimatedVisibility(visible = viewModel.capturedData.isNotEmpty()) {
                ExtendedFloatingActionButton (
                    icon = {
                        Icon(Icons.Filled.Add, "Add")
                    },
                    text = {
                        Text("Add")
                    },
                    onClick = {
                        onAddNewCapture.invoke()
                    }
                )
            }

        }
    ) {
        Box(
            modifier = Modifier.padding(it)
        ){
            if(viewModel.capturedData.isEmpty()){
                EmptyStateWithIcon(
                    icon = Icons.Default.Add,
                    title = "No captures yet",
                    description = "Click the add button to start capturing",
                    actionText = "Capture"
                ){
                    onAddNewCapture.invoke()
                }
            }else{
                LazyColumn(

                ) {
                    items(viewModel.capturedData){
                        Card(
                            modifier = Modifier.padding(5.dp)
                        ) {
                            ListItem(
                                leadingContent = {
                                    Card() {
                                        Image(
                                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                                            contentDescription = "Logo",
                                            alignment = Alignment.Center,
                                            modifier = Modifier
                                                .width(90.dp)
                                                .height(90.dp)
                                                .padding(5.dp)
                                        )
                                    }
                                },
                                headlineContent = {
                                    Text(it.name)
                                },
                                supportingContent = {
                                    Text(it.desc)
                                },
                                colors = ListItemDefaults.colors(
                                    containerColor = Color(0xFF1E1F22)
                                )

                            )
                        }
                    }
                }
            }

        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomePagePreview() {
    SmartCaptureTheme {
        HomePageComponent()
    }
}