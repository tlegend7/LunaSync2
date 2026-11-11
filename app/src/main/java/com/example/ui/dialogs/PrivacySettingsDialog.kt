package com.example.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SharingPermissions
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaToggle
import com.example.ui.theme.*

/**
 * Diálogo / Pantalla modal de control de privacidad y permisos (Figma: privacy · Tú eliges #5:615)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacySettingsDialog(
  currentPermissions: SharingPermissions,
  onSavePermissions: (SharingPermissions) -> Unit,
  onDismiss: () -> Unit
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f

  var shareSymptoms by remember { mutableStateOf(currentPermissions.shareSymptoms) }
  var shareMood by remember { mutableStateOf(currentPermissions.shareMood) }
  var shareIntimacy by remember { mutableStateOf(currentPermissions.shareIntimacy) }
  var isSaving by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .testTag("privacy_settings_dialog")
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
            tint = if (isDark) LunaNavyPrimary else LunaNavyPrimary,
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
          text = "Tú eliges",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "Los permisos se pueden cambiar en cualquier momento.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(14.dp))

        // 3. Badge "Control de privacidad"
        LunaBadge(
          text = "Control de privacidad",
          style = LunaBadgeStyle.PRIVATE
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 4. Switch: Síntomas físicos
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Síntomas físicos",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Flujo, dolor y molestias registradas.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          LunaToggle(
            checked = shareSymptoms,
            onCheckedChange = { shareSymptoms = it },
            modifier = Modifier.testTag("toggle_share_symptoms")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // 5. Switch: Ánimo y energía
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Ánimo y energía",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Solo los registros que decidas compartir.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          LunaToggle(
            checked = shareMood,
            onCheckedChange = { shareMood = it },
            modifier = Modifier.testTag("toggle_share_mood")
          )
        }

        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)
        Spacer(modifier = Modifier.height(12.dp))

        // 6. Switch: Registros de intimidad
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Registros de intimidad",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Solo los no marcados como privados.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
          Spacer(modifier = Modifier.width(12.dp))
          LunaToggle(
            checked = shareIntimacy,
            onCheckedChange = { shareIntimacy = it },
            modifier = Modifier.testTag("toggle_share_intimacy")
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Panel "Tus notas privadas son solo tuyas" (Figma Patrón 20 #15:265)
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isDark) LunaDarkSurface else LunaIceBlueSubtle,
          border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.padding(14.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Lock,
              contentDescription = null,
              tint = LunaNavyPrimary,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "Tus notas privadas son solo tuyas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "No se comparten con tu pareja, aunque actives otros permisos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "La revocación limita el acceso futuro. No borra lo que alguien ya haya visto.",
          style = MaterialTheme.typography.bodySmall,
          fontSize = 11.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 8. Botón Guardar permisos
        LunaButton(
          text = "Guardar permisos",
          onClick = {
            isSaving = true
            val updated = currentPermissions.copy(
              shareSymptoms = shareSymptoms,
              shareMood = shareMood,
              shareIntimacy = shareIntimacy
            )
            onSavePermissions(updated)
            onDismiss()
          },
          loading = isSaving,
          type = LunaButtonType.PRIMARY,
          modifier = Modifier.testTag("save_permissions_button")
        )
      }
    }
  }
}

