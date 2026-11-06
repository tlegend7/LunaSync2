package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckIn
import com.example.data.model.CheckInPromptType
import com.example.data.model.CheckInStatus
import com.example.data.model.QuickResponseOption
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleHealthFollicularSubtle
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5
import com.example.ui.theme.AppleSystemIndigo

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInBannerComponent(
  checkIn: CheckIn?,
  isElla: Boolean,
  onQuickReplySelected: (QuickResponseOption) -> Unit,
  modifier: Modifier = Modifier
) {
  if (checkIn == null) return

  val promptTitle = when (checkIn.promptType) {
    CheckInPromptType.HOW_ARE_YOU_FEELING.name -> "¿Cómo te sientes en este momento?"
    CheckInPromptType.NEED_SPACE_OR_CUDDLES.name -> "¿Necesitas espacio a solas o mimos?"
    CheckInPromptType.ARE_YOU_UPSET_WITH_ME.name -> "¿Estás molesta conmigo o es el bajón hormonal?"
    CheckInPromptType.CAN_I_BRING_YOU_SOMETHING.name -> "¿Puedo llevarte algo rico o caliente?"
    else -> "¿Cómo estás hoy?"
  }

  // iOS Live Activity / Interactive Notification Card Style
  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(22.dp))
      .testTag("check_in_banner")
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(AppleHealthFollicularSubtle)
        ) {
          Icon(
            imageVector = Icons.Default.QuestionAnswer,
            contentDescription = null,
            tint = AppleSystemBlue,
            modifier = Modifier.size(18.dp)
          )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = if (isElla) "CHECK-IN DE TU PAREJA" else "CHECK-IN ENVIADO",
            style = MaterialTheme.typography.labelSmall,
            color = AppleSystemBlue,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
          )
          Text(
            text = promptTitle,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          if (!checkIn.customNote.isNullOrBlank()) {
            Text(
              text = "\"${checkIn.customNote}\"",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 2.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))

      if (isElla && checkIn.status == CheckInStatus.PENDING.name) {
        Text(
          text = "Responde con un toque:",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          AssistChip(
            onClick = { onQuickReplySelected(QuickResponseOption.ALL_GOOD_JUST_CRAMPS) },
            label = { Text("Todo bien, solo cólicos", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = AssistChipDefaults.assistChipColors(containerColor = AppleSystemGray5.copy(alpha = 0.5f)),
            border = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("reply_cramps")
          )
          AssistChip(
            onClick = { onQuickReplySelected(QuickResponseOption.NEED_SILENCE) },
            label = { Text("Necesito silencio y relax", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = AssistChipDefaults.assistChipColors(containerColor = AppleSystemGray5.copy(alpha = 0.5f)),
            border = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("reply_silence")
          )
          AssistChip(
            onClick = { onQuickReplySelected(QuickResponseOption.CUDDLES_PLEASE) },
            label = { Text("¡Mimos por favor! 💕", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = AssistChipDefaults.assistChipColors(containerColor = AppleSystemGray5.copy(alpha = 0.5f)),
            border = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("reply_cuddles")
          )
          AssistChip(
            onClick = { onQuickReplySelected(QuickResponseOption.LET_US_TALK) },
            label = { Text("Hablemos un ratito", fontSize = 12.sp, fontWeight = FontWeight.Medium) },
            colors = AssistChipDefaults.assistChipColors(containerColor = AppleSystemGray5.copy(alpha = 0.5f)),
            border = null,
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.testTag("reply_talk")
          )
        }
      } else if (checkIn.status == CheckInStatus.ANSWERED.name) {
        val answerText = when (checkIn.responseOption) {
          QuickResponseOption.ALL_GOOD_JUST_CRAMPS.name -> "Todo bien, solo cólicos o cansancio"
          QuickResponseOption.NEED_SILENCE.name -> "Necesito silencio y relax a solas"
          QuickResponseOption.CUDDLES_PLEASE.name -> "¡Mimos por favor! 💕"
          QuickResponseOption.LET_US_TALK.name -> "Hablemos un ratito"
          else -> checkIn.responseOption ?: "Respondido"
        }

        Surface(
          shape = RoundedCornerShape(14.dp),
          color = AppleHealthFollicularSubtle,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Favorite,
              contentDescription = null,
              tint = AppleSystemIndigo,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (isElla) "Respondiste: $answerText" else "Ella respondió: $answerText",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.SemiBold,
              color = AppleSystemIndigo
            )
          }
        }
      } else {
        Text(
          text = "Esperando la respuesta de tu novia con tranquilidad...",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }
  }
}
