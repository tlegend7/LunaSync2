package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.DayHighlightType
import com.example.data.model.IntimacyLog
import com.example.data.model.PredictedCycle
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseFertileSage
import com.example.ui.theme.PhaseFertileSubtle
import com.example.ui.theme.PhaseFollicularPeach
import com.example.ui.theme.PhaseFollicularSubtle
import com.example.ui.theme.PhaseLutealLavender
import com.example.ui.theme.PhaseLutealSubtle
import com.example.ui.theme.PhaseMenstrualRose
import com.example.ui.theme.PhaseMenstrualSubtle
import com.example.ui.theme.PhaseOvulationGold
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

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
  canLogSymptoms: Boolean = true,
  onDateSelected: (LocalDate) -> Unit,
  onOpenSymptomDialogForDate: (LocalDate) -> Unit,
  onOpenPeriodDialog: (() -> Unit)? = null,
  onOpenPeriodDialogForDate: ((LocalDate) -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val today = remember { LocalDate.now() }
  var displayedMonth by remember { mutableStateOf(YearMonth.from(selectedDate)) }

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

  // Pre-calculate day details for the selected date
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
      .padding(horizontal = 16.dp, vertical = 8.dp)
  ) {
    // Top Overview Summary
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround,
        modifier = Modifier.padding(vertical = 12.dp, horizontal = 14.dp)
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Ciclo Medio", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${averageCycleLength}d", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(AppleHairlineBorder))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Periodo", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("${averagePeriodLength}d", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PhaseMenstrualRose)
        }
        Box(modifier = Modifier.width(1.dp).height(24.dp).background(AppleHairlineBorder))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text("Predicciones", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
          Text("+6 meses", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = PhaseFertileSage)
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Month Navigation Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
    ) {
      IconButton(
        onClick = { displayedMonth = displayedMonth.minusMonths(1) },
        modifier = Modifier.size(36.dp).testTag("prev_month_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowBack,
          contentDescription = "Mes anterior",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(20.dp)
        )
      }

      val monthTitle = displayedMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
      val yearTitle = displayedMonth.year

      Text(
        text = "$monthTitle $yearTitle",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      IconButton(
        onClick = { displayedMonth = displayedMonth.plusMonths(1) },
        modifier = Modifier.size(36.dp).testTag("next_month_button")
      ) {
        Icon(
          Icons.AutoMirrored.Filled.ArrowForward,
          contentDescription = "Mes siguiente",
          tint = MaterialTheme.colorScheme.onSurface,
          modifier = Modifier.size(20.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Interactive Calendar Surface
    Surface(
      shape = RoundedCornerShape(22.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(22.dp))
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        // Weekday header (L, M, M, J, V, S, D)
        Row(
          horizontalArrangement = Arrangement.SpaceAround,
          modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
        ) {
          listOf("L", "M", "M", "J", "V", "S", "D").forEach { dayLabel ->
            Text(
              text = dayLabel,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
              textAlign = TextAlign.Center,
              modifier = Modifier.weight(1f)
            )
          }
        }

        // Calendar Grid Matrix
        val firstDayOfMonth = displayedMonth.atDay(1)
        val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1) // 0 = Monday, 6 = Sunday
        val daysInMonth = displayedMonth.lengthOfMonth()

        val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7
        val weeks = totalCells / 7

        for (week in 0 until weeks) {
          Row(
            horizontalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
          ) {
            for (col in 0..6) {
              val cellIndex = week * 7 + col
              val dayNumber = cellIndex - dayOfWeekOffset + 1

              if (dayNumber in 1..daysInMonth) {
                val cellDate = displayedMonth.atDay(dayNumber)
                val detail = CycleCalculationEngine.getDayDetail(
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

                CalendarDayCell(
                  dayNumber = dayNumber,
                  detail = detail,
                  isToday = cellDate.isEqual(today),
                  isSelected = cellDate.isEqual(selectedDate),
                  onClick = { onDateSelected(cellDate) },
                  modifier = Modifier.weight(1f)
                )
              } else {
                // Dimmed placeholder for padding days
                Box(
                  modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f)
                )
              }
            }
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Legend row
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(10.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
        ) {
          LegendItem(color = PhaseMenstrualRose, label = "Periodo confirmado")
          LegendItem(color = PhaseMenstrualRose, isOutlined = true, label = "Periodo estimado / proyectado")
          LegendItem(color = PhaseFertileSage, label = "Ventana fértil")
          LegendItem(color = PhaseOvulationGold, isStar = true, label = "Ovulación")
          LegendItem(color = PhaseLutealLavender, label = "Síntoma")
          LegendItem(color = PhaseMenstrualRose, isDot = true, label = "Intimidad")
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Selected Day Insight Card
    DayDetailCard(
      detail = selectedDayDetail,
      selectedDate = selectedDate,
      hasCycles = pastCycles.isNotEmpty(),
      todaySymptom = selectedDateSymptom,
      isToday = selectedDate.isEqual(today),
      canLogSymptoms = canLogSymptoms,
      onLogSymptoms = { onOpenSymptomDialogForDate(selectedDate) },
      onOpenPeriodDialog = if (onOpenPeriodDialogForDate != null || onOpenPeriodDialog != null) {
        {
          if (onOpenPeriodDialogForDate != null) {
            onOpenPeriodDialogForDate(selectedDate)
          } else {
            onOpenPeriodDialog?.invoke()
          }
        }
      } else null
    )
  }
}

@Composable
private fun CalendarDayCell(
  dayNumber: Int,
  detail: com.example.data.model.DayCycleDetail,
  isToday: Boolean,
  isSelected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shape = RoundedCornerShape(12.dp)

  // Determine background and text colors based on clinical phase
  val (bgColor, textColor, borderColor) = when (detail.highlightType) {
    DayHighlightType.ACTUAL_PERIOD -> Triple(PhaseMenstrualRose, Color.White, Color.Transparent)
    DayHighlightType.PREDICTED_PERIOD -> Triple(PhaseMenstrualSubtle, PhaseMenstrualRose, PhaseMenstrualRose.copy(alpha = 0.5f))
    DayHighlightType.OVULATION_DAY -> Triple(PhaseFertileSubtle, PhaseFertileSage, PhaseOvulationGold)
    DayHighlightType.FERTILE_WINDOW -> Triple(PhaseFertileSubtle, PhaseFertileSage, Color.Transparent)
    DayHighlightType.FOLLICULAR -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface, Color.Transparent)
    DayHighlightType.LUTEAL -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface, Color.Transparent)
    DayHighlightType.NONE -> Triple(Color.Transparent, MaterialTheme.colorScheme.onSurface, Color.Transparent)
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .aspectRatio(1f)
      .padding(2.dp)
      .clip(shape)
      .background(bgColor)
      .then(
        if (isSelected) {
          Modifier.border(2.dp, BrandTerracottaRose, shape)
        } else if (borderColor != Color.Transparent) {
          Modifier.border(1.dp, borderColor, shape)
        } else if (isToday) {
          Modifier.border(1.5.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f), shape)
        } else {
          Modifier
        }
      )
      .clickable { onClick() }
      .testTag("calendar_day_${dayNumber}")
  ) {
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.Center
    ) {
      // Day Number
      Text(
        text = dayNumber.toString(),
        fontSize = 13.sp,
        fontWeight = if (detail.highlightType == DayHighlightType.ACTUAL_PERIOD || isSelected) FontWeight.Bold else FontWeight.Medium,
        color = textColor
      )

      // Micro indicators for Ovulation, Symptoms, or Intimacy
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 1.dp)
      ) {
        if (detail.highlightType == DayHighlightType.OVULATION_DAY) {
          Box(
            modifier = Modifier
              .size(4.dp)
              .clip(CircleShape)
              .background(PhaseOvulationGold)
          )
        } else if (detail.hasSymptomLogged) {
          Box(
            modifier = Modifier
              .size(4.dp)
              .clip(CircleShape)
              .background(PhaseLutealLavender)
          )
        }

        if (detail.hasIntimacyLogged) {
          Spacer(modifier = Modifier.width(2.dp))
          Box(
            modifier = Modifier
              .size(4.dp)
              .clip(CircleShape)
              .background(PhaseMenstrualRose)
          )
        }
      }
    }
  }
}

@Composable
private fun LegendItem(
  color: Color,
  label: String,
  isOutlined: Boolean = false,
  isStar: Boolean = false,
  isDot: Boolean = false
) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    if (isStar) {
      Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
          .size(12.dp)
          .clip(CircleShape)
          .background(color)
      ) {
        Icon(Icons.Default.Star, contentDescription = null, tint = Color.White, modifier = Modifier.size(8.dp))
      }
    } else if (isDot) {
      Box(
        modifier = Modifier
          .size(8.dp)
          .clip(CircleShape)
          .background(color)
      )
    } else {
      Box(
        modifier = Modifier
          .size(10.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(if (isOutlined) color.copy(alpha = 0.2f) else color)
          .then(if (isOutlined) Modifier.border(1.dp, color, RoundedCornerShape(3.dp)) else Modifier)
      )
    }
    Spacer(modifier = Modifier.width(4.dp))
    Text(
      text = label,
      fontSize = 10.sp,
      color = MaterialTheme.colorScheme.onSurfaceVariant
    )
  }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DayDetailCard(
  detail: com.example.data.model.DayCycleDetail,
  selectedDate: LocalDate,
  hasCycles: Boolean = true,
  todaySymptom: DailySymptom?,
  isToday: Boolean,
  canLogSymptoms: Boolean = true,
  onLogSymptoms: () -> Unit,
  onOpenPeriodDialog: (() -> Unit)? = null,
  modifier: Modifier = Modifier
) {
  val dateTitle = selectedDate.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM", Locale("es", "ES")))
    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

  Surface(
    shape = RoundedCornerShape(22.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(22.dp))
  ) {
    Column(modifier = Modifier.padding(18.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = dateTitle,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Text(
            text = detail.phaseName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when (detail.highlightType) {
              DayHighlightType.ACTUAL_PERIOD -> PhaseMenstrualRose
              DayHighlightType.PREDICTED_PERIOD -> PhaseMenstrualRose
              DayHighlightType.OVULATION_DAY -> PhaseFertileSage
              DayHighlightType.FERTILE_WINDOW -> PhaseFertileSage
              DayHighlightType.FOLLICULAR -> PhaseFollicularPeach
              DayHighlightType.LUTEAL -> PhaseLutealLavender
              DayHighlightType.NONE -> MaterialTheme.colorScheme.onSurfaceVariant
            }
          )
        }

        if (detail.isPredicted) {
          val isFuture = detail.date.isAfter(LocalDate.now())
          val badgeText = if (isFuture) "Proyección Futura" else "Estimación no confirmada"
          val badgeColor = if (detail.highlightType == DayHighlightType.PREDICTED_PERIOD) PhaseMenstrualRose else PhaseFertileSage
          val badgeBg = if (detail.highlightType == DayHighlightType.PREDICTED_PERIOD) PhaseMenstrualSubtle else PhaseFertileSubtle
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = badgeBg,
            modifier = Modifier.border(0.5.dp, badgeColor.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
          ) {
            Text(
              text = badgeText,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold,
              color = badgeColor,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(12.dp))

      // Hormonal / Biological Insight for women
      Surface(
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxWidth().border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(14.dp))
      ) {
        Row(modifier = Modifier.padding(12.dp)) {
          Icon(
            Icons.Default.Spa,
            contentDescription = null,
            tint = BrandTerracottaRose,
            modifier = Modifier.size(18.dp).padding(top = 2.dp)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = detail.hormoneInsight,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp
          )
        }
      }

      // Display logged symptoms if they exist
      if (todaySymptom != null) {
        Spacer(modifier = Modifier.height(14.dp))
        Text(
          text = "REGISTRADO PARA ESTE DÍA:",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(6.dp))
        FlowRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          if (todaySymptom.flowLevel != "NONE") {
            DetailChip(text = "Flujo: ${todaySymptom.flowLevel}", color = PhaseMenstrualRose)
          }
          if (todaySymptom.cramps) {
            DetailChip(text = "Cólicos", color = PhaseMenstrualRose)
          }
          if (todaySymptom.tenderBreasts) {
            DetailChip(text = "Senos sensibles", color = PhaseLutealLavender)
          }
          if (todaySymptom.cravings) {
            DetailChip(text = "Antojos", color = PhaseFollicularPeach)
          }
          if (todaySymptom.cervicalMucus != "NONE") {
            DetailChip(text = "Moco: ${todaySymptom.cervicalMucus}", color = PhaseFertileSage)
          }
          DetailChip(text = "Ánimo: ${todaySymptom.mood}", color = BrandTerracottaRose)
        }
      }

      if (canLogSymptoms) {
        Spacer(modifier = Modifier.height(16.dp))

        // Primary Button for logging
        Button(
          onClick = onLogSymptoms,
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
          modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .testTag("log_symptoms_for_selected_date_button")
        ) {
          Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = if (todaySymptom != null) "Editar registro de este día" else "Registrar síntomas de este día",
            fontWeight = FontWeight.SemiBold
          )
        }

        if (onOpenPeriodDialog != null) {
          Spacer(modifier = Modifier.height(8.dp))
          androidx.compose.material3.OutlinedButton(
            onClick = onOpenPeriodDialog,
            shape = RoundedCornerShape(14.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, PhaseMenstrualRose.copy(alpha = 0.5f)),
            modifier = Modifier
              .fillMaxWidth()
              .height(46.dp)
              .testTag("manage_period_from_calendar_button")
          ) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = PhaseMenstrualRose, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = if (!hasCycles) "Registrar primer periodo" else "Registrar / Corregir inicio de periodo",
              color = PhaseMenstrualRose,
              fontWeight = FontWeight.SemiBold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun DetailChip(text: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.12f),
    modifier = Modifier.border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
  ) {
    Text(
      text = text,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}
