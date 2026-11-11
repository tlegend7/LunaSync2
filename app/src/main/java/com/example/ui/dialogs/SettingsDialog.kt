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
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaNavigationRow
import com.example.ui.components.LunaOptionChip
import com.example.ui.theme.LunaBorderLight
import com.example.ui.theme.LunaDarkBorder
import com.example.ui.theme.LunaDarkSurface
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaSurfaceLight
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.StatusError

enum class AppAppearanceMode {
  SYSTEM,
  LIGHT,
  DARK
}

/**
 * Pantalla / Diálogo de Ajustes y Apariencia (Figma: settings · A tu gusto #5:1011)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
  currentAppearance: AppAppearanceMode,
  onAppearanceSelected: (AppAppearanceMode) -> Unit,
  currentRole: UserRole,
  onSwitchRole: () -> Unit,
  onOpenPrivacy: () -> Unit,
  onSignOut: () -> Unit,
  userEmail: String?,
  onDismiss: () -> Unit
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f

  AlertDialog(
    onDismissRequest = onDismiss,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .testTag("settings_dialog")
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
          text = "A tu gusto",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
          text = "Apariencia, cuenta y preferencias.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(20.dp))

        // 3. Encabezado Apariencia (Figma #5:1042)
        Text(
          text = "Apariencia",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.SemiBold,
          color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Segmented Control: Sistema / Claro / Oscuro (Figma #15:337)
        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          LunaOptionChip(
            text = "Sistema",
            selected = currentAppearance == AppAppearanceMode.SYSTEM,
            onSelect = { onAppearanceSelected(AppAppearanceMode.SYSTEM) },
            modifier = Modifier.weight(1f).testTag("appearance_system_chip")
          )

          LunaOptionChip(
            text = "Claro",
            selected = currentAppearance == AppAppearanceMode.LIGHT,
            onSelect = { onAppearanceSelected(AppAppearanceMode.LIGHT) },
            modifier = Modifier.weight(1f).testTag("appearance_light_chip")
          )

          LunaOptionChip(
            text = "Oscuro",
            selected = currentAppearance == AppAppearanceMode.DARK,
            onSelect = { onAppearanceSelected(AppAppearanceMode.DARK) },
            modifier = Modifier.weight(1f).testTag("appearance_dark_chip")
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Puedes seguir el modo del teléfono o elegir uno.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)
        Spacer(modifier = Modifier.height(8.dp))

        // 4. Fila Usuario (Figma #15:357)
        LunaNavigationRow(
          title = if (!userEmail.isNullOrBlank()) userEmail else if (currentRole == UserRole.PRIMARY_ELLA) "Ana S." else "Dani M.",
          subtitle = "Cuenta personal conectada",
          onClick = {},
          icon = Icons.Default.AccountCircle,
          iconTint = LunaNavyPrimary
        )

        HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)

        // 5. Fila Privacidad (Figma #15:367)
        LunaNavigationRow(
          title = "Privacidad",
          subtitle = "Tus decisiones, siempre visibles.",
          onClick = {
            onDismiss()
            onOpenPrivacy()
          },
          icon = Icons.Default.Shield,
          iconTint = LunaNavyPrimary,
          modifier = Modifier.testTag("open_privacy_from_settings")
        )

        HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)

        // 6. Fila Cambiar experiencia / alternar rol demo (Figma #15:376)
        LunaNavigationRow(
          title = "Cambiar experiencia",
          subtitle = if (currentRole == UserRole.PRIMARY_ELLA) "Vista previa del rol de pareja" else "Vista previa del rol de ella",
          onClick = {
            onDismiss()
            onSwitchRole()
          },
          icon = Icons.Default.Favorite,
          iconTint = LunaNavyPrimary,
          modifier = Modifier.testTag("switch_experience_row")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 7. Botón Cerrar sesión
        LunaButton(
          text = "Cerrar sesión",
          onClick = {
            onDismiss()
            onSignOut()
          },
          type = LunaButtonType.DANGER,
          icon = Icons.Default.ExitToApp,
          modifier = Modifier.testTag("sign_out_button")
        )

        Spacer(modifier = Modifier.height(10.dp))

        // 8. Botón Volver a mi espacio
        LunaButton(
          text = "Volver a mi espacio",
          onClick = onDismiss,
          type = LunaButtonType.SECONDARY,
          modifier = Modifier.testTag("close_settings_button")
        )
      }
    }
  }
}

