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
import com.example.data.model.PerceivedMood
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerObservationDialog(
  onDismiss: () -> Unit,
  onSave: (perceivedMood: PerceivedMood?, apparentPainLevel: Int?, unloggedSpottingReported: Boolean,
    careActionTaken: String?, sweetMessage: String?, onComplete: (Boolean, String?) -> Unit) -> Unit,
  date: LocalDate = LocalDate.now()
) {
  var perceivedMood by remember { mutableStateOf<PerceivedMood?>(null) }
  var pain by remember { mutableStateOf<Int?>(null) }
  var unloggedSpotting by remember { mutableStateOf(false) }
  var careAction by remember { mutableStateOf("") }
  var sweetMessage by remember { mutableStateOf("") }
  var isSubmitting by remember { mutableStateOf(false) }
  var dialogError by remember { mutableStateOf<String?>(null) }
  var showDetails by remember { mutableStateOf(false) }
  val hasContent = careAction.isNotBlank() || sweetMessage.isNotBlank() || unloggedSpotting || perceivedMood != null || pain != null
  val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", Locale.forLanguageTag("es-ES")))

  LunaFlowDialog("Un gesto de cuidado", dateLabel, onDismiss, "partner_observation_dialog", busy = isSubmitting,
    actions = {
      dialogError?.let { Text(it, color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall, modifier = Modifier.testTag("observation_error")) }
      LunaButton("Guardar observación", {
        if (!isSubmitting && hasContent) {
          isSubmitting = true
          dialogError = null
          onSave(perceivedMood, pain, unloggedSpotting, careAction.trim().takeIf { it.isNotEmpty() },
            sweetMessage.trim().takeIf { it.isNotEmpty() }) { success, error ->
            isSubmitting = false
            if (success) onDismiss()
            else dialogError = error ?: "No se pudo guardar la observación. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("save_observation_button"), enabled = hasContent, loading = isSubmitting)
    }) {
    LunaFormField("¿Qué hiciste?", careAction, { careAction = it }, Modifier.testTag("observation_care_action_input"),
      enabled = !isSubmitting, placeholder = "Un gesto de cuidado que hayas hecho.")
    LunaFormField("Mensaje para tu pareja", sweetMessage, { sweetMessage = it }, Modifier.testTag("observation_sweet_message_input"),
      enabled = !isSubmitting, placeholder = "Hoy puedes ir a tu ritmo.")
    Text("Si ella te lo comentó", style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LunaOptionChip("No indicó", !unloggedSpotting, { unloggedSpotting = false }, Modifier.weight(1f), enabled = !isSubmitting)
      LunaOptionChip("Mencionó manchado", unloggedSpotting, { unloggedSpotting = true },
        Modifier.weight(1f).testTag("spotting_switch"), enabled = !isSubmitting)
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(shape = RoundedCornerShape(16.dp), color = if (dark) LunaSandDarkSoft else LunaSandSoft,
      contentColor = MaterialTheme.colorScheme.onBackground) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.luna_observation_shield), null, Modifier.size(22.dp),
          tint = MaterialTheme.colorScheme.primary)
        Text("Es una observación tuya", style = MaterialTheme.typography.titleMedium)
        Text("No modifica su menstruación ni confirma síntomas. Ella decide si añade algo a su registro.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    TextButton({ showDetails = !showDetails }, enabled = !isSubmitting, modifier = Modifier.testTag("observation_details_button")) {
      Text(if (showDetails) "Ocultar detalles de tu percepción" else "Añadir detalles de tu percepción")
    }
    if (showDetails) {
      Text("Tu percepción es opcional; no sustituye lo que ella registra.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
      Text("Ánimo que percibes", style = MaterialTheme.typography.titleMedium)
      FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        LunaOptionChip("Sin indicar", perceivedMood == null, { perceivedMood = null }, enabled = !isSubmitting)
        PerceivedMood.entries.forEach { mood ->
          LunaOptionChip(when (mood) {
            PerceivedMood.CALM -> "Tranquila"
            PerceivedMood.TIRED -> "Cansada"
            PerceivedMood.SAD -> "Sensible"
            PerceivedMood.IRRITABLE -> "Irritable"
            PerceivedMood.LOVING -> "Cariñosa"
          }, perceivedMood == mood, { perceivedMood = mood }, Modifier.testTag("mood_chip_${mood.name}"), enabled = !isSubmitting)
        }
      }
      Text("Molestia percibida", style = MaterialTheme.typography.titleMedium)
      FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        LunaOptionChip("Sin indicar", pain == null, { pain = null }, enabled = !isSubmitting)
        (0..5).forEach { level ->
          LunaOptionChip("$level/5", pain == level, { pain = level },
            Modifier.testTag("partner_pain_$level"), enabled = !isSubmitting)
        }
      }
    }
  }
}
