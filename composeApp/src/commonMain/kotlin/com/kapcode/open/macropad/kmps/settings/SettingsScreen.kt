package com.kapcode.open.macropad.kmps.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kapcode.open.macropad.kmps.AdLocation
import com.kapcode.open.macropad.kmps.PlatformAdBanner

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
    val isPro by viewModel.isPro.collectAsState()
    val isAdFree by viewModel.isAdFree.collectAsState()
    val isServerPro by viewModel.isServerProActive.collectAsState()
    val adsDisabled = isPro || isAdFree || isServerPro

    Column(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            GeneralSettingsSection(viewModel = viewModel)
            specificSettings()
        }
        
        if (!adsDisabled) {
            PlatformAdBanner(
                location = AdLocation.SETTINGS,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
    }
}
