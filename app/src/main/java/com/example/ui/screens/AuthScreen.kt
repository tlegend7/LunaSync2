package com.example.ui.screens

import android.app.Activity
import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.ui.components.*
import com.example.ui.theme.LunaNavyDarkCard

@Composable
fun AuthScreen(
  authRepo: AuthRepository,
  currentRole: UserRole,
  onRoleSelected: (UserRole) -> Unit,
  onAuthSuccess: () -> Unit,
  onEnterGuestMode: () -> Unit = {},
  modifier: Modifier = Modifier,
  onOpenAppearance: () -> Unit = {}
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  Scaffold(modifier = modifier, contentWindowInsets = WindowInsets.safeDrawing,
    bottomBar = {
      Surface(color = MaterialTheme.colorScheme.background) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 24.dp, vertical = 16.dp)) {
          LunaButton("Entrar con mi cuenta", {
            val activity = context as? Activity
            if (activity == null) errorMessage = "No se pudo abrir el acceso. Intenta nuevamente."
            else {
              isLoading = true
              errorMessage = null
              authRepo.signInWithGoogle(activity, scope,
                onAuthSuccess = { isLoading = false; onAuthSuccess() },
                onAuthError = { isLoading = false; errorMessage = it },
                onCancelled = { isLoading = false })
            }
          }, Modifier.testTag("google_signin_button"), loading = isLoading, loadingText = "Abriendo acceso…")
        }
      }
    }) { padding ->
    Column(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)
      .verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)) {
      LunaContextTopBar("LunaSync / Tu espacio", "LS", onOpenAppearance)
      Text("Tu ritmo.\nSu compañía.", style = MaterialTheme.typography.headlineMedium)
      Text("Un espacio para conocerte mejor, a solas o en pareja.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
      Surface(shape = RoundedCornerShape(24.dp), color = if (dark) Color(0xFF1B3553) else LunaNavyDarkCard,
        contentColor = if (dark) Color(0xFFF6F9FF) else Color.White) {
        Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Image(painterResource(if (dark) R.drawable.luna_welcome_moon_dark else R.drawable.luna_welcome_moon_light),
            null, Modifier.size(40.dp))
          Text("LunaSync", style = MaterialTheme.typography.displayLarge)
          Text("A tu lado, sin invadir tu espacio.", style = MaterialTheme.typography.bodyLarge,
            color = if (dark) Color(0xFFB9D2EE) else Color(0xFFC2D6EC))
        }
      }
      Text("¿Cómo usarás LunaSync?", style = MaterialTheme.typography.titleLarge)
      Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        RoleChoice("Quiero seguir mi ciclo", "Mis registros y mi privacidad.", R.drawable.luna_droplets,
          currentRole == UserRole.PRIMARY_ELLA, !isLoading, { onRoleSelected(UserRole.PRIMARY_ELLA) }, "role_ella_chip")
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        RoleChoice("Quiero acompañar", "Solo lo que mi pareja decida compartir.", R.drawable.luna_heart,
          currentRole == UserRole.SUPPORT_PARTNER, !isLoading, { onRoleSelected(UserRole.SUPPORT_PARTNER) }, "role_partner_chip")
      }
      Text("Vincularse es opcional. Tú eliges qué compartir.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
      errorMessage?.let { Text(it, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("auth_error")) }
      TextButton(onClick = onEnterGuestMode, enabled = !isLoading, modifier = Modifier.testTag("guest_demo_mode_button")) {
        Text("Explorar la demostración")
      }
    }
  }
}

@Composable
private fun RoleChoice(title: String, subtitle: String, @DrawableRes icon: Int, selected: Boolean,
  enabled: Boolean, onSelect: () -> Unit, tag: String) {
  Surface(shape = RoundedCornerShape(12.dp), color = if (selected) MaterialTheme.colorScheme.primaryContainer
    else MaterialTheme.colorScheme.background) {
    Row(Modifier.fillMaxWidth().heightIn(min = 48.dp)
      .selectable(selected, enabled = enabled, role = Role.RadioButton, onClick = onSelect)
      .padding(vertical = 8.dp).testTag(tag), verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Icon(painterResource(icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground)
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      Icon(painterResource(R.drawable.luna_chevron_right), null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}
