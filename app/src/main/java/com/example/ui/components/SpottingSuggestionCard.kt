package com.example.ui.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleHealthMenstrual
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5

@Composable
fun SpottingSuggestionCard(
  onAccept: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    shape = RoundedCornerShape(20.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
      .testTag("spotting_suggestion_card")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.Top) {
        Icon(
          imageVector = Icons.Default.Info,
          contentDescription = null,
          tint = AppleHealthMenstrual,
          modifier = Modifier.size(20.dp).padding(top = 2.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "SUGERENCIA DE REGISTRO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = AppleHealthMenstrual,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(3.dp))
          Text(
            text = "Tu pareja notó que comentaste tener manchado. ¿Deseas añadirlo a tu registro de hoy?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 20.sp
          )
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth()
      ) {
        FilledTonalButton(
          onClick = onDismiss,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = AppleSystemGray5.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onSurface
          ),
          modifier = Modifier.testTag("dismiss_spotting_button")
        ) {
          Text("Descartar", fontSize = 13.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.width(10.dp))
        Button(
          onClick = onAccept,
          shape = RoundedCornerShape(12.dp),
          colors = ButtonDefaults.buttonColors(containerColor = AppleSystemBlue),
          modifier = Modifier.testTag("accept_spotting_button")
        ) {
          Text("Añadir al registro", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }
  }
}
