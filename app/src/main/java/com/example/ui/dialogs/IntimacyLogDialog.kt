package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.EncounterType
import com.example.data.model.PostContactEffect
import com.example.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntimacyLogDialog(
  date: LocalDate = LocalDate.now(),
  onDismiss: () -> Unit,
  onSave: (encounterType: EncounterType, usedLubricant: Boolean, hadOrgasm: Boolean,
    postContactEffect: PostContactEffect, notes: String?, isPrivate: Boolean,
    onComplete: (Boolean, String?) -> Unit) -> Unit
) {
  var encounterType by remember { mutableStateOf(EncounterType.PROTECTED) }
  var usedLubricant by remember { mutableStateOf(false) }
  var hadOrgasm by remember { mutableStateOf(false) }
  var effect by remember { mutableStateOf(PostContactEffect.NONE) }
  var notes by remember { mutableStateOf("") }
  var isPrivate by remember { mutableStateOf(true) }
  var showDetails by remember { mutableStateOf(false) }
  var isSubmitting by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  val locale = Locale.forLanguageTag("es-ES")
  val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", locale))
    .replaceFirstChar { it.titlecase(locale) }

  LunaFlowDialog("Un registro íntimo", dateLabel, onDismiss, "intimacy_log_dialog", busy = isSubmitting,
    subtitleModifier = Modifier.testTag("intimacy_dialog_date_text"),
    actions = {
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("intimacy_error")) }
      LunaButton("Guardar registro", {
        if (!isSubmitting) {
          isSubmitting = true
          error = null
          onSave(encounterType, usedLubricant, hadOrgasm, effect, notes.trim().takeIf { it.isNotEmpty() }, isPrivate) { success, failure ->
            isSubmitting = false
            if (success) onDismiss()
            else error = failure ?: "No se pudo guardar el registro. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("save_intimacy_button"), loading = isSubmitting)
    }) {
    LunaBadge(if (isPrivate) "Solo tú" else "Según tus permisos", style = LunaBadgeStyle.PRIVATE)
    Text("Tipo de encuentro", style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LunaOptionChip("Protegido", encounterType == EncounterType.PROTECTED, { encounterType = EncounterType.PROTECTED },
        Modifier.weight(1f).testTag("chip_protected"), enabled = !isSubmitting)
      LunaOptionChip("Sin protección", encounterType == EncounterType.UNPROTECTED, { encounterType = EncounterType.UNPROTECTED },
        Modifier.weight(1f).testTag("chip_unprotected"), enabled = !isSubmitting)
    }
    if (encounterType == EncounterType.OUTERCOURSE) Text("Contacto sin penetración", style = MaterialTheme.typography.bodySmall)
    Text("Después del encuentro", style = MaterialTheme.typography.titleLarge)
    Row(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      LunaOptionChip("Sin cambios", effect == PostContactEffect.NONE, { effect = PostContactEffect.NONE },
        Modifier.weight(1f).testTag("effect_chip_NONE"), enabled = !isSubmitting)
      LunaOptionChip("Sensibilidad", effect == PostContactEffect.FRICTION_SENSITIVITY, { effect = PostContactEffect.FRICTION_SENSITIVITY },
        Modifier.weight(1f).testTag("effect_chip_FRICTION_SENSITIVITY"), enabled = !isSubmitting)
    }
    if (effect !in listOf(PostContactEffect.NONE, PostContactEffect.FRICTION_SENSITIVITY)) {
      Text(effectLabel(effect), style = MaterialTheme.typography.bodySmall)
    }
    LunaFormField("Nota opcional", notes, { notes = it }, Modifier.testTag("intimacy_notes_input"),
      enabled = !isSubmitting, placeholder = "Añadir algo que quieras recordar")
    IntimacyToggle("Mantener privado", if (isPrivate) "No se mostrará a tu pareja."
      else "Solo se comparte si activas también el permiso de intimidad.", isPrivate, { isPrivate = it },
      "intimacy_privacy_switch", !isSubmitting)
    Text("Compartir un registro es una decisión explícita.", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton({ showDetails = !showDetails }, enabled = !isSubmitting, modifier = Modifier.testTag("intimacy_details_button")) {
      Text(if (showDetails) "Ocultar detalles del encuentro" else "Más detalles del encuentro")
    }
    if (showDetails) {
      LunaOptionChip("Contacto sin penetración", encounterType == EncounterType.OUTERCOURSE,
        { encounterType = EncounterType.OUTERCOURSE }, Modifier.fillMaxWidth().testTag("chip_outercourse"), enabled = !isSubmitting)
      IntimacyToggle("Uso de lubricante", "Opcional", usedLubricant, { usedLubricant = it }, "intimacy_lubricant_switch", !isSubmitting)
      IntimacyToggle("Orgasmo", "Opcional", hadOrgasm, { hadOrgasm = it }, "intimacy_orgasm_switch", !isSubmitting)
      Text("Otros efectos que notaste", style = MaterialTheme.typography.titleMedium)
      FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        PostContactEffect.entries.filter { it !in listOf(PostContactEffect.NONE, PostContactEffect.FRICTION_SENSITIVITY) }.forEach { value ->
          LunaOptionChip(effectLabel(value), effect == value, { effect = value },
            Modifier.testTag("effect_chip_${value.name}"), enabled = !isSubmitting)
        }
      }
      Text("Este registro no confirma sangrado ni cambia las fechas de tu menstruación.",
        style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
}

private fun effectLabel(effect: PostContactEffect): String = when (effect) {
  PostContactEffect.NONE -> "Sin cambios"
  PostContactEffect.FRICTION_SENSITIVITY -> "Sensibilidad"
  PostContactEffect.AROUSAL_FLUID -> "Mayor humedad"
  PostContactEffect.CONTACT_SPOTTING -> "Manchado tras contacto"
  PostContactEffect.PH_SHIFT -> "Cambio de pH registrado"
}

@Composable
private fun IntimacyToggle(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit,
  tag: String, enabled: Boolean) {
  Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LunaToggle(checked, onChange, Modifier.testTag(tag), enabled, title)
  }
}
