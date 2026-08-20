package com.payda.iptv.ui.tv

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class DeviceType {
    MOBILE,
    TV,
}

@Composable
fun rememberDeviceType(): DeviceType {
    val context = LocalContext.current
    return remember(context) {
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val currentModeType = uiModeManager?.currentModeType
            ?: (context.resources.configuration.uiMode and Configuration.UI_MODE_TYPE_MASK)

        if (currentModeType == Configuration.UI_MODE_TYPE_TELEVISION) {
            DeviceType.TV
        } else {
            DeviceType.MOBILE
        }
    }
}
