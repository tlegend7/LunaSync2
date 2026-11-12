package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.repository.FirestoreProvider
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MainScreen
import com.example.ui.dialogs.AppearanceDialog
import com.example.ui.theme.LunaSyncTheme
import com.example.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
  private val viewModel: MainViewModel by viewModels {
    object : ViewModelProvider.Factory {
      @Suppress("UNCHECKED_CAST")
      override fun <T : ViewModel> create(modelClass: Class<T>): T {
        FirestoreProvider.ensureInitialized(applicationContext)
        return MainViewModel(application) as T
      }
    }
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    FirestoreProvider.ensureInitialized(applicationContext)
    enableEdgeToEdge()

    setContent {
      val appearance by viewModel.appearanceMode.collectAsState()
      val isDark = appearance.isDark(isSystemInDarkTheme())
      SideEffect {
        WindowCompat.getInsetsController(window, window.decorView).apply {
          isAppearanceLightStatusBars = !isDark
          isAppearanceLightNavigationBars = !isDark
        }
      }
      LunaSyncTheme(darkTheme = isDark) {
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
  val appearance by viewModel.appearanceMode.collectAsState()
  var showAppearance by remember { mutableStateOf(false) }

  // Attempt silent auto-sign-in on app start
  LaunchedEffect(Unit) {
    viewModel.authRepo.attemptAutoSignIn(scope)
  }

  val isAuthed = uiState.currentUser != null || uiState.isGuestMode

  if (!isAuthed) {
    AuthScreen(
      authRepo = viewModel.authRepo,
      currentRole = uiState.currentRole,
      onRoleSelected = { role -> viewModel.selectOnboardingRole(role) },
      onAuthSuccess = { /* Automatically handled via auth state flow */ },
      onEnterGuestMode = { viewModel.enterGuestDemoMode() },
      onOpenAppearance = { showAppearance = true }
    )
    if (showAppearance) AppearanceDialog(appearance, viewModel::selectAppearance, { showAppearance = false })
  } else {
    MainScreen(viewModel = viewModel)
  }
}
