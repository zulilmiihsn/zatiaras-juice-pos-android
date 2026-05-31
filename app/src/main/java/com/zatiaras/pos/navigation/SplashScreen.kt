package com.zatiaras.pos.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.zatiaras.pos.core.ui.R as CoreUiR

/**
 * Startup splash while session restoration decides the first real destination.
 *
 * Keep this screen visual-only; routing decisions belong in StartupViewModel and
 * AppNavGraph so startup behavior remains testable.
 */
@Composable
fun SplashScreen(
    modifier: Modifier = Modifier,
) {
    // Use a subtle infinite pulse so startup feels alive without implying
    // progress that the app cannot measure here.
    val infiniteTransition = rememberInfiniteTransition(label = "logo_pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "logo_scale",
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(id = CoreUiR.drawable.zatiaras_logo),
                contentDescription = stringResource(CoreUiR.string.brand_logo_content_description),
                modifier = Modifier
                    .size(180.dp)
                    .scale(scale),
            )

            Spacer(modifier = Modifier.height(32.dp))

            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
