package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckIn
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.IntimacyLog
import com.example.data.model.PartnerObservation
import com.example.data.model.QuickResponseOption
import com.example.ui.components.CheckInBannerComponent
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaCard
import com.example.ui.components.LunaNavigationRow
import com.example.ui.components.SpottingSuggestionCard
import com.example.ui.theme.LunaBorderLight
import com.example.ui.theme.LunaCoralSoft
import com.example.ui.theme.LunaCoralText
import com.example.ui.theme.LunaDarkBorder
import com.example.ui.theme.LunaDarkSurface
import com.example.ui.theme.LunaIceBlueAccent
import com.example.ui.theme.LunaIceBlueSubtle
import com.example.ui.theme.LunaIceBlueText
import com.example.ui.theme.LunaNavyDarkCard
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaPeriodRed
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageText
import com.example.ui.theme.LunaSandSoft
import com.example.ui.theme.LunaSandText
import com.example.ui.theme.LunaSurfaceLight
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Pantalla principal de la mujer (Figma: woman · Hoy, a tu ritmo. #5:89)
 */
@Composable
fun EllaDashboardView(
  cycleStatus: CycleStatusData?,
  todaySymptom: DailySymptom?,
  intimacyLogs: List<IntimacyLog>,
  partnerObservations: List<PartnerObservation>,
  latestCheckIn: CheckIn?,
  activeSpottingSuggestion: PartnerObservation?,
  onOpenPeriodDialog: () -> Unit = {},
  onOpenSymptomDialog: () -> Unit,
  onOpenIntimacyDialog: () -> Unit,
  onNavigateToCalendar: () -> Unit,
  onQuickReplySelected: (QuickResponseOption) -> Unit,
  onAcceptSpotting: () -> Unit,
  onDismissSpotting: () -> Unit,
  selectedDate: LocalDate = LocalDate.now(),
  onSelectDate: (LocalDate) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val localeEs = Locale("es", "ES")

  // Formato de fecha para el encabezado: "Jueves, 18 de junio"
  val dateTitleFormatter = DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localeEs)
  val formattedDate = selectedDate.format(dateTitleFormatter)
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }

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
      text = "Hoy, a tu ritmo.",
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = "$formattedDate · Tu registro es personal.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(16.dp))

    // 2. Carrusel semanal horizontal (Figma Patrón 04 / Semana 15–21 junio)
    val startOfWeek = selectedDate.with(DayOfWeek.MONDAY)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      for (i in 0..6) {
        val currentDay = startOfWeek.plusDays(i.toLong())
        val isSelected = currentDay == selectedDate
        val dayLetter = currentDay.dayOfWeek.getDisplayName(TextStyle.NARROW, localeEs).uppercase()
        val dayNumber = currentDay.dayOfMonth.toString()

        val itemBg = if (isSelected) {
          if (isDark) LunaIceBlueAccent else LunaNavyPrimary
        } else {
          if (isDark) LunaDarkSurface else LunaSurfaceLight
        }

        val textColor = if (isSelected) {
          if (isDark) LunaNavyPrimary else Color.White
        } else {
          MaterialTheme.colorScheme.onBackground
        }

        val captionColor = if (isSelected) {
          if (isDark) LunaNavyPrimary else Color.White
        } else {
          MaterialTheme.colorScheme.onSurfaceVariant
        }

        Surface(
          shape = RoundedCornerShape(12.dp),
          color = itemBg,
          border = if (!isSelected) BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight) else null,
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectDate(currentDay) }
        ) {
          Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp)
          ) {
            Text(
              text = dayLetter,
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Medium,
              color = captionColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
              text = dayNumber,
              style = MaterialTheme.typography.titleMedium,
              fontWeight = FontWeight.SemiBold,
              color = textColor
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(20.dp))

    // 3. Hero Card: Resumen del ciclo (Figma #5:161)
    if (cycleStatus == null) {
      // Estado sin historial (Figma #5:1091: empty · Tu historia empieza contigo)
      Surface(
        shape = RoundedCornerShape(24.dp),
        color = LunaNavyDarkCard,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Text(
            text = "TU HISTORIA EMPIEZA CONTIGO",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = LunaIceBlueText,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "Sin fechas inventadas",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Registra el primer día de tu última menstruación para calcular tus fases reales con base biológica.",
            style = MaterialTheme.typography.bodyMedium,
            color = LunaIceBlueText
          )
          Spacer(modifier = Modifier.height(16.dp))
          LunaButton(
            text = "Registrar inicio menstrual",
            onClick = onOpenPeriodDialog,
            type = LunaButtonType.PRIMARY,
            icon = Icons.Default.CalendarMonth,
            modifier = Modifier.testTag("register_period_start_cta")
          )
        }
      }
    } else {
      val dayNum = cycleStatus.currentDay
      val nextPeriodEstimate = cycleStatus.nextPeriodDate
      val nextPeriodStr = try {
        val formatterShort = DateTimeFormatter.ofPattern("d 'de' MMMM", localeEs)
        nextPeriodEstimate.format(formatterShort)
      } catch (e: Exception) {
        "En cálculo"
      }

      Surface(
        shape = RoundedCornerShape(24.dp),
        color = LunaNavyDarkCard,
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(20.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "TU CICLO",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.SemiBold,
              color = LunaIceBlueText,
              letterSpacing = 0.5.sp
            )

            LunaBadge(
              text = if (cycleStatus.isDelayed) "Ciclo Abierto" else "Estimación",
              style = if (cycleStatus.isDelayed) LunaBadgeStyle.FACT else LunaBadgeStyle.ESTIMATE
            )
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = "Día $dayNum",
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Próximo periodo estimado · $nextPeriodStr",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = Color.White
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Las fechas pueden variar. Basado en tus registros.",
            style = MaterialTheme.typography.bodySmall,
            color = LunaIceBlueText
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // 4. Botón principal: Registrar cómo me siento (Figma #5:166)
    LunaButton(
      text = if (cycleStatus == null) "Registrar inicio de ciclo" else "Registrar cómo me siento",
      onClick = {
        if (cycleStatus == null) onOpenPeriodDialog() else onOpenSymptomDialog()
      },
      type = LunaButtonType.PRIMARY,
      icon = Icons.Default.Edit,
      modifier = Modifier.testTag("log_symptoms_cta")
    )

    Spacer(modifier = Modifier.height(24.dp))

    // 5. Encabezado de sección: Tus registros (Figma #5:168)
    Text(
      text = "Tus registros",
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Fila: Menstruación (Figma Patrón 05 #15:107)
    LunaNavigationRow(
      title = "Menstruación",
      subtitle = if (cycleStatus != null) "Ciclo en curso · Día ${cycleStatus.currentDay}" else "Sin registros de inicio",
      onClick = onOpenPeriodDialog,
      icon = Icons.Default.WaterDrop,
      iconTint = LunaPeriodRed,
      modifier = Modifier.testTag("open_period_dialog_row")
    )

    HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)

    // Fila: Intimidad (Figma Patrón 06 #15:118)
    val intimacyCount = intimacyLogs.size
    LunaNavigationRow(
      title = "Intimidad",
      subtitle = if (intimacyCount > 0) "$intimacyCount registros íntimos protegidos" else "Un registro que tú controlas.",
      onClick = onOpenIntimacyDialog,
      icon = Icons.Default.Lock,
      iconTint = LunaNavyPrimary,
      modifier = Modifier.testTag("log_intimacy_cta")
    )

    // Fila: Calendario de acceso rápido
    HorizontalDivider(color = if (isDark) LunaDarkBorder else LunaBorderLight, thickness = 1.dp)

    LunaNavigationRow(
      title = "Tu calendario",
      subtitle = "Consulta tu historial de ciclos y registros",
      onClick = onNavigateToCalendar,
      icon = Icons.Default.CalendarMonth,
      iconTint = LunaSageText,
      modifier = Modifier.testTag("open_calendar_cta")
    )

    // 6. Mensaje de apoyo de la pareja o Check-In activo (Figma Patrón 07 #15:124)
    if (latestCheckIn != null) {
      Spacer(modifier = Modifier.height(16.dp))
      CheckInBannerComponent(
        checkIn = latestCheckIn,
        isElla = true,
        onQuickReplySelected = onQuickReplySelected
      )
    } else if (partnerObservations.isNotEmpty()) {
      val latestObs = partnerObservations.firstOrNull()
      val sweetMsg = latestObs?.sweetMessage
      val careAction = latestObs?.careActionTaken
      if (!sweetMsg.isNullOrBlank() || !careAction.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        Surface(
          shape = RoundedCornerShape(16.dp),
          color = if (isDark) LunaDarkSurface else LunaSageSoft,
          border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaSageSoft),
          modifier = Modifier.fillMaxWidth().testTag("partner_message_card")
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                tint = LunaSageText,
                modifier = Modifier.size(20.dp)
              )
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Un mensaje de tu pareja",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onBackground
              )
            }
            Spacer(modifier = Modifier.height(6.dp))
            val textContent = sweetMsg ?: careAction ?: ""
            Text(
              text = "“$textContent”",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }
    }

    // 7. Tarjeta de Sugerencia de Manchado (si la pareja reportó observación)
    if (activeSpottingSuggestion != null) {
      Spacer(modifier = Modifier.height(16.dp))
      SpottingSuggestionCard(
        onAccept = onAcceptSpotting,
        onDismiss = onDismissSpotting
      )
    }
  }
}
