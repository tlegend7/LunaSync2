package com.example.ui.dialogs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.PerceivedMood
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseFertileSage
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PartnerObservationDialog(
  onDismiss: () -> Unit,
  onSave: (
    perceivedMood: PerceivedMood,
    apparentPainLevel: Int,
    unloggedSpottingReported: Boolean,
    careActionTaken: String?,
    sweetMessage: String?
  ) -> Unit
) {
  var perceivedMood by remember { mutableStateOf(PerceivedMood.TIRED) }
  var painFloat by remember { mutableFloatStateOf(1f) }
  var unloggedSpotting by remember { mutableStateOf(false) }
  var careAction by remember { mutableStateOf("") }
  var sweetMessage by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    shape = RoundedCornerShape(24.dp),
    title = {
      Text(
        text = "Registrar Observación y Apoyo",
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
        Text(
          text = "Ánimo que percibes en tu pareja:",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.padding(vertical = 6.dp)
        ) {
          PerceivedMood.entries.forEach { pm ->
            FilterChip(
              selected = perceivedMood == pm,
              onClick = { perceivedMood = pm },
              label = {
                val text = when (pm) {
                  PerceivedMood.CALM -> "Tranquila 😌"
                  PerceivedMood.TIRED -> "Cansada 🥱"
                  PerceivedMood.SAD -> "Sensible 🥺"
                  PerceivedMood.IRRITABLE -> "Irritable ⚡"
                  PerceivedMood.LOVING -> "Cariñosa 💕"
                }
                Text(text, fontSize = 12.sp)
              },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandTerracottaRose.copy(alpha = 0.15f),
                selectedLabelColor = BrandTerracottaRose
              ),
              border = FilterChipDefaults.filterChipBorder(
                enabled = true,
                selected = perceivedMood == pm,
                borderColor = AppleHairlineBorder,
                selectedBorderColor = BrandTerracottaRose
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Nivel de molestia o cansancio percibido: ${painFloat.roundToInt()} / 5",
          style = MaterialTheme.typography.labelLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Slider(
          value = painFloat,
          onValueChange = { painFloat = it },
          valueRange = 0f..5f,
          steps = 4,
          colors = SliderDefaults.colors(
            thumbColor = BrandTerracottaRose,
            activeTrackColor = BrandTerracottaRose
          ),
          modifier = Modifier.testTag("partner_pain_slider")
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Verbal Spotting Toggle
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = BrandTerracottaRose
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Mencionó haber tenido manchado",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium
            )
            Text(
              text = "Le mostrará una sugerencia sutil para si desea agregarlo",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = unloggedSpotting,
            onCheckedChange = { unloggedSpotting = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = BrandTerracottaRose
            ),
            modifier = Modifier.testTag("spotting_switch")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = careAction,
          onValueChange = { careAction = it },
          label = { Text("Acción de cuidado realizada") },
          placeholder = { Text("Ej. Le preparé una infusión caliente...") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
          value = sweetMessage,
          onValueChange = { sweetMessage = it },
          label = { Text("Mensaje cariñoso de apoyo") },
          placeholder = { Text("Ej. Aquí estoy si me necesitas...") },
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(14.dp)
        )
      }
    },
    confirmButton = {
      Button(
        onClick = {
          onSave(
            perceivedMood,
            painFloat.roundToInt(),
            unloggedSpotting,
            careAction.ifBlank { null },
            sweetMessage.ifBlank { null }
          )
          onDismiss()
        },
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
        modifier = Modifier.height(44.dp).testTag("save_observation_button")
      ) {
        Text("Guardar Observación", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Cancelar", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  )
}
