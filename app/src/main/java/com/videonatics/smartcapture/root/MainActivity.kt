package com.videonatics.smartcapture.root

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.videonatics.smartcapture.ui.theme.SmartCaptureTheme
import com.videonatics.smartcapture.ui.theme.ThemeMode
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val permissionLauncher =
            registerForActivityResult(
                ActivityResultContracts.RequestPermission()
            ) {}

        permissionLauncher.launch(Manifest.permission.CAMERA)


        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.LIGHT) }

            SmartCaptureTheme (themeMode = themeMode){
                RootPage(onThemeSwitch = {tm ->
                    themeMode = tm
                })

            }
        }
    }
}


@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SmartCaptureTheme {
        RootPage{
        }
    }
}