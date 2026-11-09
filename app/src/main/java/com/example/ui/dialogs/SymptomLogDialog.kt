package com.example.ui.dialogs

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import com.example.data.model.CervicalMucus
import com.example.data.model.DailySymptom
import com.example.data.model.FlowLevel
import com.example.data.model.Mood
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseFertileSage
import com.example.ui.theme.PhaseFollicularPeach
import com.example.ui.theme.PhaseLutealLavender
import com.example.ui.theme.PhaseMenstrualRose
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomLogDialog(
  initialSymptom: DailySymptom?,
  onDismiss: () -> Unit,
  onSave: (
    flowLevel: FlowLevel,
    painLevel: Int,
    mood: Mood,
    energyLevel: Int,
    cramps: Boolean,
    headache: Boolean,
    backPain: Boolean,
    bloating: Boolean,
    cervicalMucus: CervicalMucus,
    tenderBreasts: Boolean,
    cravings: Boolean,
    acne: Boolean,
    publicNote: String?,
    privateNote: String?,
    isPrivate: Boolean,
    onComplete: (Boolean, String?) -> Unit
  ) -> Unit
) {
  var flowLevel by remember {
    mutableStateOf(
      initialSymptom?.let {
        try { FlowLevel.valueOf(it.flowLevel) } catch (e: Exception) { FlowLevel.NONE }
      } ?: FlowLevel.NONE
    )
  }

  var painFloat by remember {
    mutableFloatStateOf(initialSymptom?.painLevel?.toFloat() ?: 0f)
  }

  var energyFloat by remember {
    mutableFloatStateOf(if (initialSymptom != null && initialSymptom.energyLevel > 0) initialSymptom.energyLevel.toFloat() else 3f)
  }

  var mood by remember {
    mutableStateOf(
      initialSymptom?.let {
        try { if (it.mood.isNotBlank()) Mood.valueOf(it.mood) else Mood.CALM } catch (e: Exception) { Mood.CALM }
      } ?: Mood.CALM
    )
  }

  var cervicalMucus by remember {
    mutableStateOf(
      initialSymptom?.let {
        try { CervicalMucus.valueOf(it.cervicalMucus) } catch (e: Exception) { CervicalMucus.NONE }
      } ?: CervicalMucus.NONE
    )
  }

  var cramps by remember { mutableStateOf(initialSymptom?.cramps ?: false) }
  var tenderBreasts by remember { mutableStateOf(initialSymptom?.tenderBreasts ?: false) }
  var cravings by remember { mutableStateOf(initialSymptom?.cravings ?: false) }
  var headache by remember { mutableStateOf(initialSymptom?.headache ?: false) }
  var backPain by remember { mutableStateOf(initialSymptom?.backPain ?: false) }
  var bloating by remember { mutableStateOf(initialSymptom?.bloating ?: false) }
  var acne by remember { mutableStateOf(initialSymptom?.acne ?: false) }

  var publicNote by remember { mutableStateOf(initialSymptom?.publicNote ?: "") }
  var privateNote by remember { mutableStateOf(initialSymptom?.privateNote ?: "") }
  var isPrivate by remember { mutableStateOf(initialSymptom?.isPrivate ?: false) }
  var isSubmitting by remember { mutableStateOf(false) }
  var dialogError by remember { mutableStateOf<String?>(null) }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    shape = RoundedCornerShape(26.dp),
    title = {
      Text(
        text = "Registro Holístico Diario",
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

        // 1. Flow Level
        Text(
          text = "Flujo / Sangrado:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 6.dp)
        ) {
          FlowLevel.entries.forEach { level ->
            FilterChip(
              selected = flowLevel == level,
              onClick = { flowLevel = level },
              label = {
                val label = when (level) {
                  FlowLevel.NONE -> "Ninguno"
                  FlowLevel.SPOTTING -> "Manchado"
                  FlowLevel.LIGHT -> "Ligero"
                  FlowLevel.MEDIUM -> "Medio"
                  FlowLevel.HEAVY -> "Abundante"
                }
                Text(label, fontSize = 12.sp)
              },
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PhaseMenstrualRose,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("flow_chip_${level.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 2. Cervical Mucus
        Text(
          text = "Moco Cervical (Fertilidad):",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 6.dp)
        ) {
          CervicalMucus.entries.forEach { mucus ->
            FilterChip(
              selected = cervicalMucus == mucus,
              onClick = { cervicalMucus = mucus },
              label = {
                val label = when (mucus) {
                  CervicalMucus.NONE -> "Sin flujo"
                  CervicalMucus.DRY -> "Seco"
                  CervicalMucus.STICKY -> "Pegajoso"
                  CervicalMucus.CREAMY -> "Cremoso"
                  CervicalMucus.EGG_WHITE -> "Clara de huevo ✨"
                }
                Text(label, fontSize = 12.sp)
              },
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PhaseFertileSage,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("mucus_chip_${mucus.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3. Physical Sensations
        Text(
          text = "Sensaciones del Cuerpo:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 6.dp)
        ) {
          FilterChip(
            selected = cramps,
            onClick = { cramps = !cramps },
            label = { Text("Cólicos", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandTerracottaRose,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("symptom_cramps_chip")
          )

          FilterChip(
            selected = tenderBreasts,
            onClick = { tenderBreasts = !tenderBreasts },
            label = { Text("Senos sensibles", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PhaseLutealLavender,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("symptom_breasts_chip")
          )

          FilterChip(
            selected = cravings,
            onClick = { cravings = !cravings },
            label = { Text("Antojos", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PhaseFollicularPeach,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.testTag("symptom_cravings_chip")
          )

          FilterChip(
            selected = headache,
            onClick = { headache = !headache },
            label = { Text("Cefalea", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandTerracottaRose,
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = backPain,
            onClick = { backPain = !backPain },
            label = { Text("Espalda baja", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandTerracottaRose,
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = bloating,
            onClick = { bloating = !bloating },
            label = { Text("Hinchazón", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = BrandTerracottaRose,
              selectedLabelColor = Color.White
            )
          )

          FilterChip(
            selected = acne,
            onClick = { acne = !acne },
            label = { Text("Acné / Piel", fontSize = 12.sp) },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PhaseFollicularPeach,
              selectedLabelColor = Color.White
            )
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 4. Mood & Emotional State
        Text(
          text = "Estado Emocional:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 6.dp)
        ) {
          Mood.entries.forEach { item ->
            FilterChip(
              selected = mood == item,
              onClick = { mood = item },
              label = {
                val label = when (item) {
                  Mood.HAPPY -> "Radiante"
                  Mood.CALM -> "Calma"
                  Mood.SENSITIVE -> "Sensible"
                  Mood.ENERGETIC -> "Enérgica"
                  Mood.CRAVINGS -> "Con antojo"
                  Mood.IRRITABLE -> "Irritable"
                  Mood.ANXIOUS -> "Ansiosa"
                  Mood.SAD -> "Con bajón"
                }
                Text(label, fontSize = 12.sp)
              },
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandTerracottaRose,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("mood_chip_${item.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Pain Slider
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Nivel de dolor / molestia:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${painFloat.roundToInt()} / 5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
          value = painFloat,
          onValueChange = { painFloat = it },
          valueRange = 0f..5f,
          steps = 4,
          colors = SliderDefaults.colors(
            thumbColor = PhaseMenstrualRose,
            activeTrackColor = PhaseMenstrualRose
          )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // Energy Slider
        Row(
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text("Nivel de energía vital:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${energyFloat.roundToInt()} / 5", fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Slider(
          value = energyFloat,
          onValueChange = { energyFloat = it },
          valueRange = 1f..5f,
          steps = 3,
          colors = SliderDefaults.colors(
            thumbColor = PhaseFollicularPeach,
            activeTrackColor = PhaseFollicularPeach
          )
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Public note
        OutlinedTextField(
          value = publicNote,
          onValueChange = { publicNote = it },
          label = { Text("Nota para tu pareja (visible)", fontSize = 12.sp) },
          placeholder = { Text("Ej: Me vendría genial cenar algo liviano hoy...", fontSize = 12.sp) },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("symptom_public_note_input")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Private note
        OutlinedTextField(
          value = privateNote,
          onValueChange = { privateNote = it },
          label = {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.height(14.dp))
              Spacer(modifier = Modifier.width(4.dp))
              Text("Nota privada (solo tú puedes verla)", fontSize = 12.sp)
            }
          },
          shape = RoundedCornerShape(12.dp),
          modifier = Modifier.fillMaxWidth().testTag("symptom_private_note_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy switch for entire log
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Ocultar este registro a mi pareja",
              fontWeight = FontWeight.SemiBold,
              style = MaterialTheme.typography.bodyMedium
            )
            Text(
              text = if (isPrivate) "Registro protegido en tu cuenta; inaccesible para tu pareja" else "Tu pareja podrá ver los síntomas si el permiso global está activo",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isPrivate,
            onCheckedChange = { isPrivate = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = BrandTerracottaRose
            )
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (!isSubmitting) {
            isSubmitting = true
            dialogError = null
            onSave(
              flowLevel,
              painFloat.roundToInt(),
              mood,
              energyFloat.roundToInt(),
              cramps,
              headache,
              backPain,
              bloating,
              cervicalMucus,
              tenderBreasts,
              cravings,
              acne,
              publicNote.ifBlank { null },
              privateNote.ifBlank { null },
              isPrivate
            ) { success, err ->
              isSubmitting = false
              if (success) {
                onDismiss()
              } else {
                dialogError = err ?: "Error al guardar síntomas. Intenta nuevamente."
              }
            }
          }
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
        modifier = Modifier.testTag("save_symptom_button")
      ) {
        if (isSubmitting) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
        } else {
          Text("Guardar Registro", fontWeight = FontWeight.SemiBold)
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
