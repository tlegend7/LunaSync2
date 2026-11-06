package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckInPromptType
import com.example.ui.theme.DeepOceanBlue
import com.example.ui.theme.SoftCerulean

@Composable
fun SendCheckInDialog(
  onDismiss: () -> Unit,
  onSend: (promptType: CheckInPromptType, customNote: String?) -> Unit
) {
  var selectedPrompt by remember { mutableStateOf(CheckInPromptType.HOW_ARE_YOU_FEELING) }
  var customNote by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(28.dp),
    title = {
      Text(
        text = "Enviar Check-in Afectivo",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = DeepOceanBlue
      )
    },
    text = {
      Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
          text = "Selecciona una pregunta cariñosa:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          CheckInPromptType.entries.forEach { prompt ->
            val title = when (prompt) {
              CheckInPromptType.HOW_ARE_YOU_FEELING -> "¿Cómo te sientes en este momento?"
              CheckInPromptType.NEED_SPACE_OR_CUDDLES -> "¿Necesitas espacio a solas o mimos?"
              CheckInPromptType.ARE_YOU_UPSET_WITH_ME -> "¿Estás molesta conmigo o es bajón hormonal?"
              CheckInPromptType.CAN_I_BRING_YOU_SOMETHING -> "¿Puedo llevarte algo rico o caliente?"
            }

            FilterChip(
              selected = selectedPrompt == prompt,
              onClick = { selectedPrompt = prompt },
              label = { Text(title, fontSize = 13.sp) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = SoftCerulean.copy(alpha = 0.35f)
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
          placeholder = { Text("Ej. Te pienso mucho hoy, avísame...") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSend(selectedPrompt, customNote.ifBlank { null })
          onDismiss()
        },
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = DeepOceanBlue),
        modifier = Modifier.testTag("send_check_in_button")
      ) {
        Text("Enviar Pregunta")
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar")
      }
    }
  )
}
