package com.example.blerobotcarcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import com.example.blerobotcarcontrol.ui.screens.MainScreen
import com.example.blerobotcarcontrol.ui.viewmodel.MainViewModel
import com.example.blerobotcarcontrol.ui.theme.BLERobotCarControlTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            BLERobotCarControlTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {

                    val viewModel: MainViewModel = viewModel(
                        factory = AndroidViewModelFactory(application)
                    )

                    MainScreen(viewModel = viewModel)

                }
            }
        }
    }
}
