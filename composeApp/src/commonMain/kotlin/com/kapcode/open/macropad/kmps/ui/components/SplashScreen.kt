package com.kapcode.open.macropad.kmps.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import openmacropadkmp.composeapp.generated.resources.Res
import openmacropadkmp.composeapp.generated.resources.macropadIcon512
import org.jetbrains.compose.resources.painterResource

@Composable
fun SplashScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Black) {
        Box(contentAlignment = Alignment.Center) {
            Image(
                painter = painterResource(Res.drawable.macropadIcon512),
                contentDescription = "App Icon"
            )
        }
    }
}