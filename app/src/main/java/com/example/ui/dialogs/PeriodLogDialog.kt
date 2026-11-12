package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.CycleEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

@Composable
fun PeriodLogDialog(
  initialDate: LocalDate = LocalDate.now(),
  existingCycle: CycleEntry? = null,
  today: LocalDate = LocalDate.now(),
  onDismiss: () -> Unit,
  onSave: (startDate: LocalDate, endDate: LocalDate?, onComplete: (Boolean, String?) -> Unit) -> Unit,
  onDelete: ((onComplete: (Boolean, String?) -> Unit) -> Unit)? = null
) {
  var startDate by remember { mutableStateOf(existingCycle?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: initialDate) }
  var hasFinished by remember { mutableStateOf(existingCycle?.endDate != null) }
  var endDate by remember { mutableStateOf(existingCycle?.endDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: today) }
  var saving by remember { mutableStateOf(false) }
  var deleting by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var deleteError by remember { mutableStateOf<String?>(null) }
  var showDelete by remember { mutableStateOf(false) }
  var pickStart by remember { mutableStateOf(false) }
  var pickEnd by remember { mutableStateOf(false) }
  val busy = saving || deleting
  val formatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale.forLanguageTag("es-ES")) }
  val days = ChronoUnit.DAYS.between(startDate, endDate) + 1

  LunaFlowDialog("Tu menstruación", "Registra lo que ocurrió. Las estimaciones no crean registros.",
    onDismiss, "period_log_dialog", busy = busy, actions = {
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("period_error")) }
      LunaButton("Guardar fechas", {
        error = when {
          startDate.isAfter(today) -> "La fecha de inicio no puede ser una fecha futura."
          hasFinished && endDate.isAfter(today) -> "La fecha de fin no puede ser una fecha futura."
          hasFinished && endDate.isBefore(startDate) -> "La fecha de fin no puede ser anterior a la fecha de inicio."
          else -> null
        }
        if (!busy && error == null) {
          saving = true
          onSave(startDate, if (hasFinished) endDate else null) { success, failure ->
            saving = false
            if (success) onDismiss() else error = failure ?: "No se pudieron guardar las fechas. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("save_period_button"), enabled = !deleting, loading = saving)
    }) {
    PeriodDateField("Primer día de sangrado", startDate.format(formatter), "period_dialog_start_date_text",
      "pick_start_date_button", !busy, { pickStart = true })
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)) {
      Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("El sangrado terminó", style = MaterialTheme.typography.titleMedium)
        Text("Activa para confirmar la fecha final.", style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      LunaToggle(hasFinished, { hasFinished = it }, Modifier.testTag("period_finished_switch"), !busy, "El sangrado terminó")
    }
    if (hasFinished) {
      PeriodDateField("Último día de sangrado", endDate.format(formatter), "period_dialog_end_date_text",
        "pick_end_date_button", !busy, { pickEnd = true })
    }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    Surface(shape = RoundedCornerShape(16.dp), color = if (dark) LunaCoralDarkSoft else LunaCoralSoft,
      contentColor = MaterialTheme.colorScheme.onBackground) {
      Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(painterResource(R.drawable.luna_period_droplets), null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
        Text(when {
          !hasFinished -> "Periodo en curso"
          days == 1L -> "1 día registrado"
          days > 1 -> "$days días registrados"
          else -> "Revisa las fechas"
        }, style = MaterialTheme.typography.titleMedium)
        Text(if (hasFinished) "La duración corresponde a las fechas que confirmas."
          else "Confirma el último día cuando termine.", style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
    if (existingCycle != null && onDelete != null) {
      LunaButton("Eliminar este registro", { showDelete = true; deleteError = null }, Modifier.testTag("delete_period_button"),
        type = LunaButtonType.DANGER, enabled = !busy)
      Text("Eliminar siempre requiere confirmación.", style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
  }
  if (pickStart) PeriodDatePicker(startDate, today, onDismiss = { pickStart = false }) { picked ->
    startDate = picked
    if (endDate.isBefore(picked)) endDate = picked
    pickStart = false
  }
  if (pickEnd) PeriodDatePicker(endDate, today, minimum = startDate, onDismiss = { pickEnd = false }) { picked ->
    endDate = picked
    pickEnd = false
  }
  if (showDelete && onDelete != null) {
    Dialog(onDismissRequest = { if (!deleting) showDelete = false },
      properties = DialogProperties(dismissOnBackPress = !deleting, dismissOnClickOutside = !deleting, usePlatformDefaultWidth = false)) {
      Surface(Modifier.padding(24.dp).widthIn(max = 393.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Text("¿Eliminar este registro?", style = MaterialTheme.typography.titleLarge)
          // Describe the persisted record, even if its unsaved draft dates have been changed.
          val storedStart = existingCycle?.startDate?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: initialDate
          Text("Se quitará la menstruación registrada desde ${storedStart.format(formatter)} y se actualizarán las estimaciones relacionadas.",
            style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
          deleteError?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.testTag("period_delete_error")) }
          LunaButton("Eliminar registro", {
            if (!deleting) {
              deleting = true
              deleteError = null
              onDelete { success, failure ->
                deleting = false
                if (success) onDismiss()
                else deleteError = failure ?: "No se pudo eliminar el registro. Intenta nuevamente."
              }
            }
          }, Modifier.testTag("confirm_delete_period_button"), type = LunaButtonType.DANGER,
            loading = deleting, loadingText = "Eliminando…")
          LunaButton("Conservar registro", { showDelete = false }, Modifier.testTag("cancel_delete_period_button"),
            type = LunaButtonType.SECONDARY, enabled = !deleting)
        }
      }
    }
  }
}

@Composable
private fun PeriodDateField(label: String, value: String, valueTag: String, buttonTag: String,
  enabled: Boolean, onClick: () -> Unit) {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Surface(shape = RoundedCornerShape(12.dp), color = MaterialTheme.colorScheme.surface,
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)) {
      Box(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(enabled, role = Role.Button, onClick = onClick)
        .testTag(buttonTag).padding(16.dp)) {
        Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag(valueTag))
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PeriodDatePicker(current: LocalDate, today: LocalDate, minimum: LocalDate? = null,
  onDismiss: () -> Unit, onPicked: (LocalDate) -> Unit) {
  val state = rememberDatePickerState(initialSelectedDateMillis = current.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    selectableDates = object : SelectableDates {
      override fun isSelectableDate(utcTimeMillis: Long): Boolean {
        val date = Instant.ofEpochMilli(utcTimeMillis).atZone(ZoneOffset.UTC).toLocalDate()
        return !date.isAfter(today) && (minimum == null || !date.isBefore(minimum))
      }
    })
  DatePickerDialog(onDismissRequest = onDismiss, confirmButton = {
    TextButton({ state.selectedDateMillis?.let { onPicked(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
      enabled = state.selectedDateMillis != null) { Text("Aceptar") }
  }, dismissButton = { TextButton(onDismiss) { Text("Cancelar") } }) {
    DatePicker(state, title = { Text("Elige una fecha", modifier = Modifier.padding(24.dp)) })
  }
}
