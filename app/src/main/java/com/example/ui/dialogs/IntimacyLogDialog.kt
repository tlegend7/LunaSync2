package com.example.ui.dialogs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.data.model.EncounterType
import com.example.data.model.PostContactEffect
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseFertileSage
import com.example.ui.theme.PhaseFertileSubtle
import com.example.ui.theme.PhaseFollicularPeach
import com.example.ui.theme.PhaseMenstrualRose

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun IntimacyLogDialog(
  onDismiss: () -> Unit,
  onSave: (
    encounterType: EncounterType,
    usedLubricant: Boolean,
    hadOrgasm: Boolean,
    postContactEffect: PostContactEffect,
    notes: String?,
    isPrivate: Boolean
  ) -> Unit
) {
  var encounterType by remember { mutableStateOf(EncounterType.PROTECTED) }
  var usedLubricant by remember { mutableStateOf(false) }
  var hadOrgasm by remember { mutableStateOf(false) }
  var postContactEffect by remember { mutableStateOf(PostContactEffect.NONE) }
  var notes by remember { mutableStateOf("") }
  var isPrivate by remember { mutableStateOf(false) }
  var showPhysiologyExplanation by remember { mutableStateOf(false) }
  var isSubmitting by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = { if (!isSubmitting) onDismiss() },
    shape = RoundedCornerShape(26.dp),
    title = {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Favorite, contentDescription = null, tint = PhaseMenstrualRose, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Registro Íntimo & Contacto",
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
        // 1. Encounter Type Selection
        Text(
          text = "Tipo de contacto o encuentro:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
          FilterChip(
            selected = encounterType == EncounterType.PROTECTED,
            onClick = { encounterType = EncounterType.PROTECTED },
            label = { Text("Con protección (preservativo/barrera)", fontSize = 12.sp) },
            leadingIcon = {
              Icon(Icons.Default.Shield, contentDescription = null, tint = PhaseFertileSage, modifier = Modifier.size(16.dp))
            },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PhaseFertileSage,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().testTag("chip_protected")
          )

          FilterChip(
            selected = encounterType == EncounterType.UNPROTECTED,
            onClick = { encounterType = EncounterType.UNPROTECTED },
            label = { Text("Sin protección (contacto seminal)", fontSize = 12.sp) },
            leadingIcon = {
              Icon(Icons.Default.Favorite, contentDescription = null, tint = PhaseMenstrualRose, modifier = Modifier.size(16.dp))
            },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PhaseMenstrualRose,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().testTag("chip_unprotected")
          )

          FilterChip(
            selected = encounterType == EncounterType.OUTERCOURSE,
            onClick = { encounterType = EncounterType.OUTERCOURSE },
            label = { Text("Caricias íntimas / Tocamientos (sin penetración)", fontSize = 12.sp) },
            leadingIcon = {
              Icon(Icons.Default.Spa, contentDescription = null, tint = PhaseFollicularPeach, modifier = Modifier.size(16.dp))
            },
            shape = RoundedCornerShape(12.dp),
            colors = FilterChipDefaults.filterChipColors(
              selectedContainerColor = PhaseFollicularPeach,
              selectedLabelColor = Color.White
            ),
            modifier = Modifier.fillMaxWidth().testTag("chip_outercourse")
          )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 2. Physical & Comfort details (Lubricant & Orgasm)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier.fillMaxWidth().border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(16.dp))
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            // Lubricant toggle
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("¿Uso de lubricante?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Ayuda a cuidar el pH y reduce la fricción", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Switch(
                checked = usedLubricant,
                onCheckedChange = { usedLubricant = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = PhaseFertileSage
                )
              )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Orgasm toggle
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.fillMaxWidth()
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text("¿Llegó al orgasmo?", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text("Libera oxitocina y alivia cólicos o tensión", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
              Switch(
                checked = hadOrgasm,
                onCheckedChange = { hadOrgasm = it },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = Color.White,
                  checkedTrackColor = BrandTerracottaRose
                )
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Post-Contact Effect on Flow & pH
        Text(
          text = "Sensación o cambio en flujo post-contacto:",
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))

        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          PostContactEffect.entries.forEach { effect ->
            FilterChip(
              selected = postContactEffect == effect,
              onClick = { postContactEffect = effect },
              label = {
                val label = when (effect) {
                  PostContactEffect.NONE -> "Todo habitual"
                  PostContactEffect.AROUSAL_FLUID -> "Mayor humedad / excitación"
                  PostContactEffect.PH_SHIFT -> "Flujo acuoso (semen/pH)"
                  PostContactEffect.CONTACT_SPOTTING -> "Manchado leve por roce"
                  PostContactEffect.FRICTION_SENSITIVITY -> "Sensibilidad por fricción"
                }
                Text(label, fontSize = 11.sp)
              },
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = BrandTerracottaRose,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.testTag("effect_chip_${effect.name}")
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // 4. Interactive Educational Pill on pH and Intimacy
        Surface(
          shape = RoundedCornerShape(14.dp),
          color = PhaseFertileSubtle,
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, PhaseFertileSage.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
            .clickable { showPhysiologyExplanation = !showPhysiologyExplanation }
        ) {
          Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Info, contentDescription = null, tint = PhaseFertileSage, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "¿Cómo influye el contacto en el pH y flujo?",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = PhaseFertileSage
              )
            }

            AnimatedVisibility(visible = showPhysiologyExplanation) {
              Text(
                text = "La flora vaginal es naturalmente ácida (pH 3.8 - 4.5). El semen es alcalino (~7.5) y tras una relación sin protección, alcaliniza el canal por 24 a 48 horas, causando un flujo acuoso que suele confundirse con moco fértil. Asimismo, los tocamientos o caricias aumentan el fluido de excitación natural (trasudado), y la fricción en cuello uterino sensible puede provocar un manchado rosado inofensivo.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Notes
        OutlinedTextField(
          value = notes,
          onValueChange = { notes = it },
          label = { Text("Notas íntimas (opcional)", fontSize = 12.sp) },
          placeholder = { Text("Sensaciones, caricias o detalles personales...", fontSize = 12.sp) },
          shape = RoundedCornerShape(14.dp),
          modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Privacy Switch
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Icon(
            imageVector = Icons.Default.Lock,
            contentDescription = null,
            tint = if (isPrivate) BrandTerracottaRose else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Registro íntimo privado",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold
            )
            Text(
              text = if (isPrivate) "Registro protegido; solo tú tendrás acceso" else "Visible para tu pareja si el permiso global de intimidad está activo",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Switch(
            checked = isPrivate,
            onCheckedChange = { isPrivate = it },
            colors = SwitchDefaults.colors(
              checkedThumbColor = Color.White,
              checkedTrackColor = BrandTerracottaRose
            ),
            modifier = Modifier.testTag("intimacy_privacy_switch")
          )
        }
      }
    },
    confirmButton = {
      Button(
        onClick = {
          if (!isSubmitting) {
            isSubmitting = true
            onSave(
              encounterType,
              usedLubricant,
              hadOrgasm,
              postContactEffect,
              notes.ifBlank { null },
              isPrivate
            )
            onDismiss()
          }
        },
        enabled = !isSubmitting,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
        modifier = Modifier.testTag("save_intimacy_button")
      ) {
        Text("Guardar Encuentro", fontWeight = FontWeight.SemiBold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss, enabled = !isSubmitting) {
        Text("Cancelar")
      }
    }
  )
}
