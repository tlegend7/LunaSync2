package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaCard
import com.example.ui.components.LunaDestructiveButton
import com.example.ui.components.LunaPrimaryButton
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.BrandWarmRose
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess

@Composable
fun PairingScreen(
  currentRole: UserRole,
  activeCode: String?,
  spaceStatus: String?,
  partnerId: String?,
  isLoading: Boolean,
  onGenerateCode: () -> Unit,
  onJoinCode: (String) -> Unit,
  onRevokePairing: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  var inputCode by remember { mutableStateOf("") }
  var showRevokeConfirmDialog by remember { mutableStateOf(false) }
  val isPaired = spaceStatus == SpaceStatus.PAIRED.name && !partnerId.isNullOrBlank()

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(20.dp)
  ) {
    // Header adaptado al rol activo
    Text(
      text = if (currentRole == UserRole.SUPPORT_PARTNER) "Vincular con mi Pareja" else "Invitar a mi Pareja",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )
    Spacer(modifier = Modifier.height(4.dp))
    Text(
      text = if (currentRole == UserRole.SUPPORT_PARTNER) {
        "Ingresa el código compartido para sincronizar el ciclo y acceder al espacio de apoyo y cuidados."
      } else {
        "Genera un código único para que tu pareja pueda acompañarte con información de apoyo consensuada."
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (currentRole == UserRole.SUPPORT_PARTNER) {
      // VISTA EXCLUSIVA DE LA PAREJA
      if (isPaired) {
        // Pareja YA vinculada exitosamente
        LunaCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .size(68.dp)
              .clip(CircleShape)
              .background(StatusSuccess.copy(alpha = 0.12f))
          ) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = null,
              tint = StatusSuccess,
              modifier = Modifier.size(38.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "¡Espacio Sincronizado!",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )

          Spacer(modifier = Modifier.height(8.dp))

          Text(
            text = "Estás vinculado al espacio compartido. Desde el Dashboard puedes ver la fase actual estimada, consultar la guía de cuidados diarios y enviar Check-ins de apoyo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )

          Spacer(modifier = Modifier.height(20.dp))

          LunaBadge(
            text = "Conexión Activa y Segura",
            style = LunaBadgeStyle.FACT,
            icon = Icons.Default.Favorite,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )
        }
      } else {
        // Pareja NO vinculada: Formulario directo para ingresar código
        LunaCard(
          modifier = Modifier.fillMaxWidth()
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .align(Alignment.CenterHorizontally)
              .size(64.dp)
              .clip(CircleShape)
              .background(BrandTerracottaRose.copy(alpha = 0.12f))
          ) {
            Icon(
              imageVector = Icons.Default.Key,
              contentDescription = null,
              tint = BrandTerracottaRose,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Ingresar Código de Pareja",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )

          Text(
            text = "Pídele a tu pareja que abra LunaSync en la pestaña 'Vincular' y te comparta su código de 8 caracteres (ej. LUNA-8X4K):",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier
              .padding(vertical = 8.dp)
              .align(Alignment.CenterHorizontally)
          )

          Spacer(modifier = Modifier.height(16.dp))

          OutlinedTextField(
            value = inputCode,
            onValueChange = { inputCode = it.uppercase() },
            label = { Text("Código de invitación") },
            placeholder = { Text("LUNA-XXXX") },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("pairing_code_input")
          )

          Spacer(modifier = Modifier.height(20.dp))

          LunaPrimaryButton(
            text = "Vincularme al Espacio",
            onClick = { onJoinCode(inputCode) },
            enabled = inputCode.isNotBlank(),
            loading = isLoading,
            icon = Icons.Default.Favorite,
            modifier = Modifier.testTag("submit_join_code_button")
          )
        }
      }
    } else {
      // VISTA EXCLUSIVA DE ELLA (ADMINISTRADORA DEL ESPACIO)
      LunaCard(
        modifier = Modifier.fillMaxWidth()
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .size(64.dp)
            .clip(CircleShape)
            .background(BrandTerracottaRose.copy(alpha = 0.12f))
        ) {
          Icon(
            imageVector = Icons.Default.QrCode,
            contentDescription = null,
            tint = BrandTerracottaRose,
            modifier = Modifier.size(32.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "Tu Código de Invitación",
          style = MaterialTheme.typography.titleLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Text(
          text = "Tu pareja debe ingresar este código en su app para conectarse a tu espacio compartido:",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
          modifier = Modifier
            .padding(vertical = 6.dp)
            .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (!activeCode.isNullOrBlank()) {
          Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
            modifier = Modifier
              .fillMaxWidth()
              .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center,
              modifier = Modifier.padding(vertical = 16.dp, horizontal = 20.dp)
            ) {
              Text(
                text = activeCode,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 4.sp,
                color = BrandTerracottaRose
              )
              Spacer(modifier = Modifier.width(16.dp))
              OutlinedButton(
                onClick = {
                  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                  clipboard.setPrimaryClip(ClipData.newPlainText("LunaSync Code", activeCode))
                  Toast.makeText(context, "Código copiado al portapapeles", Toast.LENGTH_SHORT).show()
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("copy_code_button")
              ) {
                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar", modifier = Modifier.size(16.dp))
              }
            }
          }
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Válido por 48 horas • Puedes revocar el acceso cuando quieras",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
          )
        } else {
          LunaPrimaryButton(
            text = "Generar Código de Invitación",
            onClick = onGenerateCode,
            loading = isLoading,
            icon = Icons.Default.Share,
            modifier = Modifier.testTag("generate_pairing_code_button")
          )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Badge de Estado de Conexión
        LunaBadge(
          text = if (isPaired) "Tu pareja está conectada a tu espacio" else "Esperando a que tu pareja ingrese el código...",
          style = if (isPaired) LunaBadgeStyle.FACT else LunaBadgeStyle.NEUTRAL,
          icon = if (isPaired) Icons.Default.CheckCircle else Icons.Default.Shield,
          modifier = Modifier.fillMaxWidth()
        )

        if (isPaired) {
          Spacer(modifier = Modifier.height(16.dp))
          LunaDestructiveButton(
            text = "Desvincular a mi Pareja",
            onClick = { showRevokeConfirmDialog = true },
            icon = Icons.Default.PersonOff,
            modifier = Modifier.testTag("revoke_partner_button")
          )
        }
      }
    }

    if (showRevokeConfirmDialog) {
      AlertDialog(
        onDismissRequest = { showRevokeConfirmDialog = false },
        icon = {
          Icon(
            imageVector = Icons.Default.PersonOff,
            contentDescription = null,
            tint = StatusError
          )
        },
        title = { Text("¿Desvincular a tu pareja?", fontWeight = FontWeight.Bold) },
        text = {
          Text(
            "Tu pareja perderá de inmediato el acceso a los datos compartidos de tu ciclo y las invitaciones activas serán revocadas."
          )
        },
        confirmButton = {
          Button(
            onClick = {
              showRevokeConfirmDialog = false
              onRevokePairing()
            },
            colors = ButtonDefaults.buttonColors(containerColor = StatusError),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("confirm_revoke_partner_button")
          ) {
            Text("Sí, desvincular", fontWeight = FontWeight.Bold, color = Color.White)
          }
        },
        dismissButton = {
          TextButton(onClick = { showRevokeConfirmDialog = false }) {
            Text("Cancelar")
          }
        }
      )
    }
  }
}
