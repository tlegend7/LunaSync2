package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.engine.CycleCalculationEngine
import com.example.data.model.CycleEntry
import com.example.data.model.DailySymptom
import com.example.data.model.DayHighlightType
import com.example.data.model.IntimacyLog
import com.example.data.model.PredictedCycle
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaButton
import com.example.ui.components.LunaButtonType
import com.example.ui.components.LunaCard
import com.example.ui.theme.LunaBorderLight
import com.example.ui.theme.LunaCoralSoft
import com.example.ui.theme.LunaCoralText
import com.example.ui.theme.LunaDarkBorder
import com.example.ui.theme.LunaDarkSurface
import com.example.ui.theme.LunaIceBlueAccent
import com.example.ui.theme.LunaIceBlueSubtle
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaPeriodRed
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageText
import com.example.ui.theme.LunaSandSoft
import com.example.ui.theme.LunaSandText
import com.example.ui.theme.LunaSurfaceLight
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Vista de Calendario mensual según Figma (calendar · Tu calendario #5:195)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CycleCalendarView(
  lastPeriodStart: LocalDate?,
  averagePeriodLength: Int,
  averageCycleLength: Int,
  pastCycles: List<CycleEntry>,
  symptoms: List<DailySymptom>,
  intimacyLogs: List<IntimacyLog>,
  predictedCycles: List<PredictedCycle>,
  selectedDate: LocalDate = LocalDate.now(),
  today: LocalDate = LocalDate.now(),
  canLogSymptoms: Boolean = true,
  onDateSelected: (LocalDate) -> Unit,
  onOpenSymptomDialogForDate: (LocalDate) -> Unit,
  onOpenPeriodDialog: (() -> Unit)? = null,
  onOpenPeriodDialogForDate: ((LocalDate) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val localeEs = Locale("es", "ES")

  var displayedMonth by remember(selectedDate.year, selectedDate.month) {
    mutableStateOf(YearMonth.from(selectedDate))
  }

  val symptomDates = remember(symptoms) {
    symptoms.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()
  }

  val intimacyDates = remember(intimacyLogs) {
    intimacyLogs.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()
  }

  val confirmedBleedingDates = remember(symptoms) {
    symptoms.filter { it.flowLevel in listOf("LIGHT", "MEDIUM", "HEAVY") }.mapNotNull {
      try { LocalDate.parse(it.date) } catch (e: Exception) { null }
    }.toSet()
  }

  val selectedDayDetail by remember(selectedDate, lastPeriodStart, pastCycles, symptoms, intimacyLogs) {
    derivedStateOf {
      CycleCalculationEngine.getDayDetail(
        date = selectedDate,
        lastPeriodStart = lastPeriodStart,
        averagePeriodDays = averagePeriodLength,
        pastCycles = pastCycles,
        configuredAverageCycle = averageCycleLength,
        today = today,
        symptomDates = symptomDates,
        intimacyDates = intimacyDates,
        confirmedBleedingDates = confirmedBleedingDates
      )
    }
  }

  val selectedDateSymptom = remember(selectedDate, symptoms) {
    val dateStr = selectedDate.toString()
    symptoms.firstOrNull { it.date == dateStr }
  }

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
      text = if (canLogSymptoms) "Tu calendario" else "Calendario compartido",
      style = MaterialTheme.typography.displaySmall,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(4.dp))

    Text(
      text = if (canLogSymptoms) "Selecciona un día para consultar o registrar." else "Consulta el historial compartido por tu pareja.",
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )

    Spacer(modifier = Modifier.height(20.dp))

    // 2. Cabecera del Mes (Figma #5:245)
    val monthTitle = displayedMonth.month.getDisplayName(TextStyle.FULL, localeEs).replaceFirstChar { it.titlecase(localeEs) }
    val yearTitle = displayedMonth.year.toString()

    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = "$monthTitle $yearTitle",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
      )

      Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
          onClick = { displayedMonth = displayedMonth.minusMonths(1) },
          modifier = Modifier.size(36.dp).testTag("prev_month_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Mes anterior",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
          )
        }

        IconButton(
          onClick = { displayedMonth = displayedMonth.plusMonths(1) },
          modifier = Modifier.size(36.dp).testTag("next_month_button")
        ) {
          Icon(
            Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Mes siguiente",
            tint = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.size(20.dp)
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 3. Días de la semana (L, M, X, J, V, S, D) (Figma #5:251)
    val dayLabels = listOf("L", "M", "X", "J", "V", "S", "D")
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
      dayLabels.forEach { label ->
        Text(
          text = label,
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Medium,
          color = LunaTextSecondary,
          textAlign = TextAlign.Center,
          modifier = Modifier.weight(1f)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // 4. Cuadrícula de Días (Semanas 1..6)
    val firstDayOfMonth = displayedMonth.atDay(1)
    val daysInMonth = displayedMonth.lengthOfMonth()
    val firstDayOfWeekIndex = firstDayOfMonth.dayOfWeek.value - 1 // 0: Lunes, 6: Domingo
    val totalCells = ((firstDayOfWeekIndex + daysInMonth + 6) / 7) * 7

    for (weekRow in 0 until (totalCells / 7)) {
      Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp)
      ) {
        for (col in 0..6) {
          val cellIndex = weekRow * 7 + col
          val dayNumber = cellIndex - firstDayOfWeekIndex + 1

          if (dayNumber in 1..daysInMonth) {
            val cellDate = displayedMonth.atDay(dayNumber)
            val isSelected = cellDate == selectedDate
            val isTodayDate = cellDate == today

            val cellDetail = remember(cellDate, lastPeriodStart, pastCycles, symptoms, intimacyLogs) {
              CycleCalculationEngine.getDayDetail(
                date = cellDate,
                lastPeriodStart = lastPeriodStart,
                averagePeriodDays = averagePeriodLength,
                pastCycles = pastCycles,
                configuredAverageCycle = averageCycleLength,
                today = today,
                symptomDates = symptomDates,
                intimacyDates = intimacyDates,
                confirmedBleedingDates = confirmedBleedingDates
              )
            }

            CalendarFigmaCell(
              dayNumber = dayNumber,
              detail = cellDetail,
              isToday = isTodayDate,
              isSelected = isSelected,
              isDark = isDark,
              onClick = {
                onDateSelected(cellDate)
              },
              modifier = Modifier.weight(1f)
            )
          } else {
            Spacer(modifier = Modifier.weight(1f))
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // 5. Leyenda según Figma (#5:331)
    Row(
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      LunaBadge(
        text = "Registrado",
        backgroundColor = LunaCoralSoft,
        textColor = LunaPeriodRed,
        shape = RoundedCornerShape(12.dp)
      )

      LunaBadge(
        text = "Estimado",
        backgroundColor = LunaSandSoft,
        textColor = LunaSandText,
        shape = RoundedCornerShape(12.dp)
      )

      LunaBadge(
        text = "Solo tú",
        backgroundColor = LunaIceBlueSubtle,
        textColor = LunaNavyPrimary,
        shape = RoundedCornerShape(12.dp)
      )
    }

    Spacer(modifier = Modifier.height(24.dp))

    // 6. Detalle del Día Seleccionado (Figma #5:336)
    val selectedDateStr = selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", localeEs))
      .replaceFirstChar { if (it.isLowerCase()) it.titlecase(localeEs) else it.toString() }

    Text(
      text = selectedDateStr,
      style = MaterialTheme.typography.titleLarge,
      fontWeight = FontWeight.SemiBold,
      color = MaterialTheme.colorScheme.onBackground
    )

    Spacer(modifier = Modifier.height(10.dp))

    // Panel: Cómo te sentiste (Figma Patrón 09 #15:162)
    Surface(
      shape = RoundedCornerShape(16.dp),
      color = if (isDark) LunaDarkSurface else LunaSurfaceLight,
      border = BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(
            imageVector = Icons.Default.Tune,
            contentDescription = null,
            tint = if (isDark) LunaIceBlueAccent else LunaNavyPrimary,
            modifier = Modifier.size(20.dp)
          )
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "Cómo te sentiste",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
          )
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (selectedDateSymptom != null) {
          val symptomItems = mutableListOf<String>()
          if (selectedDateSymptom.cramps) symptomItems.add("Cólicos leves")
          if (selectedDateSymptom.headache) symptomItems.add("Cefalea")
          if (selectedDateSymptom.backPain) symptomItems.add("Dolor lumbar")
          if (selectedDateSymptom.bloating) symptomItems.add("Inflamación")
          if (selectedDateSymptom.painLevel > 0) symptomItems.add("Dolor ${selectedDateSymptom.painLevel}/5")
          if (selectedDateSymptom.energyLevel > 0) symptomItems.add("Energía ${selectedDateSymptom.energyLevel}/5")
          if (!selectedDateSymptom.publicNote.isNullOrBlank()) symptomItems.add(selectedDateSymptom.publicNote!!)

          val summaryText = if (symptomItems.isNotEmpty()) symptomItems.joinToString(" · ") else "Sin molestias físicas registradas"
          Text(
            text = summaryText,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onBackground
          )

          Spacer(modifier = Modifier.height(4.dp))

          Text(
            text = "Solo tú puedes ver tu nota privada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          Text(
            text = "Sin síntomas registrados para esta fecha.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Botón de acción para el día seleccionado
    if (canLogSymptoms) {
      val isSymptomLogged = selectedDateSymptom != null
      LunaButton(
        text = if (isSymptomLogged) "Editar registro del día" else "Registrar síntomas de este día",
        onClick = { onOpenSymptomDialogForDate(selectedDate) },
        type = if (isSymptomLogged) LunaButtonType.SECONDARY else LunaButtonType.PRIMARY,
        icon = Icons.Default.Edit,
        modifier = Modifier.testTag(if (isSymptomLogged) "edit_day_symptoms_cta" else "log_symptoms_for_date_cta")
      )

      if (onOpenPeriodDialogForDate != null || onOpenPeriodDialog != null) {
        Spacer(modifier = Modifier.height(8.dp))
        LunaButton(
          text = "Registrar periodo en esta fecha",
          onClick = {
            if (onOpenPeriodDialogForDate != null) {
              onOpenPeriodDialogForDate(selectedDate)
            } else {
              onOpenPeriodDialog?.invoke()
            }
          },
          type = LunaButtonType.SECONDARY,
          icon = Icons.Default.CalendarMonth,
          modifier = Modifier.testTag("register_period_for_date_cta")
        )
      }
    }
  }
}

/**
 * Celda de día individual del calendario siguiendo exactamente el diseño de Figma #5:260
 */
@Composable
private fun CalendarFigmaCell(
  dayNumber: Int,
  detail: com.example.data.model.DayCycleDetail,
  isToday: Boolean,
  isSelected: Boolean,
  isDark: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(12.dp)

  // Color de fondo y borde según highlightType
  val isActualPeriod = detail.highlightType == DayHighlightType.ACTUAL_PERIOD
  val isPredictedPeriod = detail.highlightType == DayHighlightType.PREDICTED_PERIOD

  val cellBg = when {
    isSelected -> if (isDark) LunaIceBlueAccent else LunaNavyPrimary
    isActualPeriod -> LunaCoralSoft
    isPredictedPeriod -> Color.Transparent
    else -> Color.Transparent
  }

  val textColor = when {
    isSelected -> if (isDark) LunaNavyPrimary else Color.White
    isActualPeriod -> LunaPeriodRed
    isPredictedPeriod -> LunaPeriodRed
    else -> if (isDark) LunaTextPrimary else MaterialTheme.colorScheme.onBackground
  }

  val border = when {
    isSelected -> null
    isPredictedPeriod -> BorderStroke(1.dp, LunaPeriodRed)
    isToday -> BorderStroke(1.dp, if (isDark) LunaIceBlueAccent else LunaNavyPrimary)
    else -> null
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .aspectRatio(1f)
      .clip(shape)
      .background(cellBg)
      .then(if (border != null) Modifier.border(border, shape) else Modifier)
      .clickable(onClick = onClick)
      .testTag("calendar_day_$dayNumber")
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      Text(
        text = dayNumber.toString(),
        style = MaterialTheme.typography.bodyLarge,
        fontWeight = if (isSelected || isActualPeriod) FontWeight.SemiBold else FontWeight.Normal,
        color = textColor
      )

      // Indicadores puntuales (manchado, síntoma, intimidad)
      if (detail.hasSymptomLogged || detail.hasIntimacyLogged) {
        Spacer(modifier = Modifier.height(2.dp))
        Row(
          horizontalArrangement = Arrangement.Center,
          verticalAlignment = Alignment.CenterVertically
        ) {
          if (detail.hasSymptomLogged) {
            Box(
              modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) (if (isDark) LunaNavyPrimary else Color.White) else LunaSageText)
            )
          }
          if (detail.hasIntimacyLogged) {
            Spacer(modifier = Modifier.width(2.dp))
            Box(
              modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(if (isSelected) (if (isDark) LunaNavyPrimary else Color.White) else LunaPeriodRed)
            )
          }
        }
      }
    }
  }
}
