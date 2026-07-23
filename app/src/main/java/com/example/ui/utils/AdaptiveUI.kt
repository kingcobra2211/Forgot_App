package com.example.ui.utils

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Responsive metrics for device-independent UI.
 * Calculated based on WindowSizeClass to provide adaptive scaling.
 */
data class ResponsiveMetrics(
    val widthSizeClass: WindowWidthSizeClass,
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
    widthSizeClass: WindowWidthSizeClass,
    content: @Composable () -> Unit
) {
    val configuration = LocalConfiguration.current
    val widthScale = (configuration.screenWidthDp / 400f).coerceIn(0.84f, 1.12f)
    val fontScale = widthScale.coerceAtLeast(1f)
    val isShortScreen = configuration.screenHeightDp < 680
    val verticalScale = if (isShortScreen) 0.82f else 1f

    val metrics = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> ResponsiveMetrics(
            widthSizeClass = widthSizeClass,
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
        WindowWidthSizeClass.Medium -> ResponsiveMetrics(
            widthSizeClass = widthSizeClass,
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
        WindowWidthSizeClass.Expanded -> ResponsiveMetrics(
            widthSizeClass = widthSizeClass,
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
        else -> ResponsiveMetrics(
            widthSizeClass = widthSizeClass,
            horizontalPadding = 16.dp,
            verticalPadding = 12.dp,
            gridSpacing = 16.dp,
            sectionSpacing = 24.dp,
            itemSpacing = 12.dp,
            cardCornerRadius = 16.dp,
            iconScale = 1.0f,
            titleFontSize = 18.sp,
            bodyFontSize = 14.sp,
            labelFontSize = 11.sp,
            searchBarHeight = 56.dp
        )
    }

    CompositionLocalProvider(LocalResponsiveMetrics provides metrics) {
        content()
    }
}
