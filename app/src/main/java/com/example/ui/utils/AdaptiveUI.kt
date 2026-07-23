package com.example.ui.utils

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.TextUnit

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
    val metrics = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> ResponsiveMetrics(
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
        WindowWidthSizeClass.Medium -> ResponsiveMetrics(
            widthSizeClass = widthSizeClass,
            horizontalPadding = 24.dp,
            verticalPadding = 16.dp,
            gridSpacing = 20.dp,
            sectionSpacing = 32.dp,
            itemSpacing = 16.dp,
            cardCornerRadius = 20.dp,
            iconScale = 1.1f,
            titleFontSize = 20.sp,
            bodyFontSize = 16.sp,
            labelFontSize = 12.sp,
            searchBarHeight = 64.dp
        )
        WindowWidthSizeClass.Expanded -> ResponsiveMetrics(
            widthSizeClass = widthSizeClass,
            horizontalPadding = 32.dp,
            verticalPadding = 24.dp,
            gridSpacing = 24.dp,
            sectionSpacing = 40.dp,
            itemSpacing = 20.dp,
            cardCornerRadius = 24.dp,
            iconScale = 1.2f,
            titleFontSize = 22.sp,
            bodyFontSize = 18.sp,
            labelFontSize = 14.sp,
            searchBarHeight = 72.dp
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
