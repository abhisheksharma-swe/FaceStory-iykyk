package com.iykyk.facestory.ui.screen

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DashboardCustomize
import androidx.compose.material.icons.rounded.Face
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.MovieFilter
import androidx.compose.material.icons.rounded.Timeline
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.iykyk.facestory.ui.theme.*

@Composable
fun ProcessingScreen(progress: Float, stage: String) {
    val infiniteTransition = rememberInfiniteTransition(label = "PulseTransition")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(BackgroundDark, BackgroundDeep)
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier.size(96.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress.coerceIn(0f, 1f) },
                    modifier = Modifier.size(96.dp),
                    color = IndigoPrimary,
                    strokeWidth = 6.dp,
                    trackColor = CardSurfaceDark
                )

                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Processing Video",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stage,
                style = MaterialTheme.typography.titleMedium,
                color = IndigoLight.copy(alpha = pulseAlpha),
                textAlign = TextAlign.Center,
                modifier = Modifier.height(44.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = IndigoPrimary,
                trackColor = CardSurfaceDark
            )

            Spacer(modifier = Modifier.height(28.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardSurfaceElevated, RoundedCornerShape(20.dp))
                    .border(1.dp, CardBorderDark, RoundedCornerShape(20.dp))
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                PipelineStepRow(
                    icon = Icons.Rounded.MovieFilter,
                    title = "Frame Extraction (8 FPS)",
                    isComplete = progress >= 0.20f,
                    isCurrent = progress < 0.20f
                )
                PipelineStepRow(
                    icon = Icons.Rounded.Face,
                    title = "ML Kit Face Detection",
                    isComplete = progress >= 0.45f,
                    isCurrent = progress in 0.20f..0.45f
                )
                PipelineStepRow(
                    icon = Icons.Rounded.Timeline,
                    title = "Temporal Appearance Tracking",
                    isComplete = progress >= 0.60f,
                    isCurrent = progress in 0.45f..0.60f
                )
                PipelineStepRow(
                    icon = Icons.Rounded.Hub,
                    title = "FaceNet Feature Embeddings",
                    isComplete = progress >= 0.80f,
                    isCurrent = progress in 0.60f..0.80f
                )
                PipelineStepRow(
                    icon = Icons.Rounded.AutoAwesome,
                    title = "Identity Clustering & Best Shot",
                    isComplete = progress >= 0.90f,
                    isCurrent = progress in 0.80f..0.90f
                )
                PipelineStepRow(
                    icon = Icons.Rounded.DashboardCustomize,
                    title = "Story Collage Rendering",
                    isComplete = progress >= 1.0f,
                    isCurrent = progress in 0.90f..1.0f
                )
            }
        }
    }
}

@Composable
private fun PipelineStepRow(
    icon: ImageVector,
    title: String,
    isComplete: Boolean,
    isCurrent: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .size(30.dp)
                .background(
                    when {
                        isComplete -> SuccessGreen
                        isCurrent -> IndigoDark
                        else -> CardSurfaceDark
                    },
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isComplete) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isCurrent) IndigoAccent else TextMutedDark,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        Text(
            text = title,
            style = if (isCurrent) MaterialTheme.typography.labelLarge else MaterialTheme.typography.bodyMedium,
            color = when {
                isComplete -> TextPrimaryDark
                isCurrent -> IndigoAccent
                else -> TextMutedDark
            }
        )
    }
}
