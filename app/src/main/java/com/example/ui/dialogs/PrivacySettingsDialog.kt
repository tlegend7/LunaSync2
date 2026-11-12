package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SharingPermissions
import com.example.ui.components.*

@Composable
fun PrivacySettingsDialog(
  currentPermissions: SharingPermissions,
  onSavePermissions: (SharingPermissions, (Boolean, String?) -> Unit) -> Unit,
  onDismiss: () -> Unit,
  canEdit: Boolean = true
) {
  var shareSymptoms by remember { mutableStateOf(currentPermissions.shareSymptoms) }
  var shareMood by remember { mutableStateOf(currentPermissions.shareMood) }
  var shareIntimacy by remember { mutableStateOf(currentPermissions.shareIntimacy) }
  var isSaving by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  LunaFlowDialog(if (canEdit) "Tú eliges" else "Lo que comparte contigo",
    if (canEdit) "Los permisos se pueden cambiar en cualquier momento."
    else "Tu pareja controla estos permisos. Aquí puedes consultarlos.",
    onDismiss, "privacy_settings_dialog", busy = isSaving,
    actions = {
      errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("privacy_error")) }
      if (canEdit) LunaButton("Guardar permisos", {
        if (!isSaving) {
          isSaving = true
          errorMessage = null
          onSavePermissions(currentPermissions.copy(shareSymptoms = shareSymptoms, shareMood = shareMood,
            shareIntimacy = shareIntimacy)) { success, error ->
            isSaving = false
            if (success) onDismiss()
            else errorMessage = error ?: "No se pudieron guardar los permisos. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("save_permissions_button"), loading = isSaving)
      else LunaButton("Volver a mi espacio", onDismiss)
    }) {
    LunaBadge("Control de privacidad", style = LunaBadgeStyle.PRIVATE)
    PermissionRow("Síntomas físicos", "Flujo, dolor y molestias registradas.", shareSymptoms,
      { shareSymptoms = it }, "toggle_share_symptoms", canEdit && !isSaving)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    PermissionRow("Ánimo y energía", "Solo los registros que decidas compartir.", shareMood,
      { shareMood = it }, "toggle_share_mood", canEdit && !isSaving)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    PermissionRow("Registros de intimidad", "Solo los no marcados como privados.", shareIntimacy,
      { shareIntimacy = it }, "toggle_share_intimacy", canEdit && !isSaving)
    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer,
      contentColor = MaterialTheme.colorScheme.onBackground) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.luna_lock_keyhole), null, Modifier.size(22.dp),
          tint = MaterialTheme.colorScheme.primary)
        Text(if (canEdit) "Tus notas privadas son solo tuyas" else "Sus notas privadas son solo suyas",
          style = MaterialTheme.typography.titleMedium)
        Text("No se comparten con tu pareja, aunque actives otros permisos.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Text("La revocación limita el acceso futuro. No borra lo que alguien ya haya visto.",
      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
private fun PermissionRow(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit,
  tag: String, enabled: Boolean) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LunaToggle(checked, onChange, Modifier.testTag(tag), enabled, "Compartir $title")
  }
}
