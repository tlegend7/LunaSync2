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
import com.example.ui.theme.DeepOceanBlue
import com.example.ui.theme.IceBlue
import com.example.ui.theme.IceBlueSubtle
import com.example.ui.theme.SlateNavy
import com.example.ui.theme.SoftCerulean

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
      text = if (currentRole == UserRole.SUPPORT_PARTNER) "Vincular con mi Novia" else "Invitar a mi Pareja",
      style = MaterialTheme.typography.headlineMedium,
      fontWeight = FontWeight.Bold,
      color = SlateNavy
    )
    Text(
      text = if (currentRole == UserRole.SUPPORT_PARTNER) {
        "Ingresa el código que te compartió tu novia para sincronizar su ciclo y acceder al dashboard de cuidados"
      } else {
        "Genera un código único para que tu novio pueda acompañarte y apoyarte de forma empática y consensuada"
      },
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(24.dp))

    if (currentRole == UserRole.SUPPORT_PARTNER) {
      // VISTA EXCLUSIVA DEL NOVIO
      if (isPaired) {
        // Novio YA vinculado exitosamente
        Surface(
          shape = RoundedCornerShape(26.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftCerulean.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(70.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9))
            ) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(40.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "¡Espacio Sincronizado!",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = SlateNavy
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = "Estás vinculado con tu novia. Desde el Dashboard puedes ver su fase hormonal actual, recibir recomendaciones de apoyo ('Cheat Sheet') y enviarle Check-ins afectivos.",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            Surface(
              shape = RoundedCornerShape(14.dp),
              color = IceBlueSubtle,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Favorite,
                  contentDescription = null,
                  tint = DeepOceanBlue,
                  modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                  text = "Estado: Conexión Activa y Segura",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = DeepOceanBlue
                )
              }
            }
          }
        }
      } else {
        // Novio NO vinculado: Formulario directo para ingresar código de su novia
        Surface(
          shape = RoundedCornerShape(26.dp),
          color = MaterialTheme.colorScheme.surface,
          modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftCerulean.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp)
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(IceBlue)
            ) {
              Icon(
                imageVector = Icons.Default.Key,
                contentDescription = null,
                tint = DeepOceanBlue,
                modifier = Modifier.size(32.dp)
              )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
              text = "Ingresar Código de Pareja",
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )

            Text(
              text = "Pídele a tu novia que abra LunaSync en la pestaña 'Vincular' y te comparta su código de 8 caracteres (ej. LUNA-8X4K):",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              textAlign = TextAlign.Center,
              modifier = Modifier.padding(vertical = 8.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
              value = inputCode,
              onValueChange = { inputCode = it.uppercase() },
              label = { Text("Código de tu novia") },
              placeholder = { Text("LUNA-XXXX") },
              singleLine = true,
              shape = RoundedCornerShape(14.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("pairing_code_input")
            )

            Spacer(modifier = Modifier.height(20.dp))

            Button(
              onClick = { onJoinCode(inputCode) },
              enabled = inputCode.isNotBlank() && !isLoading,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = DeepOceanBlue),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("submit_join_code_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
              } else {
                Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Vincularme con mi Novia", fontWeight = FontWeight.Bold)
              }
            }
          }
        }
      }
    } else {
      // VISTA EXCLUSIVA DE ELLA (ADMINISTRADORA DEL ESPACIO)
      Surface(
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
          .fillMaxWidth()
          .border(1.dp, SoftCerulean.copy(alpha = 0.4f), RoundedCornerShape(26.dp))
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier.padding(24.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(64.dp)
              .clip(CircleShape)
              .background(IceBlue)
          ) {
            Icon(
              imageVector = Icons.Default.QrCode,
              contentDescription = null,
              tint = DeepOceanBlue,
              modifier = Modifier.size(32.dp)
            )
          }

          Spacer(modifier = Modifier.height(16.dp))

          Text(
            text = "Tu Código de Invitación",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = "Tu novio debe ingresar este código en su app para conectarse a tu espacio:",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 6.dp)
          )

          Spacer(modifier = Modifier.height(16.dp))

          if (!activeCode.isNullOrBlank()) {
            Surface(
              shape = RoundedCornerShape(16.dp),
              color = IceBlue,
              modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, SoftCerulean, RoundedCornerShape(16.dp))
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
                  color = DeepOceanBlue
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
              text = "Válido por 48 horas • Puedes revocar o cambiar permisos cuando quieras",
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          } else {
            Button(
              onClick = onGenerateCode,
              enabled = !isLoading,
              shape = RoundedCornerShape(14.dp),
              colors = ButtonDefaults.buttonColors(containerColor = DeepOceanBlue),
              modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("generate_pairing_code_button")
            ) {
              if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White)
              } else {
                Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generar Código para mi Novio", fontWeight = FontWeight.Bold)
              }
            }
          }

          Spacer(modifier = Modifier.height(20.dp))

          // Badge de Estado de Conexión
          Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (isPaired) Color(0xFFE8F5E9) else IceBlueSubtle,
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
              Icon(
                imageVector = if (isPaired) Icons.Default.CheckCircle else Icons.Default.Shield,
                contentDescription = null,
                tint = if (isPaired) Color(0xFF2E7D32) else DeepOceanBlue,
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = if (isPaired) "¡Tu novio está conectado a tu espacio!" else "Esperando a que tu novio ingrese el código...",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                color = if (isPaired) Color(0xFF2E7D32) else SlateNavy
              )
            }
          }

          if (isPaired) {
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(
              onClick = { showRevokeConfirmDialog = true },
              border = BorderStroke(1.dp, Color(0xFFEF9A9A)),
              shape = RoundedCornerShape(12.dp),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("revoke_partner_button")
            ) {
              Icon(
                imageVector = Icons.Default.PersonOff,
                contentDescription = null,
                tint = Color(0xFFC62828),
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Desvincular a mi Pareja",
                color = Color(0xFFC62828),
                fontWeight = FontWeight.SemiBold
              )
            }
          }
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
            tint = Color(0xFFC62828)
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
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFC62828)),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.testTag("confirm_revoke_partner_button")
          ) {
            Text("Sí, desvincular", fontWeight = FontWeight.Bold)
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
