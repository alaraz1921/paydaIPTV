package com.payda.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.payda.iptv.ui.playlist.PayDaIptvApp
import com.payda.iptv.ui.theme.PayDaIPTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayDaIPTVTheme {
                PayDaIptvApp()
            }
        }
    }
}
