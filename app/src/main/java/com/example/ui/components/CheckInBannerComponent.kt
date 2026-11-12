package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CheckIn
import com.example.data.model.CheckInPromptType
import com.example.data.model.CheckInStatus
import com.example.data.model.QuickResponseOption
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageDarkSoft

fun checkInQuestion(promptType: String): String = when (promptType) {
  CheckInPromptType.HOW_ARE_YOU_FEELING.name -> "¿Cómo te sientes hoy?"
  CheckInPromptType.NEED_SPACE_OR_CUDDLES.name -> "¿Prefieres compañía o un rato para ti?"
  CheckInPromptType.ARE_YOU_UPSET_WITH_ME.name -> "¿Hay algo en lo que te pueda ayudar o prefieres un momento?"
  CheckInPromptType.CAN_I_BRING_YOU_SOMETHING.name -> "¿Puedo prepararte algo o ayudarte con una tarea?"
  else -> "Una pregunta de tu pareja"
}

fun checkInResponseOptions(promptType: String): List<Pair<QuickResponseOption, String>> =
  if (promptType == CheckInPromptType.NEED_SPACE_OR_CUDDLES.name) listOf(
    QuickResponseOption.CUDDLES_PLEASE to "Compañía", QuickResponseOption.NEED_SILENCE to "Un rato sola"
  ) else listOf(
    QuickResponseOption.ALL_GOOD_JUST_CRAMPS to "Todo bien, solo cólicos",
    QuickResponseOption.NEED_SILENCE to "Descansar en silencio",
    QuickResponseOption.LET_US_TALK to "Hablemos un ratito",
    QuickResponseOption.CUDDLES_PLEASE to "Un abrazo y compañía"
  )

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CheckInBannerComponent(
  checkIn: CheckIn?,
  isElla: Boolean,
  onQuickReplySelected: (QuickResponseOption) -> Unit,
  modifier: Modifier = Modifier,
  onOpenReply: ((CheckIn) -> Unit)? = null
) {
  if (checkIn == null) return
  val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
  Surface(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp),
    color = if (dark) LunaSageDarkSoft else LunaSageSoft, contentColor = MaterialTheme.colorScheme.onBackground) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Icon(LunaIcons.Message, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
      Text(if (isElla) "Un mensaje de tu pareja" else "Tu pregunta de apoyo", style = MaterialTheme.typography.titleMedium)
      Text(checkInQuestion(checkIn.promptType), style = MaterialTheme.typography.bodyLarge)
      checkIn.customNote?.takeIf { it.isNotBlank() }?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
      if (checkIn.status == CheckInStatus.PENDING.name) {
        if (isElla) {
          Text("Responder es opcional. Puedes hacerlo a tu ritmo.", style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
          if (onOpenReply != null) LunaButton("Responder a mi pareja", { onOpenReply(checkIn) },
            Modifier.testTag("open_reply_button"))
          else FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            checkInResponseOptions(checkIn.promptType).forEach { (option, label) ->
              LunaOptionChip(label, false, { onQuickReplySelected(option) })
            }
          }
        } else Text("Esperando su respuesta. Puede hacerlo a su ritmo.", style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant)
      } else if (checkIn.status == CheckInStatus.ANSWERED.name) {
        val answer = when (checkIn.responseOption) {
          QuickResponseOption.ALL_GOOD_JUST_CRAMPS.name -> "Todo bien, solo cólicos"
          QuickResponseOption.NEED_SILENCE.name -> "Descansar en silencio"
          QuickResponseOption.LET_US_TALK.name -> "Hablemos un ratito"
          QuickResponseOption.CUDDLES_PLEASE.name -> "Un abrazo y compañía"
          else -> "Respuesta recibida"
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        Text(if (isElla) "Tu respuesta · $answer" else "Su respuesta · $answer", style = MaterialTheme.typography.titleMedium)
        checkIn.responseNote?.takeIf { it.isNotBlank() }?.let {
          Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("checkin_response_note"))
        }
      }
    }
  }
}
