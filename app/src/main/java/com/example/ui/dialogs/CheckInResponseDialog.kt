package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckIn
import com.example.data.model.QuickResponseOption
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaOptionChip
import com.example.ui.theme.LunaBorderLight
import com.example.ui.theme.LunaDarkBorder
import com.example.ui.theme.LunaDarkSurface
import com.example.ui.theme.LunaIceBlueAccent
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageText
import com.example.ui.theme.LunaSurfaceLight
import com.example.ui.theme.StatusError

/**
 * Diálogo de respuesta a un mensaje o check-in de la pareja (Figma: reply · Un mensaje de Dani #8:26)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckInResponseDialog(
  checkIn: CheckIn,
  onSendResponse: (QuickResponseOption, String?) -> Unit,
  onDismiss: () -> Unit
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  var selectedOption by remember { mutableStateOf<QuickResponseOption?>(null) }
  var customNote by remember { mutableStateOf("") }
  var isSending by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .testTag("checkin_response_dialog")
  ) {
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = if (isDark) LunaDarkSurface else LunaSurfaceLight,
      border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .padding(20.dp)
          .verticalScroll(rememberScrollState())
      ) {
        // 1. Contexto superior "Volver"
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clickable(onClick = onDismiss)
            .padding(bottom = 8.dp)
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Volver",
            tint = LunaNavyPrimary,
            modifier = Modifier.size(18.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Volver",
            style = MaterialTheme.typography.bodySmall,
            color = LunaNavyPrimary
          )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 2. Título
        Text(
          text = "Un mensaje de tu pareja",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(4.dp))

        // 3. Tarjeta de la pregunta recibida
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isDark) LunaDarkSurface else LunaSageSoft,
          border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaSageSoft),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            LunaBadge(
              text = "Pregunta de apoyo",
              backgroundColor = LunaSageText.copy(alpha = 0.15f),
              textColor = LunaSageText
            )
            Spacer(modifier = Modifier.height(8.dp))
            val promptQuestion = when (checkIn.promptType) {
              "HOW_ARE_YOU_FEELING" -> "¿Cómo te sientes hoy?"
              "NEED_COMPANY_OR_SPACE" -> "¿Prefieres compañía o un rato para ti?"
              "CAN_I_BRING_YOU_SOMETHING" -> "¿Puedo prepararte algo o ayudarte con una tarea?"
              "TOUCH_BASE" -> "Solo quería recordarte que aquí estoy para ti."
              else -> checkIn.customNote ?: "¿Cómo te sientes hoy?"
            }
            Text(
              text = "“$promptQuestion”",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            if (!checkIn.customNote.isNullOrBlank() && checkIn.promptType != "TOUCH_BASE") {
              Spacer(modifier = Modifier.height(4.dp))
              Text(
                text = "Nota: ${checkIn.customNote}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 4. Opciones de respuesta rápida
        Text(
          text = "¿Qué te gustaría responder?",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        val options = listOf(
          Pair(QuickResponseOption.NEED_SPACE, "Un rato para mí"),
          Pair(QuickResponseOption.NEED_REST, "Descansar en silencio"),
          Pair(QuickResponseOption.BRING_SNACK, "Una infusión o comida rica"),
          Pair(QuickResponseOption.FEELING_GOOD, "Me siento con energía"),
          Pair(QuickResponseOption.JUST_HUG, "Un abrazo y compañía")
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          options.forEach { (opt, label) ->
            val isSelected = selectedOption == opt
            LunaOptionChip(
              text = label,
              selected = isSelected,
              onSelect = { selectedOption = opt },
              modifier = Modifier.fillMaxWidth().testTag("reply_option_${opt.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 5. Nota opcional
        Text(
          text = "Mensaje opcional",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(6.dp))

        OutlinedTextField(
          value = customNote,
          onValueChange = { customNote = it },
          placeholder = { Text("Escribe una respuesta corta si lo deseas...") },
          singleLine = false,
          maxLines = 3,
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LunaNavyPrimary,
            unfocusedBorderColor = if (isDark) LunaDarkBorder else LunaBorderLight
          ),
          modifier = Modifier.fillMaxWidth().testTag("reply_note_input")
        )

        if (errorMessage != null) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = errorMessage!!,
            style = MaterialTheme.typography.bodySmall,
            color = StatusError
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // 6. Botón Enviar respuesta
        LunaButton(
          text = "Enviar respuesta",
          onClick = {
            val opt = selectedOption ?: QuickResponseOption.NEED_REST
            isSending = true
            onSendResponse(opt, customNote.ifBlank { null })
            onDismiss()
          },
          loading = isSending,
          enabled = !isSending,
          type = LunaButtonType.PRIMARY,
          modifier = Modifier.testTag("submit_reply_button")
        )
      }
    }
  }
}
