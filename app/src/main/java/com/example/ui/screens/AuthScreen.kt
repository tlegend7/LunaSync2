package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.UserRole
import com.example.data.repository.AuthRepository
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaContextTopBar
import com.example.ui.theme.LunaBorderLight
import com.example.ui.theme.LunaIceBlueAccent
import com.example.ui.theme.LunaIceBlueSubtle
import com.example.ui.theme.LunaIceBlueText
import com.example.ui.theme.LunaNavyDarkCard
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.StatusError

/**
 * Pantalla de bienvenida y selección de experiencia inicial (Figma: welcome · Tu ritmo. Su compañía #5:32)
 */
@Composable
fun AuthScreen(
  authRepo: AuthRepository,
  currentRole: UserRole,
  onRoleSelected: (UserRole) -> Unit,
  onAuthSuccess: () -> Unit,
  onEnterGuestMode: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val scope = rememberCoroutineScope()
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(top = 16.dp, bottom = 120.dp)
    ) {
      // 1. Contexto superior
      LunaContextTopBar(
        contextText = "LunaSync / Tu espacio",
        avatarInitials = if (currentRole == UserRole.PRIMARY_ELLA) "AS" else "DM",
        onAvatarClick = {}
      )

      Spacer(modifier = Modifier.height(16.dp))

      // 2. Título principal
      Text(
        text = "Tu ritmo.\nSu compañía.",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground,
        lineHeight = 38.sp
      )

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Un espacio para conocerte mejor, a solas o en pareja.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      Spacer(modifier = Modifier.height(20.dp))

      // 3. Tarjeta de Identidad LunaSync (Figma #5:63)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = LunaNavyDarkCard,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(24.dp)) {
          Icon(
            imageVector = Icons.Default.NightsStay,
            contentDescription = "LunaSync Moon",
            tint = LunaIceBlueAccent,
            modifier = Modifier.size(36.dp)
          )
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "LunaSync",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "A tu lado, sin invadir tu espacio.",
            style = MaterialTheme.typography.bodyLarge,
            color = LunaIceBlueText
          )
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // 4. Encabezado de elección
      Text(
        text = "¿Cómo usarás LunaSync?",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Spacer(modifier = Modifier.height(12.dp))

      // Fila 1: Quiero seguir mi ciclo (Ella)
      val isEllaSelected = currentRole == UserRole.PRIMARY_ELLA
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isEllaSelected) {
          if (isDark) MaterialTheme.colorScheme.surfaceVariant else LunaIceBlueSubtle
        } else {
          MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
          1.dp,
          if (isEllaSelected) LunaNavyPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { onRoleSelected(UserRole.PRIMARY_ELLA) }
          .testTag("role_ella_chip")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isEllaSelected) LunaNavyPrimary else Color(0xFFEDF2F7)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.WaterDrop,
              contentDescription = null,
              tint = if (isEllaSelected) Color.White else LunaNavyPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Quiero seguir mi ciclo",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Mis registros y mi privacidad.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (isEllaSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Seleccionado",
              tint = LunaNavyPrimary,
              modifier = Modifier.size(20.dp)
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = LunaTextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))

      // Fila 2: Quiero acompañar (Pareja)
      val isPartnerSelected = currentRole == UserRole.SUPPORT_PARTNER
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isPartnerSelected) {
          if (isDark) MaterialTheme.colorScheme.surfaceVariant else LunaIceBlueSubtle
        } else {
          MaterialTheme.colorScheme.surface
        },
        border = BorderStroke(
          1.dp,
          if (isPartnerSelected) LunaNavyPrimary else MaterialTheme.colorScheme.outlineVariant
        ),
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .clickable { onRoleSelected(UserRole.SUPPORT_PARTNER) }
          .testTag("role_partner_chip")
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(16.dp)
        ) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isPartnerSelected) LunaNavyPrimary else Color(0xFFEDF2F7)),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = null,
              tint = if (isPartnerSelected) Color.White else LunaNavyPrimary,
              modifier = Modifier.size(20.dp)
            )
          }

          Spacer(modifier = Modifier.width(14.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Quiero acompañar",
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = "Solo lo que mi pareja decida compartir.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }

          if (isPartnerSelected) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = "Seleccionado",
              tint = LunaNavyPrimary,
              modifier = Modifier.size(20.dp)
            )
          } else {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
              contentDescription = null,
              tint = LunaTextSecondary,
              modifier = Modifier.size(20.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Text(
        text = "Vincularse es opcional. Tú eliges qué compartir.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
          text = errorMessage!!,
          style = MaterialTheme.typography.bodySmall,
          color = StatusError,
          textAlign = TextAlign.Center
        )
      }
    }

    // Acciones fijas inferiores (Figma #5:59)
    Surface(
      color = MaterialTheme.colorScheme.background,
      tonalElevation = 6.dp,
      modifier = Modifier
        .align(Alignment.BottomCenter)
        .fillMaxWidth()
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 12.dp)
      ) {
        // Botón principal
        LunaButton(
          text = "Entrar con mi cuenta",
          onClick = {
            isLoading = true
            errorMessage = null
            authRepo.signInWithGoogle(
              activity = context as Activity,
              scope = scope,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess()
              },
              onAuthError = { error ->
                isLoading = false
                errorMessage = error
              },
              onCancelled = {
                isLoading = false
              }
            )
          },
          loading = isLoading,
          enabled = !isLoading,
          type = LunaButtonType.PRIMARY,
          modifier = Modifier.testTag("google_signin_button")
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Botón secundario para modo demo
        LunaButton(
          text = "Acceso Rápido / Modo Demo",
          onClick = onEnterGuestMode,
          enabled = !isLoading,
          type = LunaButtonType.SECONDARY,
          icon = Icons.Default.PlayArrow,
          modifier = Modifier.testTag("guest_demo_mode_button")
        )
      }
    }
  }
}
