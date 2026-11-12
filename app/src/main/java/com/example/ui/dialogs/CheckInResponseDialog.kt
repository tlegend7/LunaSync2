package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CheckIn
import com.example.data.model.CheckInPromptType
import com.example.data.model.QuickResponseOption
import com.example.ui.components.*
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageDarkSoft
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun CheckInResponseDialog(
  checkIn: CheckIn,
  onSendResponse: (QuickResponseOption, String?, (Boolean, String?) -> Unit) -> Unit,
  onDismiss: () -> Unit
) {
  var selectedOption by remember(checkIn.id) { mutableStateOf<QuickResponseOption?>(null) }
  var customNote by remember(checkIn.id) { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val needsSpaceChoice = checkIn.promptType == CheckInPromptType.NEED_SPACE_OR_CUDDLES.name
  val options = checkInResponseOptions(checkIn.promptType)

  LunaFlowDialog("Un mensaje de tu pareja", "Responder es opcional. Puedes hacerlo a tu ritmo.",
    onDismiss, "checkin_response_dialog", busy = isSending,
    actions = {
      errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("reply_error")) }
      LunaButton("Enviar respuesta", {
        selectedOption?.let { option ->
          if (!isSending) {
            isSending = true
            errorMessage = null
            onSendResponse(option, customNote.trim().takeIf { it.isNotEmpty() }) { success, error ->
              isSending = false
              if (success) onDismiss()
              else errorMessage = error ?: "No se pudo enviar la respuesta. Intenta nuevamente."
            }
          }
        }
      }, Modifier.testTag("submit_reply_button"), enabled = selectedOption != null, loading = isSending, loadingText = "Enviando…")
    }) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(shape = RoundedCornerShape(16.dp), color = if (dark) LunaSageDarkSoft else LunaSageSoft,
      contentColor = MaterialTheme.colorScheme.onBackground) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.luna_message_circle), null, Modifier.size(22.dp),
          tint = MaterialTheme.colorScheme.primary)
        Text(checkInQuestion(checkIn.promptType), style = MaterialTheme.typography.titleMedium)
        checkIn.customNote?.takeIf { it.isNotBlank() }?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
        val sentAt = checkIn.createdAt?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
          ?.format(DateTimeFormatter.ofPattern("d MMM · HH:mm", Locale.forLanguageTag("es-ES")))
        Text(if (sentAt == null) "Pregunta de apoyo" else "Tu pareja · $sentAt",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    Text("Lo que te viene bien", style = MaterialTheme.typography.titleLarge)
    if (needsSpaceChoice) Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      options.forEach { (option, label) ->
        LunaOptionChip(label, selectedOption == option, { selectedOption = option },
          Modifier.weight(1f).testTag("reply_option_${option.name}"), enabled = !isSending)
      }
    } else Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      options.forEach { (option, label) ->
        LunaOptionChip(label, selectedOption == option, { selectedOption = option },
          Modifier.fillMaxWidth().testTag("reply_option_${option.name}"), enabled = !isSending)
      }
    }
    Text("Puedes añadir una nota", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(customNote, { customNote = it }, enabled = !isSending,
      placeholder = { Text("Escribe una respuesta si lo deseas…") },
      minLines = 1, maxLines = 5, shape = RoundedCornerShape(12.dp),
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
      modifier = Modifier.fillMaxWidth().testTag("reply_note_input"))
    Text("Esto comparte tu respuesta, no tus notas privadas.", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
  }
}
