package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckInPromptType
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusErrorSubtle

@Composable
fun SendCheckInDialog(
  onDismiss: () -> Unit,
  onSend: (promptType: CheckInPromptType, customNote: String?, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
  var selectedPrompt by remember { mutableStateOf(CheckInPromptType.HOW_ARE_YOU_FEELING) }
  var customNote by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var dialogError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = "Enviar Check-in Afectivo",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )
    },
    text = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .verticalScroll(rememberScrollState())
          .padding(vertical = 4.dp)
      ) {
        if (dialogError != null) {
          Surface(
            color = StatusErrorSubtle,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp)
              .border(0.5.dp, StatusError.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
          ) {
            Text(
              text = dialogError ?: "",
              color = StatusError,
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              modifier = Modifier.padding(12.dp)
            )
          }
        }

        Text(
          text = "Selecciona una pregunta cariñosa:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          CheckInPromptType.entries.forEach { prompt ->
            val title = when (prompt) {
              CheckInPromptType.HOW_ARE_YOU_FEELING -> "¿Cómo te sientes en este momento?"
              CheckInPromptType.NEED_SPACE_OR_CUDDLES -> "¿Necesitas espacio a solas o compañía?"
              CheckInPromptType.ARE_YOU_UPSET_WITH_ME -> "¿Hay algo en lo que te pueda ayudar o prefieres un momento?"
              CheckInPromptType.CAN_I_BRING_YOU_SOMETHING -> "¿Puedo llevarte algo rico o caliente?"
            }

            FilterChip(
              selected = selectedPrompt == prompt,
              onClick = { selectedPrompt = prompt },
              label = { Text(title, fontSize = 13.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandTerracottaRose.copy(alpha = 0.15f),
                selectedLabelColor = BrandTerracottaRose
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = selectedPrompt == prompt,
                borderColor = AppleHairlineBorder,
                selectedBorderColor = BrandTerracottaRose
              ),
              modifier = Modifier.fillMaxWidth().testTag("prompt_${prompt.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        OutlinedTextField(
          value = customNote,
          onValueChange = { customNote = it },
          label = { Text("Nota personalizada (opcional)") },
          placeholder = { Text("Ej. Te pienso mucho hoy, avísame si necesitas algo...") },
          modifier = Modifier.fillMaxWidth().testTag("checkin_custom_note_input"),
          shape = RoundedCornerShape(14.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (!isSubmitting) {
            isSubmitting = true
            dialogError = null
            onSend(selectedPrompt, customNote.ifBlank { null }) { success, err ->
              isSubmitting = false
              if (success) {
                onDismiss()
              } else {
                dialogError = err ?: "Error al enviar el check-in. Intenta nuevamente."
              }
            }
          }
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
        modifier = Modifier
          .heightIn(min = 48.dp)
          .testTag("send_check_in_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(
            modifier = Modifier.size(16.dp),
            color = Color.White,
            strokeWidth = 2.dp
          )
        } else {
          Text("Enviar Pregunta", fontWeight = FontWeight.SemiBold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSubmitting) {
        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  )
}
