package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleHealthFollicularSubtle
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5
import com.example.ui.theme.AppleSystemIndigo

import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.OutlinedButton

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

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .padding(24.dp)
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center,
      modifier = Modifier.fillMaxSize()
    ) {
      // Clean Apple style app glyph
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
          .size(88.dp)
          .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(26.dp))
      ) {
        Box(contentAlignment = Alignment.Center) {
          Icon(
            imageVector = Icons.Default.NightsStay,
            contentDescription = "LunaSync Logo",
            tint = AppleSystemBlue,
            modifier = Modifier.size(44.dp)
          )
        }
      }

      Spacer(modifier = Modifier.height(20.dp))

      Text(
        text = "LunaSync",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        letterSpacing = (-0.5).sp
      )

      Text(
        text = "Sincronización y cuidado del ciclo en pareja",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(horizontal = 24.dp, vertical = 6.dp)
      )

      Spacer(modifier = Modifier.height(32.dp))

      // Apple Card for Role Selection
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "SELECCIONA TU ROL INICIAL",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
          )

          Spacer(modifier = Modifier.height(12.dp))

          Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            FilterChip(
              selected = currentRole == UserRole.PRIMARY_ELLA,
              onClick = { onRoleSelected(UserRole.PRIMARY_ELLA) },
              label = { Text("Ella (Espacio)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppleSystemBlue,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.weight(1f).testTag("role_ella_chip")
            )

            FilterChip(
              selected = currentRole == UserRole.SUPPORT_PARTNER,
              onClick = { onRoleSelected(UserRole.SUPPORT_PARTNER) },
              label = { Text("Novio (Soporte)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
              shape = RoundedCornerShape(12.dp),
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = AppleSystemBlue,
                selectedLabelColor = Color.White
              ),
              modifier = Modifier.weight(1f).testTag("role_partner_chip")
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(24.dp))

      // Apple Style Pill CTA
      Button(
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
        enabled = !isLoading,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("google_signin_button")
      ) {
        if (isLoading) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = Color.White,
            strokeWidth = 2.dp
          )
        } else {
          Icon(Icons.Default.Shield, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Continuar con Google",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
          )
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Apple HIG Outlined Secondary CTA for instant emulator exploration
      OutlinedButton(
        onClick = onEnterGuestMode,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppleHairlineBorder),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
          .fillMaxWidth()
          .height(52.dp)
          .testTag("guest_demo_mode_button")
      ) {
        Icon(
          imageVector = Icons.Default.PlayArrow,
          contentDescription = null,
          tint = AppleSystemBlue,
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Acceso Rápido / Modo Demo",
          style = MaterialTheme.typography.bodyMedium,
          fontWeight = FontWeight.SemiBold
        )
      }

      Spacer(modifier = Modifier.height(6.dp))

      Text(
        text = "Prueba todas las funciones en el emulador sin iniciar sesión",
        style = MaterialTheme.typography.bodySmall,
        fontSize = 11.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center
      )

      if (errorMessage != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = errorMessage!!,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.error,
          textAlign = TextAlign.Center
        )
      }

      Spacer(modifier = Modifier.height(24.dp))

      Surface(
        shape = RoundedCornerShape(14.dp),
        color = AppleSystemGray5.copy(alpha = 0.5f)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
          Icon(Icons.Default.Favorite, contentDescription = null, tint = AppleSystemIndigo, modifier = Modifier.size(14.dp))
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "Privacidad protegida con reglas Zero-Trust",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }
  }
}
