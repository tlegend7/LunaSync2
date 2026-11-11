package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material3.CircularProgressIndicator
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
import com.example.data.model.CareTip
import com.example.data.model.CheckIn
import com.example.data.model.CoupleSpace
import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.IntimacyLog
import com.example.data.model.PartnerObservation
import com.example.ui.components.CheckInBannerComponent
import com.example.ui.components.DailyCareCheatSheetCard
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaNavigationRow
import com.example.ui.theme.LunaBorderLight
import com.example.ui.theme.LunaDarkBorder
import com.example.ui.theme.LunaDarkSurface
import com.example.ui.theme.LunaIceBlueAccent
import com.example.ui.theme.LunaIceBlueSubtle
import com.example.ui.theme.LunaIceBlueText
import com.example.ui.theme.LunaNavyDarkCard
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageText
import com.example.ui.theme.LunaSandSoft
import com.example.ui.theme.LunaSandText
import com.example.ui.theme.LunaSurfaceLight
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import com.example.ui.theme.StatusError
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Pantalla principal de la pareja (Figma: partner · Estar, sin adivinar. #5:677)
 */
@Composable
fun PartnerDashboardView(
  space: CoupleSpace?,
  cycleStatus: CycleStatusData?,
  todaySymptom: DailySymptom?,
  intimacyLogs: List<IntimacyLog>,
  partnerCareTips: List<CareTip>,
  latestCheckIn: CheckIn?,
  partnerObservations: List<PartnerObservation>,
  isLoading: Boolean = false,
  errorMessage: String? = null,
  onOpenCheckInDialog: () -> Unit,
  onOpenObservationDialog: () -> Unit,
  selectedDate: LocalDate = LocalDate.now(),
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val localeEs = Locale("es", "ES")
  val dateTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localeEs)
  val formattedDate = selectedDate.format(dateTitleFormatter)
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }

  val shareSymptomsAllowed = space?.sharingPermissions?.shareSymptoms ?: true
  val shareMoodAllowed = space?.sharingPermissions?.shareMood ?: true
  val shareIntimacyAllowed = space?.sharingPermissions?.shareIntimacy ?: false

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(top = 12.dp, bottom = 100.dp)
  ) {
    // 1. Título y fecha
    Text(
      text = "Estar, sin adivinar.",
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "$formattedDate · Espacio compartido de cuidado.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Resumen compartido Hero card (Figma #5:727)
    Surface(
      shape = RoundedCornerShape(24.dp),
      color = LunaNavyDarkCard,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "INFORMACIÓN COMPARTIDA",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.SemiBold,
          color = LunaIceBlueText,
          letterSpacing = 0.5.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "Su ciclo,\na su manera.",
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = Color.White,
          lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (cycleStatus != null) {
          val nextPeriodEstimate = cycleStatus.estimatedNextPeriodStart
          val nextPeriodStr = if (nextPeriodEstimate != null) {
            val formatterShort = DateTimeFormatter.ofPattern("d 'de' MMMM", localeEs)
            nextPeriodEstimate.format(formatterShort)
          } else {
            "En cálculo"
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "Día ${cycleStatus.currentDay} · Próximo periodo estimado: $nextPeriodStr",
              style = MaterialTheme.typography.bodySmall,
              color = LunaIceBlueText,
              modifier = Modifier.weight(1f)
            )

            LunaBadge(
              text = "Estimación",
              style = LunaBadgeStyle.ESTIMATE
            )
          }
        } else {
          Text(
            text = "Sin estimaciones activas. Tu pareja aún no ha registrado el inicio de su ciclo.",
            style = MaterialTheme.typography.bodySmall,
            color = LunaIceBlueText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 3. Panel: Lo que sabes hoy (Figma Patrón 21 #15:271)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = if (isDark) LunaDarkSurface else LunaSurfaceLight,
      border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(32.dp)
              .clip(RoundedCornerShape(8.dp))
              .background(if (isDark) LunaDarkBorder else LunaSageSoft),
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.Check,
              contentDescription = null,
              tint = LunaSageText,
              modifier = Modifier.size(18.dp)
            )
          }
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "Lo que sabes hoy",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        val symptomSummary = buildString {
          if (!shareSymptomsAllowed) {
            append("Los síntomas físicos no están compartidos según la privacidad elegida.")
          } else if (todaySymptom != null) {
            val items = mutableListOf<String>()
            if (todaySymptom.cramps) items.add("Cólicos leves")
            if (todaySymptom.headache) items.add("Dolor de cabeza")
            if (todaySymptom.backPain) items.add("Molestia lumbar")
            if (todaySymptom.bloating) items.add("Inflamación")
            if (todaySymptom.painLevel > 0) items.add("Dolor ${todaySymptom.painLevel}/5")
            if (!todaySymptom.publicNote.isNullOrBlank()) items.add(todaySymptom.publicNote!!)
            if (items.isNotEmpty()) {
              append(items.joinToString(" · "))
              append(" · Registro compartido.")
            } else {
              append("Sin molestias físicas registradas para hoy.")
            }
          } else {
            append("Sin registros de síntomas ingresados para hoy.")
          }
        }

        Text(
          text = symptomSummary,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Botón principal: Preguntarle cómo está (Check-in) (Figma #5:736)
    LunaButton(
      text = "Preguntarle cómo está",
      onClick = onOpenCheckInDialog,
      type = LunaButtonType.PRIMARY,
      icon = Icons.Default.QuestionAnswer,
      modifier = Modifier.testTag("send_check_in_cta")
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 5. Sección: Una forma de acompañar (Figma #5:738)
    Text(
      text = "Una forma de acompañar",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "Puedes ofrecer ayuda con las tareas o preguntar si prefiere compañía. Su registro no define cómo se siente.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Fila: Anotar un gesto de cuidado (Figma Patrón 22 #15:280)
    LunaNavigationRow(
      title = "Anotar un gesto de cuidado",
      subtitle = "Tus observaciones no cambian su registro.",
      onClick = onOpenObservationDialog,
      icon = Icons.Default.Favorite,
      iconTint = LunaSageText,
      modifier = Modifier.testTag("log_observation_cta")
    )

    // 6. Si hay check-in previo o respuesta de ella
    if (latestCheckIn != null) {
      Spacer(modifier = Modifier.height(16.dp))
      CheckInBannerComponent(
        checkIn = latestCheckIn,
        isElla = false,
        onQuickReplySelected = {}
      )
    }

    // 7. Tarjeta contextual de sugerencias de cuidado diario
    if (partnerCareTips.isNotEmpty()) {
      Spacer(modifier = Modifier.height(16.dp))
      DailyCareCheatSheetCard(
        cyclePhase = cycleStatus?.currentPhase,
        tips = partnerCareTips
      )
    }

    // 8. Panel de privacidad visible: qué datos están reservados
    Spacer(modifier = Modifier.height(16.dp))
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
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = LunaNavyPrimary,
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Text(
          text = "Sus notas privadas y registros íntimos confidenciales solo son visibles para ella.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    if (errorMessage != null) {
      Spacer(modifier = Modifier.height(12.dp))
      Text(
        text = errorMessage,
        style = MaterialTheme.typography.bodySmall,
        color = StatusError
      )
    }
  }
}
