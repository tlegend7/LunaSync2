package com.example.ui.components

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import com.example.R

/** Native full-screen form: safe areas and keyboard never cover the actions. */
@Composable
@Suppress("DEPRECATION")
fun LunaFlowDialog(
  title: String,
  subtitle: String,
  onDismiss: () -> Unit,
  testTag: String,
  busy: Boolean = false,
  subtitleModifier: Modifier = Modifier,
  actions: @Composable ColumnScope.() -> Unit,
  content: @Composable ColumnScope.() -> Unit
) {
  Dialog(
    onDismissRequest = { if (!busy) onDismiss() },
    properties = DialogProperties(dismissOnBackPress = !busy, dismissOnClickOutside = !busy,
      usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
  ) {
    val view = LocalView.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    SideEffect {
      (view.parent as? DialogWindowProvider)?.window?.let { window ->
        // Compose 1.7 needs these window flags as well as decorFitsSystemWindows=false.
        window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
        window.addFlags(WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
          WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR or WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.setSoftInputMode(if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
          WindowManager.LayoutParams.SOFT_INPUT_ADJUST_NOTHING else WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
          window.attributes = window.attributes.apply { setFitInsetsTypes(0); setFitInsetsSides(0) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
          window.isStatusBarContrastEnforced = false
          window.isNavigationBarContrastEnforced = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        WindowCompat.getInsetsController(window, window.decorView).apply {
          isAppearanceLightStatusBars = !dark
          isAppearanceLightNavigationBars = !dark
        }
      }
    }
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onBackground) {
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
      .windowInsetsPadding(WindowInsets.safeDrawing.union(WindowInsets.ime)).testTag(testTag)) {
      Column(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.heightIn(min = 48.dp).clickable(enabled = !busy, role = Role.Button, onClick = onDismiss),
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(painterResource(R.drawable.luna_chevron_left), null,
            tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
          Text("Volver", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }
        Text(title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onBackground)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = subtitleModifier)
        content()
      }
      Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp), content = actions)
      }
    }
    }
  }
}
