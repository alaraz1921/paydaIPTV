package com.payda.iptv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.payda.iptv.player.IptvPlayerScreen
import com.payda.iptv.ui.theme.PayDaIPTVTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PayDaIPTVTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    IptvPlayerScreen(contentPadding = innerPadding)
                }
            }
        }
    }
}
