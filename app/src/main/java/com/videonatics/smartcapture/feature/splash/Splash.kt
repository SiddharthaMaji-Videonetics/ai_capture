package com.videonatics.smartcapture.feature.splash

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.videonatics.smartcapture.R
import com.videonatics.smartcapture.root.PageRoute
import com.videonatics.smartcapture.ui.theme.LocalPrimarySurfaceBackground
import com.videonatics.smartcapture.ui.theme.LocalTextPrimaryColor

@Composable
fun SplashScreen(navController: NavController) {
    // Check if we are in preview mode to avoid HiltViewModel initialization issues
    if (LocalInspectionMode.current) {
        SplashPageComponents()
        return
    }

    val viewModel = hiltViewModel<SplashViewModel>()
    
    // Using LaunchedEffect to handle the splash logic as a side effect
    LaunchedEffect(Unit) {
        viewModel.loadSplash {
            navController.navigate(it.name) {
                popUpTo(PageRoute.SPLASH.name) {
                    inclusive = true
                }
            }
        }
    }
    
    SplashPageComponents()
}

@Composable
fun SplashPageComponents() {

    Scaffold(
        containerColor = LocalPrimarySurfaceBackground.current,
        bottomBar = {

            BottomAppBar(
                containerColor = LocalPrimarySurfaceBackground.current
                ,
                modifier = Modifier
                    .fillMaxWidth()

            ) {
                Box( modifier = Modifier
                    .fillMaxWidth()){
                    Text("Powered by Videonetics",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalTextPrimaryColor.current, modifier = Modifier.align(Alignment.Center))
                }

            }

        }
    ) {padding ->
        Box (
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
        ){
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,

                ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    alignment = Alignment.Center,
                    modifier = Modifier.width(90.dp).height(90.dp)
                )
                Text(text = "Smart Capture",
                    fontSize = 30.sp,
                    color = LocalTextPrimaryColor.current,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                        .padding(5.dp)
                )


            }

        }
    }

}