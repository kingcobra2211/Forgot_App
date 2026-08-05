package com.example.ui.components

import androidx.compose.runtime.Composable
import com.example.ui.viewmodel.UpdateViewModel

@Deprecated("Replaced by AppVersionScreen for a clean OTA update flow.")
@Composable
fun UpdateDialog(
    viewModel: UpdateViewModel,
    onDismiss: () -> Unit
) {
    // Deprecated: Update flow is now presented via AppVersionScreen
}
