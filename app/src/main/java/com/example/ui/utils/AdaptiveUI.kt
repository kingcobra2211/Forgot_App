package com.example.ui.utils

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalConfiguration

enum class AppWindowWidthClass {
    Compact, Medium, Expanded
}

/**
 * Responsive metrics for device-independent UI.
 * Calculated based on AppWindowWidthClass to provide adaptive scaling.
 */
data class ResponsiveMetrics(
    val widthSizeClass: AppWindowWidthClass,
    val horizontalPadding: Dp,
    val verticalPadding: Dp,
    val gridSpacing: Dp,
    val sectionSpacing: Dp,
    val itemSpacing: Dp,
    val cardCornerRadius: Dp,
    val iconScale: Float,
    val titleFontSize: TextUnit,
    val bodyFontSize: TextUnit,
    val labelFontSize: TextUnit,
    val searchBarHeight: Dp
)

val LocalResponsiveMetrics = compositionLocalOf<ResponsiveMetrics> {
    error("No ResponsiveMetrics provided")
}

@Composable
fun ProvideResponsiveMetrics(
    widthSizeClass: AppWindowWidthClass? = null,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val calculatedClass = widthSizeClass ?: when {
        configuration.screenWidthDp < 600 -> AppWindowWidthClass.Compact
        configuration.screenWidthDp < 840 -> AppWindowWidthClass.Medium
        else -> AppWindowWidthClass.Expanded
    }
    val widthScale = (configuration.screenWidthDp / 400f).coerceIn(0.84f, 1.15f)
    val fontScale = widthScale.coerceAtLeast(1f)
    val verticalScale = (configuration.screenHeightDp / 720f).coerceIn(0.70f, 1.20f)

    val metrics = when (calculatedClass) {
        AppWindowWidthClass.Compact -> ResponsiveMetrics(
            widthSizeClass = calculatedClass,
            horizontalPadding = 16.dp * widthScale,
            verticalPadding = 12.dp * verticalScale,
            gridSpacing = 16.dp * verticalScale,
            sectionSpacing = 24.dp * verticalScale,
            itemSpacing = 12.dp * widthScale,
            cardCornerRadius = 16.dp * widthScale,
            iconScale = 1.0f,
            titleFontSize = 20.sp * fontScale,
            bodyFontSize = 16.sp * fontScale,
            labelFontSize = 13.sp * fontScale,
            searchBarHeight = 56.dp * verticalScale
        )
        AppWindowWidthClass.Medium -> ResponsiveMetrics(
            widthSizeClass = calculatedClass,
            horizontalPadding = 24.dp * widthScale,
            verticalPadding = 16.dp * verticalScale,
            gridSpacing = 20.dp * verticalScale,
            sectionSpacing = 32.dp * verticalScale,
            itemSpacing = 16.dp * widthScale,
            cardCornerRadius = 20.dp * widthScale,
            iconScale = 1.1f,
            titleFontSize = 21.sp * fontScale,
            bodyFontSize = 17.sp * fontScale,
            labelFontSize = 14.sp * fontScale,
            searchBarHeight = 64.dp * verticalScale
        )
        AppWindowWidthClass.Expanded -> ResponsiveMetrics(
            widthSizeClass = calculatedClass,
            horizontalPadding = 32.dp * widthScale,
            verticalPadding = 24.dp * verticalScale,
            gridSpacing = 24.dp * verticalScale,
            sectionSpacing = 40.dp * verticalScale,
            itemSpacing = 20.dp * widthScale,
            cardCornerRadius = 24.dp * widthScale,
            iconScale = 1.2f,
            titleFontSize = 22.sp * fontScale,
            bodyFontSize = 18.sp * fontScale,
            labelFontSize = 15.sp * fontScale,
            searchBarHeight = 72.dp * verticalScale
        )
    }

    CompositionLocalProvider(LocalResponsiveMetrics provides metrics) {
        content()
    }
}
