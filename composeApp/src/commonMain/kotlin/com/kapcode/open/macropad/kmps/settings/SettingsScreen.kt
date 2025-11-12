package com.kapcode.open.macropad.kmps.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * A container screen for settings.
 *
 * @param viewModel The shared SettingsViewModel.
 * @param modifier Modifier to be applied to the content.
 * @param specificSettings A composable lambda for context-specific settings content.
 */
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    modifier: Modifier = Modifier,
    specificSettings: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        GeneralSettingsSection(viewModel = viewModel)
        specificSettings()
    }
}
