package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.theme.LunaSyncTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      LunaSyncTheme {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          LunaSyncApp(viewModel = viewModel)
        }
      }
    }
  }
}

@Composable
fun LunaSyncApp(viewModel: MainViewModel) {
  val uiState by viewModel.uiState.collectAsState()
  val scope = rememberCoroutineScope()

  // Attempt silent auto-sign-in on app start
  LaunchedEffect(Unit) {
    viewModel.authRepo.attemptAutoSignIn(scope)
  }

  val isAuthed = uiState.currentUser != null || uiState.isGuestMode

  if (!isAuthed) {
    AuthScreen(
      authRepo = viewModel.authRepo,
      currentRole = uiState.currentRole,
      onRoleSelected = { role -> viewModel.switchRole(role) },
      onAuthSuccess = { /* Automatically handled via auth state flow */ },
      onEnterGuestMode = { viewModel.enterGuestDemoMode() }
    )
  } else {
    MainScreen(viewModel = viewModel)
  }
}
