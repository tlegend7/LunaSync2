package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CheckInPromptType
import com.example.ui.components.*

@Composable
fun SendCheckInDialog(
  onDismiss: () -> Unit,
  onSend: (promptType: CheckInPromptType, customNote: String?, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
  var selectedPrompt by remember { mutableStateOf(CheckInPromptType.HOW_ARE_YOU_FEELING) }
  var customNote by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var dialogError by remember { mutableStateOf<String?>(null) }
  var showMore by remember { mutableStateOf(false) }
  val primaryQuestions = listOf(CheckInPromptType.HOW_ARE_YOU_FEELING,
    CheckInPromptType.NEED_SPACE_OR_CUDDLES, CheckInPromptType.CAN_I_BRING_YOU_SOMETHING)

  LunaFlowDialog("Una pregunta,\nsin presión.", "Un check-in para acompañar a tu pareja.",
    onDismiss, "send_checkin_dialog", busy = isSubmitting,
    actions = {
      dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("checkin_error")) }
      LunaButton("Enviar pregunta", {
        if (!isSubmitting) {
          isSubmitting = true
          dialogError = null
          onSend(selectedPrompt, customNote.trim().takeIf { it.isNotEmpty() }) { success, error ->
            isSubmitting = false
            if (success) onDismiss()
            else dialogError = error ?: "No se pudo enviar la pregunta. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("send_check_in_button"), loading = isSubmitting, loadingText = "Enviando…")
    }) {
    Text("Elige una pregunta", style = MaterialTheme.typography.titleLarge)
    Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().selectable(true, enabled = !isSubmitting,
          role = Role.RadioButton, onClick = {}).testTag("prompt_${selectedPrompt.name}")) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(painterResource(R.drawable.luna_message_circle), null, Modifier.size(22.dp),
            tint = MaterialTheme.colorScheme.primary)
          Text(checkInQuestion(selectedPrompt.name), style = MaterialTheme.typography.titleMedium)
          Text("Pregunta seleccionada", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      val questions = if (showMore) CheckInPromptType.entries else primaryQuestions
      questions.filter { it != selectedPrompt }.forEach { prompt ->
        Row(Modifier.fillMaxWidth().heightIn(min = 48.dp).selectable(false, enabled = !isSubmitting,
          role = Role.RadioButton, onClick = { selectedPrompt = prompt }).testTag("prompt_${prompt.name}"),
          verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
          Icon(painterResource(R.drawable.luna_heart), null, Modifier.size(22.dp))
          Text(when (prompt) {
            CheckInPromptType.NEED_SPACE_OR_CUDDLES -> "¿Compañía o espacio?"
            CheckInPromptType.CAN_I_BRING_YOU_SOMETHING -> "¿Puedo ayudarte con algo?"
            else -> checkInQuestion(prompt.name)
          }, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
          Icon(painterResource(R.drawable.luna_chevron_right), null, Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
      }
    }
    LunaFormField("Añade una nota, si quieres", customNote, { customNote = it },
      Modifier.testTag("checkin_custom_note_input"), enabled = !isSubmitting, placeholder = "Estoy aquí si necesitas algo.")
    Text("Responder es opcional. Dale espacio para hacerlo a su ritmo.", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton({ showMore = !showMore }, enabled = !isSubmitting, modifier = Modifier.testTag("more_checkin_questions")) {
      Text(if (showMore) "Ver menos preguntas" else "Más preguntas")
    }
  }
}
