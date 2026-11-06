package com.example.data.engine

import com.example.data.model.CareTip
import com.example.data.model.CycleEntry
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.data.model.FertileWindow
import com.example.data.model.PredictedCycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

object CycleCalculationEngine {

  /**
   * Computes the weighted average cycle length from past completed cycles.
   * More recent cycles are given progressively higher weight (w_i = i).
   */
  fun calculateWeightedAverage(cycles: List<CycleEntry>, fallbackAverage: Int = 28): Double {
    val validCycles = cycles.mapNotNull { it.cycleLength }
      .filter { it in 20..45 }

    if (validCycles.isEmpty()) return fallbackAverage.toDouble()

    var weightedSum = 0.0
    var totalWeight = 0.0

    validCycles.forEachIndexed { index, length ->
      val weight = (index + 1).toDouble()
      weightedSum += length * weight
      totalWeight += weight
    }

    return weightedSum / totalWeight
  }

  /**
   * Computes sample standard deviation of cycle lengths.
   */
  fun calculateStandardDeviation(cycles: List<CycleEntry>, mean: Double): Double {
    val validCycles = cycles.mapNotNull { it.cycleLength }
      .filter { it in 20..45 }

    if (validCycles.size < 2) return 1.5 // Default normal biological variability

    val sumSquaredDiffs = validCycles.sumOf { (it - mean) * (it - mean) }
    return sqrt(sumSquaredDiffs / (validCycles.size - 1))
  }

  /**
   * Calculates comprehensive cycle status, active phase, fertile window and predictions.
   */
  fun calculateCycleStatus(
    lastPeriodStart: LocalDate,
    averagePeriodDays: Int = 5,
    pastCycles: List<CycleEntry> = emptyList(),
    configuredAverageCycle: Int = 28,
    today: LocalDate = LocalDate.now()
  ): CycleStatusData {
    val meanCycle = calculateWeightedAverage(pastCycles, configuredAverageCycle)
    val roundedCycle = meanCycle.roundToInt()
    val sigma = calculateStandardDeviation(pastCycles, meanCycle)

    val daysSinceStart = ChronoUnit.DAYS.between(lastPeriodStart, today).toInt()
    val currentDay = if (daysSinceStart >= 0) (daysSinceStart % roundedCycle) + 1 else 1

    val nextPeriodDate = lastPeriodStart.plusDays(roundedCycle.toLong())
    val daysUntilNext = ChronoUnit.DAYS.between(today, nextPeriodDate).toInt().coerceAtLeast(0)

    // Standard luteal phase is 14 days before next period
    val estimatedOvulationDate = nextPeriodDate.minusDays(14)
    val sigmaRounded = sigma.roundToInt().coerceAtLeast(0)

    val fertileWindow = FertileWindow(
      startDate = estimatedOvulationDate.minusDays((5 + sigmaRounded).toLong()),
      endDate = estimatedOvulationDate.plusDays((1 + sigmaRounded).toLong()),
      ovulationDate = estimatedOvulationDate
    )

    val estimatedOvulationDay = roundedCycle - 14
    val currentPhase = when {
      currentDay in 1..averagePeriodDays -> CyclePhase.MENSTRUAL
      currentDay in (averagePeriodDays + 1)..(estimatedOvulationDay - 2) -> CyclePhase.FOLLICULAR
      currentDay in (estimatedOvulationDay - 1)..(estimatedOvulationDay + 1) -> CyclePhase.OVULATORY
      else -> CyclePhase.LUTEAL
    }

    return CycleStatusData(
      currentDay = currentDay,
      totalDays = roundedCycle,
      currentPhase = currentPhase,
      daysUntilNextPeriod = daysUntilNext,
      fertileWindow = fertileWindow,
      nextPeriodDate = nextPeriodDate,
      confidenceIntervalDays = 1.64 * sigma
    )
  }

  /**
   * Generates empathetic, actionable care recommendations for the partner based on active phase.
   */
  fun getPartnerCareTips(phase: CyclePhase, currentDay: Int): List<CareTip> {
    return when (phase) {
      CyclePhase.MENSTRUAL -> listOf(
        CareTip(
          title = "Confort e Hidratación",
          description = "Prepara infusiones calientes (manzanilla, jengibre) o ten lista una bolsa de agua tibia para calmar cólicos.",
          categoryIcon = "tea"
        ),
        CareTip(
          title = "Alivio Físico y Tensión",
          description = "Ofrece un suave masaje en la espalda baja o los pies sin que tenga que pedírtelo.",
          categoryIcon = "massage"
        ),
        CareTip(
          title = "Baja Demanda y Espacio",
          description = "Sus niveles de energía están en el punto más bajo. Prioriza planes tranquilos en casa y descanso.",
          categoryIcon = "heart"
        )
      )

      CyclePhase.FOLLICULAR -> listOf(
        CareTip(
          title = "Energía y Entusiasmo",
          description = "El estrógeno va en aumento. Es un momento ideal para proponer citas, proyectos creativos o ejercicio juntos.",
          categoryIcon = "activity"
        ),
        CareTip(
          title = "Apoyo Proactivo",
          description = "Se siente con mayor claridad mental y motivación. Sé su fan número uno en sus metas de la semana.",
          categoryIcon = "heart"
        ),
        CareTip(
          title = "Nutrición Ligera",
          description = "Prefiere comidas frescas, ricas en vegetales y proteínas magras que acompañen su vitalidad.",
          categoryIcon = "food"
        )
      )

      CyclePhase.OVULATORY -> listOf(
        CareTip(
          title = "Pico de Fertilidad y Conexión",
          description = "Ventana de máxima fertilidad y deseo. Mayor lubricación natural. Ideal para conectar con amor y calma.",
          categoryIcon = "star"
        ),
        CareTip(
          title = "Cuidado del pH y Flora",
          description = "Manos siempre muy limpias antes de caricias íntimas para cuidar sus lactobacilos. Si usan lubricante, elijan uno con pH balanceado.",
          categoryIcon = "heart"
        ),
        CareTip(
          title = "Comunicación Abierta",
          description = "Conversen con empatía sobre sus planes compartidos y expectativas íntimas con confianza mutua.",
          categoryIcon = "chat"
        )
      )

      CyclePhase.LUTEAL -> listOf(
        CareTip(
          title = "Sensibilidad de Tejidos y Cuello",
          description = "El cuello uterino desciende y está más sensible. Tras intimidad o tocamientos puede haber ligero manchado por roce; sé muy suave.",
          categoryIcon = "heart"
        ),
        CareTip(
          title = "Antojos y Magnesio",
          description = "Los antojos de chocolate negro o frutos secos son normales por la caída de serotonina. ¡Sorpréndela con su snack favorito!",
          categoryIcon = "food"
        ),
        CareTip(
          title = "Paciencia y Aligerar su Carga",
          description = "Toma la iniciativa con tareas domésticas o prepara la cena para que pueda desconectar y descansar.",
          categoryIcon = "massage"
        )
      )
    }
  }

  /**
   * Projects the next 3 to 6 future menstrual cycles with confidence bands based on historical deviation.
   * As months progress, the confidence interval expands gracefully (sigma * sqrt(k)).
   */
  fun predictFutureCycles(
    lastPeriodStart: LocalDate,
    averagePeriodDays: Int = 5,
    pastCycles: List<CycleEntry> = emptyList(),
    configuredAverageCycle: Int = 28,
    cyclesCount: Int = 6
  ): List<PredictedCycle> {
    val meanCycle = calculateWeightedAverage(pastCycles, configuredAverageCycle)
    val roundedCycle = meanCycle.roundToInt().coerceIn(21, 40)
    val sigma = calculateStandardDeviation(pastCycles, meanCycle)

    val predictions = mutableListOf<PredictedCycle>()
    var currentPeriodStart = lastPeriodStart

    for (k in 1..cyclesCount) {
      val nextPeriodStart = currentPeriodStart.plusDays(roundedCycle.toLong())
      val nextPeriodEnd = nextPeriodStart.plusDays((averagePeriodDays - 1).coerceAtLeast(1).toLong())
      val ovulationDate = nextPeriodStart.plusDays(roundedCycle.toLong() - 14)
      val sigmaSpread = (sigma * sqrt(k.toDouble())).roundToInt().coerceAtLeast(1)

      predictions.add(
        PredictedCycle(
          cycleNumber = k,
          expectedStartDate = nextPeriodStart,
          expectedEndDate = nextPeriodEnd,
          expectedOvulationDate = ovulationDate,
          fertileWindowStart = ovulationDate.minusDays(5 + sigmaSpread.toLong()),
          fertileWindowEnd = ovulationDate.plusDays(1 + sigmaSpread.toLong()),
          confidenceDays = 1.64 * sigma * sqrt(k.toDouble())
        )
      )
      currentPeriodStart = nextPeriodStart
    }
    return predictions
  }

  /**
   * Evaluates any date (past, current, or future) to determine its clinical cycle phase,
   * highlight marker for the calendar, and empathetic hormonal insights for women.
   */
  fun getDayDetail(
    date: LocalDate,
    lastPeriodStart: LocalDate,
    averagePeriodDays: Int = 5,
    pastCycles: List<CycleEntry> = emptyList(),
    configuredAverageCycle: Int = 28,
    today: LocalDate = LocalDate.now(),
    symptomDates: Set<LocalDate> = emptySet(),
    intimacyDates: Set<LocalDate> = emptySet()
  ): com.example.data.model.DayCycleDetail {
    val meanCycle = calculateWeightedAverage(pastCycles, configuredAverageCycle)
    val roundedCycle = meanCycle.roundToInt().coerceIn(21, 40)
    val hasSymptom = symptomDates.contains(date)
    val hasIntimacy = intimacyDates.contains(date)

    // Check past / current cycle
    val currentPeriodEnd = lastPeriodStart.plusDays((averagePeriodDays - 1).toLong())
    val currentNextPeriod = lastPeriodStart.plusDays(roundedCycle.toLong())
    val currentOvulation = currentNextPeriod.minusDays(14)
    val currentFertileStart = currentOvulation.minusDays(5)
    val currentFertileEnd = currentOvulation.plusDays(1)

    // 1. Is it during the active/recent period?
    if (!date.isBefore(lastPeriodStart) && !date.isAfter(currentPeriodEnd)) {
      val dayNum = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt() + 1
      return com.example.data.model.DayCycleDetail(
        date = date,
        cycleDay = dayNum,
        highlightType = com.example.data.model.DayHighlightType.ACTUAL_PERIOD,
        phaseName = "Menstruación (Día $dayNum)",
        hormoneInsight = "Tus niveles de estrógeno y progesterona están en su punto basal. El endometrio se renueva. Dale a tu cuerpo descanso, hidratación tibia y alimentos reconfortantes.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = false
      )
    }

    // 2. Is it in the current ovulation date?
    if (date.isEqual(currentOvulation)) {
      val dayNum = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt() + 1
      return com.example.data.model.DayCycleDetail(
        date = date,
        cycleDay = dayNum,
        highlightType = com.example.data.model.DayHighlightType.OVULATION_DAY,
        phaseName = "Día de Ovulación (Pico Fértil)",
        hormoneInsight = "Pico de la hormona LH (Luteinizante) y estrógeno. El óvulo es liberado. Suele presentarse moco cervical tipo clara de huevo, mayor líbido y resplandor en la piel.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = date.isAfter(today)
      )
    }

    // 3. Is it in the current fertile window?
    if (!date.isBefore(currentFertileStart) && !date.isAfter(currentFertileEnd)) {
      val dayNum = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt() + 1
      return com.example.data.model.DayCycleDetail(
        date = date,
        cycleDay = dayNum,
        highlightType = com.example.data.model.DayHighlightType.FERTILE_WINDOW,
        phaseName = "Ventana Fértil",
        hormoneInsight = "El estrógeno continúa elevándose a medida que maduran los folículos. Los espermatozoides pueden sobrevivir hasta 5 días en el tracto reproductivo.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = date.isAfter(today)
      )
    }

    // 4. Check future predicted cycles
    val futurePredictions = predictFutureCycles(
      lastPeriodStart = lastPeriodStart,
      averagePeriodDays = averagePeriodDays,
      pastCycles = pastCycles,
      configuredAverageCycle = configuredAverageCycle,
      cyclesCount = 6
    )

    for (pred in futurePredictions) {
      if (!date.isBefore(pred.expectedStartDate) && !date.isAfter(pred.expectedEndDate)) {
        val dayNum = ChronoUnit.DAYS.between(pred.expectedStartDate, date).toInt() + 1
        return com.example.data.model.DayCycleDetail(
          date = date,
          cycleDay = dayNum,
          highlightType = com.example.data.model.DayHighlightType.PREDICTED_PERIOD,
          phaseName = "Periodo Estimado (Ciclo #${pred.cycleNumber})",
          hormoneInsight = "Inicio estimado del sangrado menstrual proyectado con tu histórico regular de $roundedCycle días (±${String.format("%.1f", pred.confidenceDays)}d).",
          hasSymptomLogged = hasSymptom,
          hasIntimacyLogged = hasIntimacy,
          isPredicted = true
        )
      }

      if (date.isEqual(pred.expectedOvulationDate)) {
        return com.example.data.model.DayCycleDetail(
          date = date,
          cycleDay = roundedCycle - 14,
          highlightType = com.example.data.model.DayHighlightType.OVULATION_DAY,
          phaseName = "Ovulación Proyectada (Ciclo #${pred.cycleNumber})",
          hormoneInsight = "Día clave proyectado para la ovulación futura. Esta fecha se reajustará de inmediato si registras cambios en tus periodos previos.",
          hasSymptomLogged = hasSymptom,
          hasIntimacyLogged = hasIntimacy,
          isPredicted = true
        )
      }

      if (!date.isBefore(pred.fertileWindowStart) && !date.isAfter(pred.fertileWindowEnd)) {
        return com.example.data.model.DayCycleDetail(
          date = date,
          cycleDay = null,
          highlightType = com.example.data.model.DayHighlightType.FERTILE_WINDOW,
          phaseName = "Ventana Fértil Estimada",
          hormoneInsight = "Días de alta probabilidad biológica de concepción proyectados para tu ciclo #${pred.cycleNumber}.",
          hasSymptomLogged = hasSymptom,
          hasIntimacyLogged = hasIntimacy,
          isPredicted = true
        )
      }
    }

    // 5. Default follicular or luteal phase calculation
    val daysFromStart = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt()
    val normalizedDay = if (daysFromStart >= 0) (daysFromStart % roundedCycle) + 1 else 1
    val isFollicular = normalizedDay < (roundedCycle - 14)

    return com.example.data.model.DayCycleDetail(
      date = date,
      cycleDay = normalizedDay,
      highlightType = if (isFollicular) com.example.data.model.DayHighlightType.FOLLICULAR else com.example.data.model.DayHighlightType.LUTEAL,
      phaseName = if (isFollicular) "Fase Folicular (Renovación)" else "Fase Lútea (Calma & Autocuidado)",
      hormoneInsight = if (isFollicular) {
        "El estrógeno va en ascenso. Suele acompañarse de energía creciente, claridad mental, optimismo y resistencia física."
      } else {
        "La progesterona toma el protagonismo. Es natural sentir necesidad de introspección, alimentos ricos en magnesio y un ritmo más pausado."
      },
      hasSymptomLogged = hasSymptom,
      hasIntimacyLogged = hasIntimacy,
      isPredicted = date.isAfter(today)
    )
  }
}
