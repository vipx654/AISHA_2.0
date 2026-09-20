package com.aisha.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.aisha.app.ui.AishaNavHost
import com.aisha.app.ui.theme.AishaTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AishaTheme {
                AishaNavHost()
            }
        }
    }
}
