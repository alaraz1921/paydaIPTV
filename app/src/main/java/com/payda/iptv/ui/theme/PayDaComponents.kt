package com.payda.iptv.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun PayDaButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    height: Dp = 48.dp,
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(height),
        enabled = enabled,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = PayDaSurfaceFocused,
            contentColor = Color.White,
            disabledContainerColor = PayDaSurface,
            disabledContentColor = PayDaTextDisabled,
        ),
    ) {
        Text(
            text = text.uppercase(),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
fun PayDaCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var hasFocus by remember { mutableStateOf(false) }
    Surface(
        modifier = if (onClick != null) {
            modifier
                .graphicsLayer {
                    val scale = if (hasFocus && enabled) 1.025f else 1f
                    scaleX = scale
                    scaleY = scale
                }
                .onFocusChanged { hasFocus = it.hasFocus }
                .clickable(
                    enabled = enabled,
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick,
                )
        } else {
            modifier
        },
        shape = RoundedCornerShape(8.dp),
        color = when {
            !enabled -> PayDaSurface.copy(alpha = 0.72f)
            isPressed -> PayDaSurfacePressed
            hasFocus -> PayDaSurfaceFocused
            else -> PayDaSurface
        },
        border = BorderStroke(if (hasFocus) 3.dp else 1.dp, if (hasFocus) Color.White else PayDaBorder),
    ) {
        Box(
            modifier = Modifier.padding(contentPadding),
            content = content,
        )
    }
}

@Composable
fun PayDaFocusableCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    PayDaCard(
        modifier = modifier,
        onClick = onClick,
        enabled = enabled,
        contentPadding = contentPadding,
        content = content,
    )
}

@Composable
fun PayDaTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = PayDaTextPrimary,
    unfocusedTextColor = PayDaTextPrimary,
    disabledTextColor = PayDaTextDisabled,
    focusedContainerColor = PayDaSurface,
    unfocusedContainerColor = PayDaSurface,
    disabledContainerColor = PayDaSurface,
    focusedBorderColor = PayDaTextPrimary,
    unfocusedBorderColor = PayDaBorder,
    focusedLabelColor = PayDaTextPrimary,
    unfocusedLabelColor = PayDaTextSecondary,
    cursorColor = PayDaTextPrimary,
)

@Composable
fun PayDaPlaceholderLogo(
    text: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = PayDaSurfaceHigh,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = text.firstOrNull()?.uppercase() ?: "?",
                color = PayDaTextPrimary,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}
