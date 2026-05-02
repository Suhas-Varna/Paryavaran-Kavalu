package com.example.paryavaran_kavalu

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.paryavaran_kavalu.ui.AppNavigation
import com.example.paryavaran_kavalu.ui.theme.ParyavaranKavaluTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            ParyavaranKavaluTheme {
                AppNavigation()   
            }
        }
    }
}