package com.aaron.cameraparams

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.aaron.cameraparams.ui.MainScreen
import com.aaron.cameraparams.ui.theme.CameraParamsTheme

class CameraParamsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        startUi()
    }

    private fun startUi() {
        setContent {
            CameraParamsTheme {
                MainScreen()
            }
        }
    }
}
