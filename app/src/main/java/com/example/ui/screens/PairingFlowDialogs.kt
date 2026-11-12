package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.PairingInvite
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
internal fun InviteDetailsDialog(invite: PairingInvite?, version: Long, paired: Boolean, loading: Boolean,
  error: String?, guest: Boolean, clock: Clock, onGenerate: () -> Unit, onPrivacy: () -> Unit, onDismiss: () -> Unit) {
  val context = LocalContext.current
  var copyError by remember { mutableStateOf<String?>(null) }
  fun canCopy(): Boolean = !paired && !loading && invite?.status == "ACTIVE" && invite.version == version &&
    invite.expiresAt?.toDate()?.toInstant()?.isAfter(clock.instant()) == true
  val validity = when {
    guest -> "Demostración · No vincula cuentas reales"
    paired -> "Los permisos siguen bajo tu control."
    invite == null -> "Genera un código cuando quieras invitar."
    invite.version != version -> "Este código fue sustituido. Genera uno nuevo."
    invite.status != "ACTIVE" -> "Este código ya fue utilizado o revocado."
    invite.expiresAt == null -> "No se pudo consultar la vigencia. Genera un código nuevo."
    !canCopy() && !loading -> "Este código ha expirado. Genera uno nuevo."
    else -> "Vence: " + invite.expiresAt?.toDate()?.toInstant()?.atZone(clock.zone)
      ?.format(DateTimeFormatter.ofPattern("d MMM yyyy · HH:mm", Locale.forLanguageTag("es-ES")))
  }

  LunaFlowDialog(if (paired) "Tu vínculo está activo" else "Invitar, sin compartir\nde más.",
    if (paired) "Revisa los permisos o finaliza este vínculo antes de invitar a otra persona."
    else "Tu pareja necesita este código para vincular su cuenta.", onDismiss, "invite_details_dialog", busy = loading,
    actions = {
      (copyError ?: error)?.let { Text(it, style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("invite_error")) }
      LunaButton("Volver a nuestro espacio", onDismiss, enabled = !loading)
    }) {
    Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.lunaHero,
      contentColor = MaterialTheme.colorScheme.lunaOnHero) {
      Column(Modifier.fillMaxWidth().padding(24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (guest) "TU CÓDIGO DE DEMOSTRACIÓN" else if (paired) "TU VÍNCULO" else "TU CÓDIGO DE INVITACIÓN",
          style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.lunaHeroMuted)
        Text(if (paired) "Vínculo activo" else if (loading) "Generando…" else invite?.inviteCode ?: "Sin código activo",
          style = MaterialTheme.typography.headlineMedium)
        Text(validity, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.lunaHeroMuted)
      }
    }
    LunaButton("Copiar código", {
      val code = invite?.inviteCode
      if (canCopy() && !code.isNullOrBlank()) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("LunaSync", code))
        Toast.makeText(context, "Código copiado", Toast.LENGTH_SHORT).show()
      } else copyError = "El código ya no está vigente. Genera uno nuevo."
    }, Modifier.testTag("copy_pairing_code_button"), type = LunaButtonType.SECONDARY, enabled = canCopy())
    if (!paired) LunaButton(if (invite == null) "Generar código" else "Generar nuevo código",
      { copyError = null; onGenerate() }, Modifier.testTag("regenerate_pairing_code_button"), loading = loading,
      loadingText = "Generando…")
    Text("Lo que ocurre después", style = MaterialTheme.typography.titleLarge)
    Text("Una vez que se vincule, solo verá la información autorizada. Tus notas privadas nunca se comparten.",
      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LunaNavigationRow("Revisar permisos antes de invitar", "Tú eliges qué compartir.", onPrivacy,
      icon = LunaIcons.Shield, enabled = !loading)
    if (!paired) Text("Si generas un código nuevo, sustituirá al anterior.",
      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}

@Composable
internal fun UnlinkPairingDialog(role: UserRole, onConfirm: ((Boolean, String?) -> Unit) -> Unit,
  onDismiss: () -> Unit) {
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val owner = role == UserRole.PRIMARY_ELLA
  LunaFlowDialog(if (owner) "¿Desvincular\na tu pareja?" else "¿Finalizar\nel vínculo?",
    if (owner) "Tu historial permanece en tu cuenta." else "El historial de tu pareja permanece en su cuenta.",
    onDismiss, "unlink_pairing_dialog", busy = busy, actions = {
      error?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error,
        modifier = Modifier.testTag("unlink_error")) }
      LunaButton(if (owner) "Sí, desvincular" else "Sí, finalizar el vínculo", {
        if (!busy) {
          busy = true
          error = null
          onConfirm { success, failure ->
            busy = false
            if (success) onDismiss()
            else error = failure ?: "No se pudo finalizar el vínculo. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("confirm_unlink_button"), type = LunaButtonType.DANGER, loading = busy,
        loadingText = "Finalizando vínculo…")
    }) {
    Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.errorContainer,
      contentColor = MaterialTheme.colorScheme.onBackground) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(LunaIcons.ConfirmationShield, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text("Se detiene el acceso compartido", style = MaterialTheme.typography.titleMedium)
        Text(if (owner) "Tu pareja dejará de acceder a este espacio. Esto no puede borrar información que ya haya visto o guardado."
          else "Dejarás de acceder al espacio compartido. El historial de tu pareja permanece en su cuenta.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Text(if (owner) "Puedes volver a invitarla más adelante con una nueva vinculación."
      else "Podrás volver a vincularte si tu pareja te envía una nueva invitación.", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    LunaButton("Mantener el vínculo", onDismiss, Modifier.testTag("cancel_unlink_button"), type = LunaButtonType.SECONDARY,
      enabled = !busy)
  }
}
