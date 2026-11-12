package com.example.ui.dialogs

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.UserRole
import com.example.data.preferences.AppAppearanceMode
import com.example.ui.components.*

@Composable
fun SettingsDialog(
  currentAppearance: AppAppearanceMode,
  onAppearanceSelected: (AppAppearanceMode) -> Unit,
  currentRole: UserRole,
  onSwitchRole: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onSignOut: () -> Unit,
  userEmail: String?,
  onDismiss: () -> Unit,
  isGuestMode: Boolean = false
) {
  val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  LunaFlowDialog("A tu gusto", "Apariencia, cuenta y preferencias.", onDismiss, "settings_dialog",
    actions = {
      LunaButton("Volver a mi espacio", onDismiss, Modifier.testTag("close_settings_button"))
    }) {
    Text("Apariencia", style = MaterialTheme.typography.titleLarge)
    AppearanceOptions(currentAppearance, onAppearanceSelected)
    Text("Puedes seguir el modo del teléfono o elegir uno.", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    SettingsRow(if (dark) "Ver modo claro" else "Ver modo oscuro", "Misma información, otra luz.",
      R.drawable.luna_moon, { onAppearanceSelected(if (dark) AppAppearanceMode.LIGHT else AppAppearanceMode.DARK) })
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SettingsRow(userEmail?.takeIf { it.isNotBlank() } ?: if (isGuestMode) "Cuenta de demostración" else "Tu cuenta",
      if (isGuestMode) "Demostración · Sin cuenta conectada" else "Cuenta personal", R.drawable.luna_circle_user_round)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    SettingsRow("Privacidad", if (currentRole == UserRole.PRIMARY_ELLA) "Tus decisiones, siempre visibles."
      else "Consulta lo que tu pareja comparte contigo.", R.drawable.luna_shield_check,
      { onDismiss(); onOpenPrivacy() }, Modifier.testTag("open_privacy_from_settings"))
    if (isGuestMode) {
      HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      SettingsRow("Cambiar experiencia", if (currentRole == UserRole.PRIMARY_ELLA) "Vista previa del rol de pareja"
        else "Vista previa del rol de ella", R.drawable.luna_heart,
        { onDismiss(); onSwitchRole() }, Modifier.testTag("switch_experience_row"))
      Text("Demostración con datos ficticios.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LunaButton(if (isGuestMode) "Salir de la demostración" else "Cerrar sesión",
      { onDismiss(); onSignOut() }, Modifier.testTag("sign_out_button"), type = LunaButtonType.DANGER)
  }
}

@Composable
fun AppearanceDialog(currentAppearance: AppAppearanceMode, onAppearanceSelected: (AppAppearanceMode) -> Unit,
  onDismiss: () -> Unit) {
  LunaFlowDialog("A tu gusto", "Puedes seguir el modo del teléfono o elegir uno.", onDismiss, "appearance_dialog",
    actions = { LunaButton("Volver", onDismiss) }) {
    Text("Apariencia", style = MaterialTheme.typography.titleLarge)
    AppearanceOptions(currentAppearance, onAppearanceSelected)
  }
}

@Composable
private fun AppearanceOptions(currentAppearance: AppAppearanceMode, onAppearanceSelected: (AppAppearanceMode) -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      AppAppearanceMode.entries.forEach { mode ->
        LunaOptionChip(when (mode) {
          AppAppearanceMode.SYSTEM -> "Sistema"
          AppAppearanceMode.LIGHT -> "Claro"
          AppAppearanceMode.DARK -> "Oscuro"
        }, selected = currentAppearance == mode, onSelect = { onAppearanceSelected(mode) },
          modifier = Modifier.weight(1f).testTag("appearance_${mode.name.lowercase()}_chip"))
      }
    }
}

@Composable
private fun SettingsRow(title: String, subtitle: String, @DrawableRes icon: Int,
  onClick: (() -> Unit)? = null, modifier: Modifier = Modifier) {
  Row(modifier.fillMaxWidth().heightIn(min = 48.dp)
    .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier),
    horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    Icon(painterResource(icon), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onBackground)
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    if (onClick != null) Icon(painterResource(R.drawable.luna_chevron_right), null, Modifier.size(16.dp),
      tint = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
