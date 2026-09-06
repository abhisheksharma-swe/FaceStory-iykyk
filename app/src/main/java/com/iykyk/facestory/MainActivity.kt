package com.iykyk.facestory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.iykyk.facestory.domain.model.ProcessingState
import com.iykyk.facestory.ui.screen.ErrorScreen
import com.iykyk.facestory.ui.screen.HomeScreen
import com.iykyk.facestory.ui.screen.NoFacesScreen
import com.iykyk.facestory.ui.screen.ProcessingScreen
import com.iykyk.facestory.ui.screen.ResultScreen
import com.iykyk.facestory.ui.theme.FaceStoryTheme
import com.iykyk.facestory.ui.viewmodel.CollageViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: CollageViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        handleIntent(intent)

        setContent {
            FaceStoryTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val state by viewModel.state.collectAsState()

                    when (val currentState = state) {
                        is ProcessingState.Idle -> {
                            HomeScreen(
                                onVideoSelected = { uri -> viewModel.selectVideo(uri) }
                            )
                        }
                        is ProcessingState.Processing -> {
                            ProcessingScreen(
                                progress = currentState.progress,
                                stage = currentState.stage
                            )
                        }
                        is ProcessingState.Success -> {
                            ResultScreen(
                                people = currentState.people,
                                collage = currentState.collage,
                                diagnostics = currentState.diagnostics,
                                onSave = { currentState.collage?.let { viewModel.saveCollage(it) } },
                                onShare = { currentState.collage?.let { viewModel.shareCollage(it) } },
                                onReset = { viewModel.reset() }
                            )
                        }
                        is ProcessingState.NoFacesFound -> {
                            NoFacesScreen(
                                framesScanned = currentState.framesScanned,
                                blurrySkipped = currentState.blurryFramesSkipped,
                                onTryAnother = { viewModel.reset() }
                            )
                        }
                        is ProcessingState.Error -> {
                            ErrorScreen(
                                message = currentState.message,
                                onRetry = { viewModel.reset() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return
        viewModel.selectVideo(uri)
    }
}
