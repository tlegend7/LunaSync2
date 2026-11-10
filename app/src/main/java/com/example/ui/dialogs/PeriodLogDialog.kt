package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import com.example.data.model.CycleEntry
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseMenstrualRose
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PeriodLogDialog(
  initialDate: LocalDate = LocalDate.now(),
  existingCycle: CycleEntry? = null,
  today: LocalDate = LocalDate.now(),
  onDismiss: () -> Unit,
  onSave: (
    startDate: LocalDate,
    endDate: LocalDate?,
    onComplete: (Boolean, String?) -> Unit
  ) -> Unit,
  onDelete: ((onComplete: (Boolean, String?) -> Unit) -> Unit)? = null
) {
  var startDate by remember {
    mutableStateOf(
      existingCycle?.let {
        try { LocalDate.parse(it.startDate) } catch (e: Exception) { initialDate }
      } ?: initialDate
    )
  }

  var hasFinishedBleeding by remember {
    mutableStateOf(existingCycle?.endDate != null)
  }

  var endDate by remember {
    mutableStateOf(
      existingCycle?.endDate?.let {
        try { LocalDate.parse(it) } catch (e: Exception) { startDate.plusDays(4) }
      } ?: startDate.plusDays(4).coerceAtMost(today)
    )
  }

  var isSubmitting by remember { mutableStateOf(false) }
  var dialogError by remember { mutableStateOf<String?>(null) }
  var showDeleteConfirm by remember { mutableStateOf(false) }

  // Date picker dialog states
  var showStartDatePicker by remember { mutableStateOf(false) }
  var showEndDatePicker by remember { mutableStateOf(false) }

  val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM yyyy", Locale("es", "ES")) }

  if (showStartDatePicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = startDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    DatePickerDialog(
      onDismissRequest = { showStartDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { millis ->
            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
            startDate = picked
            if (hasFinishedBleeding && endDate.isBefore(picked)) {
              endDate = picked.plusDays(4).coerceAtMost(today)
            }
          }
          showStartDatePicker = false
        }) {
          Text("Aceptar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showStartDatePicker = false }) {
          Text("Cancelar")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showEndDatePicker) {
    val datePickerState = rememberDatePickerState(
      initialSelectedDateMillis = endDate.atStartOfDay(ZoneId.of("UTC")).toInstant().toEpochMilli()
    )
    DatePickerDialog(
      onDismissRequest = { showEndDatePicker = false },
      confirmButton = {
        TextButton(onClick = {
          datePickerState.selectedDateMillis?.let { millis ->
            val picked = Instant.ofEpochMilli(millis).atZone(ZoneId.of("UTC")).toLocalDate()
            endDate = picked
          }
          showEndDatePicker = false
        }) {
          Text("Aceptar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEndDatePicker = false }) {
          Text("Cancelar")
        }
      }
    ) {
      DatePicker(state = datePickerState)
    }
  }

  if (showDeleteConfirm && onDelete != null) {
    AlertDialog(
      onDismissRequest = { showDeleteConfirm = false },
      title = { Text("¿Eliminar este registro de periodo?") },
      text = { Text("Esta acción eliminará el ciclo registrado y recalculará la duración entre los periodos adyacentes.") },
      confirmButton = {
        Button(
          onClick = {
            showDeleteConfirm = false
            isSubmitting = true
            onDelete { success, err ->
              isSubmitting = false
              if (success) {
                onDismiss()
              } else {
                dialogError = err ?: "Error al eliminar el ciclo."
              }
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
          modifier = Modifier.testTag("confirm_delete_period_button")
        ) {
          Text("Eliminar")
        }
      },
      dismissButton = {
        TextButton(onClick = { showDeleteConfirm = false }) {
          Text("Cancelar")
        }
      }
    )
  }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    shape = RoundedCornerShape(26.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Spa, contentDescription = null, tint = PhaseMenstrualRose, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = if (existingCycle != null) "Editar Menstruación" else "Registrar Menstruación",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )
      }
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
            shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.errorContainer,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
          ) {
            Text(
              text = dialogError!!,
              color = MaterialTheme.colorScheme.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(10.dp)
            )
          }
        }

        Text(
          text = "Fecha de inicio del sangrado (Día 1):",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(4.dp))

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, AppleHairlineBorder, RoundedCornerShape(12.dp))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = startDate.format(dateFormatter),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.testTag("period_dialog_start_date_text")
              )
              Text(
                text = "Inicio del periodo menstrual",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            OutlinedButton(
              onClick = { showStartDatePicker = true },
              shape = RoundedCornerShape(10.dp),
              modifier = Modifier.testTag("pick_start_date_button")
            ) {
              Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Cambiar", fontSize = 12.sp)
            }
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Bleeding Finished Switch
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(14.dp))
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = "¿Ya terminó el sangrado?",
                fontWeight = FontWeight.SemiBold,
                style = MaterialTheme.typography.bodyMedium
              )
              Text(
                text = if (hasFinishedBleeding) "Registrar fecha de fin de sangrado" else "El periodo sigue en curso",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
            Switch(
              checked = hasFinishedBleeding,
              onCheckedChange = {
                hasFinishedBleeding = it
                if (it && endDate.isBefore(startDate)) {
                  endDate = startDate.plusDays(4).coerceAtMost(today)
                }
              },
              colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = PhaseMenstrualRose
              ),
              modifier = Modifier.testTag("period_finished_switch")
            )
          }
        }

        AnimatedVisibility(visible = hasFinishedBleeding) {
          Column(modifier = Modifier.padding(top = 12.dp)) {
            Text(
              text = "Fecha de fin del sangrado:",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            Surface(
              shape = RoundedCornerShape(12.dp),
              color = MaterialTheme.colorScheme.surface,
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, AppleHairlineBorder, RoundedCornerShape(12.dp))
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(12.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = endDate.format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                  )
                  val daysCount = java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1
                  Text(
                    text = "Duración del sangrado: $daysCount días",
                    fontSize = 11.sp,
                    color = PhaseMenstrualRose,
                    fontWeight = FontWeight.SemiBold
                  )
                }
                OutlinedButton(
                  onClick = { showEndDatePicker = true },
                  shape = RoundedCornerShape(10.dp),
                  modifier = Modifier.testTag("pick_end_date_button")
                ) {
                  Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
                  Spacer(modifier = Modifier.width(4.dp))
                  Text("Cambiar", fontSize = 12.sp)
                }
              }
            }
          }
        }

        if (existingCycle != null && onDelete != null) {
          Spacer(modifier = Modifier.height(16.dp))
          OutlinedButton(
            onClick = { showDeleteConfirm = true },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().testTag("delete_period_button")
          ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Eliminar este ciclo")
          }
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (startDate.isAfter(today)) {
            dialogError = "La fecha de inicio no puede ser una fecha futura."
            return@Button
          }
          if (hasFinishedBleeding) {
            if (endDate.isAfter(today)) {
              dialogError = "La fecha de fin no puede ser una fecha futura."
              return@Button
            }
            if (endDate.isBefore(startDate)) {
              dialogError = "La fecha de fin no puede ser anterior a la fecha de inicio."
              return@Button
            }
          }

          if (!isSubmitting) {
            isSubmitting = true
            dialogError = null
            onSave(startDate, if (hasFinishedBleeding) endDate else null) { success, err ->
              isSubmitting = false
              if (success) {
                onDismiss()
              } else {
                dialogError = err ?: "Error al guardar el periodo."
              }
            }
          }
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PhaseMenstrualRose),
        modifier = Modifier.testTag("save_period_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
          Text(if (existingCycle != null) "Actualizar" else "Guardar Periodo", fontWeight = FontWeight.SemiBold)
        }
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSubmitting) {
        Text("Cancelar")
      }
    }
  )
}
