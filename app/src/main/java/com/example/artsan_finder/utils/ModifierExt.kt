package com.example.artsan_finder.utils

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback

/**
 * A senior-level modifier that adds a professional bounce effect + haptic feedback.
 * Similar to high-end apps like Instagram and WhatsApp.
 */
fun Modifier.bounceClickable(
    enabled: Boolean = true,
    hapticEnabled: Boolean = true,
    onClick: () -> Unit
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val haptic = LocalHapticFeedback.current

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "bounceScale"
    )

    // Trigger haptic feedback when pressed
    LaunchedEffect(isPressed) {
        if (isPressed && hapticEnabled) {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    this.graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interactionSource,
        indication = null, // Disable default ripple for a cleaner look
        enabled = enabled,
        onClick = onClick
    )
}

/**
 * Adds a smooth entrance animation to a component.
 */
fun Modifier.animateEntrance(
    delay: Int = 0
): Modifier = composed {
    val visible = remember { androidx.compose.runtime.mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (delay > 0) kotlinx.coroutines.delay(delay.toLong())
        visible.value = true
    }
    
    val alpha by animateFloatAsState(
        targetValue = if (visible.value) 1f else 0f,
        animationSpec = tween(durationMillis = 500, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "entranceAlpha"
    )
    
    val translateY by animateFloatAsState(
        targetValue = if (visible.value) 0f else 40f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "entranceTranslateY"
    )

    this.graphicsLayer {
        this.alpha = alpha
        translationY = translateY
    }
}
