package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.SpaceStatus
import com.example.data.model.UserRole
import com.example.data.model.PairingInvite
import com.example.data.model.SharingPermissions
import java.time.Clock
import java.time.format.DateTimeFormatter
import java.util.Locale
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaIcons
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaNavigationRow
import com.example.ui.theme.*

/**
 * Pantalla del Espacio Compartido / Nosotros (Figma: space · Nuestro espacio #5:535 y partnerSpace #5:1282)
 */
@Composable
fun PairingScreen(
  currentRole: UserRole,
  activeInvite: PairingInvite?,
  spaceStatus: String?,
  partnerId: String?,
  isLoading: Boolean,
  onGenerateCode: () -> Unit,
  onJoinCode: (String) -> Unit,
  onRevokePairing: ((Boolean, String?) -> Unit) -> Unit = {},
  onOpenPrivacySettings: () -> Unit = {},
  modifier: Modifier = Modifier,
  activeInviteVersion: Long = 1L,
  inviteError: String? = null,
  isGuestMode: Boolean = false,
  clock: Clock = Clock.systemDefaultZone(),
  sharingPermissions: SharingPermissions = SharingPermissions(),
  onOpenSharedRecords: () -> Unit = {}
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  var inputCode by remember { mutableStateOf("") }
  var showUnlinkDialog by remember { mutableStateOf(false) }
  var showInviteDialog by remember { mutableStateOf(false) }

  val isPaired = spaceStatus == SpaceStatus.PAIRED.name && !partnerId.isNullOrBlank()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 12.dp, bottom = 100.dp)
  ) {
    // 1. Título y subtítulo
    Text(
      text = if (currentRole == UserRole.SUPPORT_PARTNER) "Un vínculo con espacio" else "Nuestro espacio",
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = if (currentRole == UserRole.SUPPORT_PARTNER) {
        "Acompañar con respeto, sin invadir la privacidad."
      } else {
        "Conectados, con límites que tú eliges."
      },
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    if (currentRole == UserRole.SUPPORT_PARTNER) {
      // ---------------- VISTA DE LA PAREJA (SUPPORT_PARTNER) ----------------
      if (isPaired) {
        // Tarjeta Vínculo Activo
        Surface(
          shape = RoundedCornerShape(24.dp),
          color = MaterialTheme.colorScheme.lunaHero,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Icon(
              imageVector = LunaIcons.HeroHeart,
              contentDescription = null,
              tint = LunaIceBlueAccent,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Espacio Compartido Activo",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.lunaOnHero
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "Conectado al ciclo de tu pareja. Tienes acceso a recomendaciones y resúmenes autorizados.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.lunaHeroMuted
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Tarjeta de Privacidad y límites
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isDark) LunaDarkSurface else LunaIceBlueSubtle,
          border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
          ) {
            Icon(
              imageVector = LunaIcons.Lock,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
              Text(
                text = "Privacidad protegida",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
              )
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "Sus notas privadas nunca se comparten. Los registros de intimidad solo aparecen si ella los autoriza.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text("Lo que tu pareja comparte", style = MaterialTheme.typography.titleLarge)
        listOf("Síntomas físicos" to sharingPermissions.shareSymptoms, "Ánimo y energía" to sharingPermissions.shareMood,
          "Intimidad" to sharingPermissions.shareIntimacy).forEach { (title, shared) ->
          LunaNavigationRow(title, if (shared) "Solo los registros compartidos." else "No compartido.",
            onClick = if (shared) onOpenSharedRecords else onOpenPrivacySettings,
            icon = if (shared) LunaIcons.Check else LunaIcons.Lock,
            iconTint = if (shared) { if (isDark) LunaSageDarkText else LunaSageText }
              else MaterialTheme.colorScheme.onSurfaceVariant)
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
        Spacer(Modifier.height(12.dp))
        Text("No compartido no significa que no exista información. Sus notas privadas nunca aparecen aquí.",
          style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(24.dp))

        LunaButton(
          text = "Desvincularme del espacio",
          onClick = { showUnlinkDialog = true },
          type = LunaButtonType.DANGER,
          modifier = Modifier.testTag("unlink_partner_button")
        )
      } else {
        // Formulario de Unión mediante Código (Figma #5:1236 join)
        Surface(
          shape = RoundedCornerShape(20.dp),
          color = if (isDark) LunaDarkSurface else LunaSurfaceLight,
          border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(20.dp)) {
            Box(
              modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (isDark) LunaDarkBorder else LunaIceBlueSubtle),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = LunaIcons.Link,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp)
              )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
              text = "Pídele su código a tu pareja",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
              text = "Ella lo genera desde su espacio. Los permisos se eligen por separado.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = inputCode,
              onValueChange = { inputCode = it.uppercase() },
              label = { Text("Código de invitación") },
              placeholder = { Text("LUNA-XXXX") },
              singleLine = true,
              shape = RoundedCornerShape(12.dp),
              colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = if (isDark) LunaDarkBorder else LunaBorderLight
              ),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pairing_code_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            LunaButton(
              text = "Vincularme al espacio",
              onClick = { onJoinCode(inputCode) },
              enabled = inputCode.isNotBlank(),
              loading = isLoading,
              type = LunaButtonType.PRIMARY,
              modifier = Modifier.testTag("submit_join_code_button")
            )
          }
        }
      }
    } else {
      // ---------------- VISTA DE ELLA (PRIMARY_ELLA) ----------------
      // Panel de estado de pareja (Figma Patrón 17 #15:237)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.lunaHero,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Icon(
            imageVector = LunaIcons.HeroHeart,
            contentDescription = null,
            tint = LunaIceBlueAccent,
            modifier = Modifier.size(22.dp)
          )
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = if (isPaired) "Espacio de Pareja Activo" else "Tu Espacio Personal",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.lunaOnHero
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = if (isPaired) {
              "Vínculo activo · Tú administras lo que compartes en todo momento."
            } else {
              "Aún no tienes una pareja vinculada. Puedes invitarla cuando lo desees."
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.lunaHeroMuted
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      // Fila: Privacidad y permisos (Figma Patrón 18 #15:247)
      LunaNavigationRow(
        title = "Privacidad y permisos",
        subtitle = "Revisa qué puede ver tu pareja.",
        onClick = onOpenPrivacySettings,
        icon = LunaIcons.Shield,
        iconTint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("open_privacy_settings_row")
      )

      HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)

      // Fila: Invitar o revisar el vínculo (Figma Patrón 19 #15:257)
      LunaNavigationRow(
        title = "Invitar o revisar el vínculo",
        subtitle = "La conexión no comparte todo automáticamente.",
        onClick = {
          showInviteDialog = true
        },
        icon = LunaIcons.Link,
        iconTint = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.testTag("open_invite_row")
      )

      HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)

      Spacer(modifier = Modifier.height(20.dp))

      // Encabezado: Un acuerdo sencillo (Figma #5:610)
      Text(
        text = "Un acuerdo sencillo",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = "Acompañar no es adivinar. Preguntar siempre es una buena forma de empezar.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(16.dp))

      // Botón secundario: Revisar lo que comparto
      LunaButton(
        text = "Revisar lo que comparto",
        onClick = onOpenPrivacySettings,
        type = LunaButtonType.SECONDARY,
        modifier = Modifier.testTag("review_shared_permissions_cta")
      )

      if (isPaired) {
        Spacer(modifier = Modifier.height(10.dp))

        // Botón de peligro: Desvincular a mi pareja (Figma #12:31)
        LunaButton(
          text = "Desvincular a mi pareja",
          onClick = { showUnlinkDialog = true },
          type = LunaButtonType.DANGER,
          modifier = Modifier.testTag("open_unlink_confirm_cta")
        )
      }
    }
  }

  if (showInviteDialog) InviteDetailsDialog(activeInvite, activeInviteVersion, isPaired, isLoading,
    inviteError, isGuestMode, clock, onGenerateCode,
    onPrivacy = { showInviteDialog = false; onOpenPrivacySettings() },
    onDismiss = { showInviteDialog = false })
  if (showUnlinkDialog) UnlinkPairingDialog(currentRole, onRevokePairing, { showUnlinkDialog = false })
}
