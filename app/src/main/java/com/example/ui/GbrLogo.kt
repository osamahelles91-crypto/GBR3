package com.example.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.R

@Composable
fun GbrLogo(
    modifier: Modifier = Modifier,
    scale: Float = 1.0f,
    showPaintsText: Boolean = true
) {
    Box(
        modifier = modifier
            .size((280 * scale).dp, (130 * scale).dp),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_gbr_logo),
            contentDescription = "GBR Paints Logo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Fit
        )
    }
}
