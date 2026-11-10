package com.example.data.engine

import com.example.data.model.CareTip
import com.example.data.model.CycleEntry
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.data.model.DayCycleDetail
import com.example.data.model.DayHighlightType
import com.example.data.model.FertileWindow
import com.example.data.model.PredictedCycle
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * Motor clínico y predictivo unificado para el seguimiento del ciclo menstrual.
 *
 * Principios rectores del Bloque 2:
 * 1. Los registros históricos confirmados por la usuaria son la fuente canónica de verdad.
 * 2. Un ciclo en curso no tiene duración conocida hasta que se registra el siguiente inicio (cycleLength == null).
 * 3. El día del ciclo (Día 1 en adelante) se cuenta desde el último inicio real sin reinicios automáticos por módulo.
 * 4. Mayor peso determinista a los ciclos completos más recientes, ordenando explícitamente por fecha.
 * 5. Conteo inclusivo consistente para periodos de sangrado (duración de 1 día abarca únicamente una fecha).
 * 6. Política unificada de límites clínicos [MIN_CYCLE_DAYS..MAX_CYCLE_DAYS] para promedios de predicción.
 * 7. Clara diferenciación visual y de datos entre registros reales y proyecciones estimadas.
 */
object CycleCalculationEngine {

  const val MIN_CYCLE_DAYS = 20
  const val MAX_CYCLE_DAYS = 45
  const val DEFAULT_AVERAGE_CYCLE = 28
  const val DEFAULT_PERIOD_DURATION = 5
  const val LUTEAL_PHASE_DAYS = 14

  /**
   * Calcula el promedio ponderado de longitud de ciclos completos válidos.
   * Ordena explícitamente por [CycleEntry.startDate] ascendente (del más antiguo al más reciente)
   * para garantizar que el resultado sea determinista e independiente del orden en que se entregue la lista.
   * Los ciclos más recientes reciben progresivamente mayor peso ponderado (w_i = index + 1).
   *
   * Solo considera ciclos completos ([CycleEntry.cycleLength] != null) dentro del rango clínico [20..45].
   * Si no hay historial completo suficiente, retorna [fallbackAverage].
   */
  /**
   * Calcula el promedio ponderado de longitud de ciclos completos válidos.
   * Ordena explícitamente por [CycleEntry.startDate] ascendente (del más antiguo al más reciente)
   * para garantizar que el resultado sea determinista e independiente del orden en que se entregue la lista.
   * Los ciclos más recientes reciben progresivamente mayor peso ponderado (w_i = index + 1).
   *
   * Solo considera para la estimación de periodicidad regular los ciclos completos dentro de [MIN_CYCLE_DAYS..MAX_CYCLE_DAYS].
   * Los ciclos atípicos (por ej. 60 días) se preservan intactos en el historial real del usuario, sin recortarlos artificialmente.
   * Si no hay historial utilizable por el estimador, retorna [fallbackAverage].
   */
  fun calculateWeightedAverage(cycles: List<CycleEntry>, fallbackAverage: Int = DEFAULT_AVERAGE_CYCLE): Double {
    val usableCycles = cycles.filter { entry ->
      val len = entry.cycleLength
      len != null && len in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS
    }
    val clampedFallback = fallbackAverage.coerceIn(MIN_CYCLE_DAYS, MAX_CYCLE_DAYS)

    if (usableCycles.isEmpty()) return clampedFallback.toDouble()

    // Ordenar explícitamente por startDate ascendente: el más reciente queda al final
    val sorted = usableCycles.sortedWith(compareBy({ it.startDate }, { it.id }))

    var weightedSum = 0.0
    var totalWeight = 0.0

    sorted.forEachIndexed { index, entry ->
      val weight = (index + 1).toDouble()
      val actualLen = entry.cycleLength!!.toDouble()
      weightedSum += actualLen * weight
      totalWeight += weight
    }

    return (weightedSum / totalWeight).coerceIn(MIN_CYCLE_DAYS.toDouble(), MAX_CYCLE_DAYS.toDouble())
  }

  /**
   * Calcula la desviación estándar de la longitud de los ciclos completos válidos respecto a la media.
   * Utiliza la misma población de ciclos válidos que el promedio.
   * Si hay menos de 2 ciclos utilizables, asume una variabilidad fisiológica basal típica de 1.5 días.
   */
  fun calculateStandardDeviation(cycles: List<CycleEntry>, mean: Double): Double {
    val usableCycles = cycles.filter { entry ->
      val len = entry.cycleLength
      len != null && len in MIN_CYCLE_DAYS..MAX_CYCLE_DAYS
    }

    if (usableCycles.size < 2) return 1.5

    val sumSquaredDiffs = usableCycles.sumOf {
      val diff = it.cycleLength!! - mean
      diff * diff
    }
    return sqrt(sumSquaredDiffs / (usableCycles.size - 1))
  }

  /**
   * Calcula el estado actual del ciclo para una fecha dada (típicamente "Hoy").
   *
   * Reglas estrictas:
   * - El día del ciclo se cuenta desde [lastPeriodStart] (Día 1 = fecha de inicio).
   * - NUNCA se reinicia a 1 por módulo al superar la duración media; continúa acumulando días si hay retraso.
   * - [daysUntilNextPeriod] puede ser negativo si la fecha estimada ya pasó, representando retraso real.
   * - Incorpora el fin de sangrado confirmado [CycleEntry.endDate] para no prolongar artificialmente la fase menstrual.
   */
  fun calculateCycleStatus(
    lastPeriodStart: LocalDate,
    averagePeriodDays: Int = DEFAULT_PERIOD_DURATION,
    pastCycles: List<CycleEntry> = emptyList(),
    configuredAverageCycle: Int = DEFAULT_AVERAGE_CYCLE,
    today: LocalDate = LocalDate.now(),
    confirmedBleedingDates: Set<LocalDate> = emptySet()
  ): CycleStatusData {
    val meanCycle = calculateWeightedAverage(pastCycles, configuredAverageCycle)
    val roundedCycle = meanCycle.roundToInt().coerceIn(MIN_CYCLE_DAYS, MAX_CYCLE_DAYS)
    val sigma = calculateStandardDeviation(pastCycles, meanCycle)

    val daysSinceStart = ChronoUnit.DAYS.between(lastPeriodStart, today).toInt()
    // Día 1 es la fecha de inicio. Si today es posterior, acumula continuamente sin módulo.
    val currentDay = if (daysSinceStart >= 0) daysSinceStart + 1 else 1

    val nextPeriodDate = lastPeriodStart.plusDays(roundedCycle.toLong())
    val daysUntilNext = ChronoUnit.DAYS.between(today, nextPeriodDate).toInt()

    val estimatedOvulationDate = nextPeriodDate.minusDays(LUTEAL_PHASE_DAYS.toLong())
    val estimatedOvulationDay = roundedCycle - (LUTEAL_PHASE_DAYS - 1)

    val fertileWindow = FertileWindow(
      startDate = estimatedOvulationDate.minusDays(5),
      endDate = estimatedOvulationDate.plusDays(1),
      ovulationDate = estimatedOvulationDate
    )

    // Buscar si el ciclo activo tiene fin de sangrado confirmado por la usuaria
    val activeCycle = pastCycles.firstOrNull { it.startDate == lastPeriodStart.toString() }
    val activeConfirmedEnd = activeCycle?.endDate?.let {
      try { LocalDate.parse(it) } catch (e: Exception) { null }
    }

    val isDelayed = daysUntilNext < 0

    // Determinación precisa de si hoy es sangrado:
    // Si hay fin confirmado: solo es menstrual entre lastPeriodStart y activeConfirmedEnd.
    // Si no hay fin confirmado: es confirmada hoy si today == lastPeriodStart o hay sangrado diario confirmado;
    // o es estimada si currentDay <= averagePeriodDays.
    val isBleedingToday = today.isEqual(lastPeriodStart) ||
      (activeConfirmedEnd != null && !today.isBefore(lastPeriodStart) && !today.isAfter(activeConfirmedEnd)) ||
      confirmedBleedingDates.contains(today)

    val isEstimatedBleed = activeConfirmedEnd == null && currentDay in 1..averagePeriodDays

    val currentPhase = when {
      isDelayed -> CyclePhase.LUTEAL // Al superarse la fecha estimada, permanece en fase lútea tardía hasta nuevo sangrado
      isBleedingToday || isEstimatedBleed -> CyclePhase.MENSTRUAL
      currentDay in 1..(estimatedOvulationDay - 2) -> CyclePhase.FOLLICULAR
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
      confidenceIntervalDays = sigma
    )
  }

  /**
   * Genera recomendaciones empáticas de apoyo para la pareja según la fase del ciclo.
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
          description = "Ventana de máxima fertilidad estimada. Mayor lubricación natural. Ideal para conectar con amor y calma.",
          categoryIcon = "star"
        ),
        CareTip(
          title = "Cuidado del pH y Flora",
          description = "Manos siempre muy limpias antes de caricias íntimas. Si usan lubricante, elijan uno con pH balanceado.",
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
          description = "El cuello uterino desciende y está más sensible. Tras intimidad puede haber ligero manchado por roce; sé muy suave.",
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
   * Proyecta hasta [cyclesCount] ciclos menstruales futuros con conteo inclusivo exacto y ventana fértil consistente.
   * La variabilidad esperada se expande con cada ciclo proyectado (sigma * sqrt(k)).
   */
  fun predictFutureCycles(
    lastPeriodStart: LocalDate,
    averagePeriodDays: Int = DEFAULT_PERIOD_DURATION,
    pastCycles: List<CycleEntry> = emptyList(),
    configuredAverageCycle: Int = DEFAULT_AVERAGE_CYCLE,
    cyclesCount: Int = 6
  ): List<PredictedCycle> {
    val meanCycle = calculateWeightedAverage(pastCycles, configuredAverageCycle)
    val roundedCycle = meanCycle.roundToInt().coerceIn(MIN_CYCLE_DAYS, MAX_CYCLE_DAYS)
    val sigma = calculateStandardDeviation(pastCycles, meanCycle)

    val predictions = mutableListOf<PredictedCycle>()
    var currentPeriodStart = lastPeriodStart

    // Conteo inclusivo de sangrado: si dura 1 día, fin = inicio (0 días agregados)
    val periodSpanDays = (averagePeriodDays - 1).coerceAtLeast(0).toLong()

    for (k in 1..cyclesCount) {
      val nextPeriodStart = currentPeriodStart.plusDays(roundedCycle.toLong())
      val nextPeriodEnd = nextPeriodStart.plusDays(periodSpanDays)
      val ovulationDate = nextPeriodStart.plusDays((roundedCycle - LUTEAL_PHASE_DAYS).toLong())
      val sigmaSpread = (sigma * sqrt(k.toDouble())).roundToInt().coerceAtLeast(0)

      predictions.add(
        PredictedCycle(
          cycleNumber = k,
          expectedStartDate = nextPeriodStart,
          expectedEndDate = nextPeriodEnd,
          expectedOvulationDate = ovulationDate,
          fertileWindowStart = ovulationDate.minusDays(5 + sigmaSpread.toLong()),
          fertileWindowEnd = ovulationDate.plusDays(1 + sigmaSpread.toLong()),
          confidenceDays = sigma * sqrt(k.toDouble())
        )
      )
      currentPeriodStart = nextPeriodStart
    }
    return predictions
  }

  /**
   * Evalúa cualquier fecha (histórica, actual o futura) identificando con precisión si corresponde
   * a un hecho registrado confirmado o a una estimación proyectada.
   *
   * Busca primero en los ciclos históricos reales de [pastCycles] para no reconstruir todo
   * erróneamente a partir únicamente del último ciclo.
   */
  fun getDayDetail(
    date: LocalDate,
    lastPeriodStart: LocalDate?,
    averagePeriodDays: Int = DEFAULT_PERIOD_DURATION,
    pastCycles: List<CycleEntry> = emptyList(),
    configuredAverageCycle: Int = DEFAULT_AVERAGE_CYCLE,
    today: LocalDate = LocalDate.now(),
    symptomDates: Set<LocalDate> = emptySet(),
    intimacyDates: Set<LocalDate> = emptySet(),
    confirmedBleedingDates: Set<LocalDate> = emptySet()
  ): DayCycleDetail {
    val hasSymptom = symptomDates.contains(date)
    val hasIntimacy = intimacyDates.contains(date)
    val hasConfirmedBleeding = confirmedBleedingDates.contains(date)

    if (lastPeriodStart == null) {
      return DayCycleDetail(
        date = date,
        cycleDay = null,
        highlightType = if (hasConfirmedBleeding) DayHighlightType.ACTUAL_PERIOD else DayHighlightType.NONE,
        phaseName = if (hasConfirmedBleeding) "Menstruación registrada" else "Sin registros de inicio",
        hormoneInsight = "Registra la fecha de inicio de tu periodo para ver el análisis de tus fases y predicciones.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = false,
        isPeriodConfirmed = hasConfirmedBleeding
      )
    }

    val meanCycle = calculateWeightedAverage(pastCycles, configuredAverageCycle)
    val roundedCycle = meanCycle.roundToInt().coerceIn(MIN_CYCLE_DAYS, MAX_CYCLE_DAYS)

    // 1. Revisar si la fecha cae dentro de un ciclo histórico anterior al más reciente
    val sortedAscCycles = pastCycles.mapNotNull { entry ->
      try {
        val start = LocalDate.parse(entry.startDate)
        Pair(start, entry)
      } catch (e: Exception) {
        null
      }
    }.sortedBy { it.first }

    for (i in sortedAscCycles.indices) {
      val (cycleStart, entry) = sortedAscCycles[i]
      val nextCycleStart = if (i + 1 < sortedAscCycles.size) sortedAscCycles[i + 1].first else null

      val belongsToThisHistoricalCycle = !date.isBefore(cycleStart) && (nextCycleStart == null || date.isBefore(nextCycleStart))
      if (belongsToThisHistoricalCycle && nextCycleStart != null) {
        // Ciclo histórico cerrado
        val dayNum = ChronoUnit.DAYS.between(cycleStart, date).toInt() + 1
        val confirmedEnd = entry.endDate?.let {
          try { LocalDate.parse(it) } catch (e: Exception) { null }
        }

        val isStartDay = date.isEqual(cycleStart)
        val isConfirmedBleed = isStartDay || (confirmedEnd != null && !date.isBefore(cycleStart) && !date.isAfter(confirmedEnd)) || hasConfirmedBleeding
        val isEstimatedBleed = !isConfirmedBleed && confirmedEnd == null && dayNum in 1..averagePeriodDays

        return when {
          isConfirmedBleed -> DayCycleDetail(
            date = date,
            cycleDay = dayNum,
            highlightType = DayHighlightType.ACTUAL_PERIOD,
            phaseName = if (isStartDay) "Menstruación histórica (Día 1)" else "Menstruación confirmada (Día $dayNum)",
            hormoneInsight = "Día de sangrado menstrual registrado en tu historial.",
            hasSymptomLogged = hasSymptom,
            hasIntimacyLogged = hasIntimacy,
            isPredicted = false,
            isPeriodConfirmed = true
          )
          isEstimatedBleed -> DayCycleDetail(
            date = date,
            cycleDay = dayNum,
            highlightType = DayHighlightType.PREDICTED_PERIOD,
            phaseName = "Periodo no confirmado (Día $dayNum)",
            hormoneInsight = "Estimación según tu promedio; no se confirmó fin de sangrado ni registro diario para esta fecha histórica.",
            hasSymptomLogged = hasSymptom,
            hasIntimacyLogged = hasIntimacy,
            isPredicted = true,
            isPeriodConfirmed = false
          )
          else -> DayCycleDetail(
            date = date,
            cycleDay = dayNum,
            highlightType = DayHighlightType.NONE,
            phaseName = "Ciclo histórico (Día $dayNum)",
            hormoneInsight = "Registro histórico correspondiente al ciclo iniciado el $cycleStart.",
            hasSymptomLogged = hasSymptom,
            hasIntimacyLogged = hasIntimacy,
            isPredicted = false,
            isPeriodConfirmed = false
          )
        }
      }
    }

    // 2. Si la fecha es anterior a lastPeriodStart y no cayó en un ciclo anterior conocido
    if (date.isBefore(lastPeriodStart)) {
      return DayCycleDetail(
        date = date,
        cycleDay = null,
        highlightType = if (hasConfirmedBleeding) DayHighlightType.ACTUAL_PERIOD else DayHighlightType.NONE,
        phaseName = if (hasConfirmedBleeding) "Sangrado registrado" else "Sin ciclo registrado",
        hormoneInsight = "Esta fecha es anterior al primer inicio de periodo registrado en LunaSync.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = false,
        isPeriodConfirmed = hasConfirmedBleeding
      )
    }

    // 3. Evaluar el ciclo activo (iniciado en lastPeriodStart)
    val activeCycleEntry = sortedAscCycles.lastOrNull()?.second
    val activeConfirmedEnd = activeCycleEntry?.endDate?.let {
      try { LocalDate.parse(it) } catch (e: Exception) { null }
    }

    val daysFromStart = ChronoUnit.DAYS.between(lastPeriodStart, date).toInt()
    val activeDayNum = daysFromStart + 1

    val isStartDay = date.isEqual(lastPeriodStart)
    val isConfirmedBleedDay = isStartDay ||
      (activeConfirmedEnd != null && !date.isBefore(lastPeriodStart) && !date.isAfter(activeConfirmedEnd)) ||
      hasConfirmedBleeding

    val isEstimatedBleedDay = !isConfirmedBleedDay && activeConfirmedEnd == null && activeDayNum in 1..averagePeriodDays

    if (isConfirmedBleedDay) {
      return DayCycleDetail(
        date = date,
        cycleDay = activeDayNum,
        highlightType = DayHighlightType.ACTUAL_PERIOD,
        phaseName = if (isStartDay) "Menstruación registrada (Día 1)" else "Menstruación confirmada (Día $activeDayNum)",
        hormoneInsight = "Fase menstrual activa confirmada. El endometrio se renueva. Prioriza descanso, hidratación y confort.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = false,
        isPeriodConfirmed = true
      )
    }

    if (isEstimatedBleedDay) {
      val isFuture = date.isAfter(today)
      return DayCycleDetail(
        date = date,
        cycleDay = activeDayNum,
        highlightType = DayHighlightType.PREDICTED_PERIOD,
        phaseName = if (isFuture) "Periodo estimado (Día $activeDayNum)" else "Periodo no confirmado (Día $activeDayNum)",
        hormoneInsight = "Día estimado según tu duración promedio. No has confirmado fin de sangrado ni registro diario para esta fecha.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = true,
        isPeriodConfirmed = false
      )
    }

    // Ventana fértil del ciclo actual
    val currentNextPeriod = lastPeriodStart.plusDays(roundedCycle.toLong())
    val currentOvulation = currentNextPeriod.minusDays(LUTEAL_PHASE_DAYS.toLong())
    val currentFertileStart = currentOvulation.minusDays(5)
    val currentFertileEnd = currentOvulation.plusDays(1)

    // ¿Es el día de ovulación del ciclo actual?
    if (date.isEqual(currentOvulation)) {
      return DayCycleDetail(
        date = date,
        cycleDay = activeDayNum,
        highlightType = DayHighlightType.OVULATION_DAY,
        phaseName = "Día de Ovulación Estimada",
        hormoneInsight = "Ovulación estimada fisiológicamente a 14 días del próximo periodo previsto. Mayor probabilidad de fertilidad.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = true,
        isPeriodConfirmed = false
      )
    }

    // ¿Está en la ventana fértil estimada del ciclo actual?
    if (!date.isBefore(currentFertileStart) && !date.isAfter(currentFertileEnd)) {
      return DayCycleDetail(
        date = date,
        cycleDay = activeDayNum,
        highlightType = DayHighlightType.FERTILE_WINDOW,
        phaseName = "Ventana Fértil Estimada",
        hormoneInsight = "Ventana de fertilidad proyectada según el promedio de tus ciclos.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = true,
        isPeriodConfirmed = false
      )
    }

    // 4. Si la fecha supera o iguala la fecha estimada de próximo periodo (currentNextPeriod):
    // REGLA FUNDAMENTAL: La proyección nunca debe sustituir el conteo del ciclo real abierto.
    // Si la fecha es <= today (o si hoy ya se encuentra en retraso), esta fecha pertenece al ciclo actual abierto.
    val isAfterOrAtExpectedPeriod = !date.isBefore(currentNextPeriod)
    val isOpenCycleDelay = isAfterOrAtExpectedPeriod && (date <= today || today >= currentNextPeriod)

    if (isOpenCycleDelay) {
      val delayedDays = ChronoUnit.DAYS.between(currentNextPeriod, date).toInt()
      val delayLabel = if (delayedDays > 0) " (${delayedDays}d de retraso)" else ""
      return DayCycleDetail(
        date = date,
        cycleDay = activeDayNum,
        highlightType = DayHighlightType.LUTEAL,
        phaseName = "Retraso Estimado (Día $activeDayNum)",
        hormoneInsight = "Se ha superado la fecha estimada para el próximo periodo$delayLabel. El ciclo actual continúa abierto hasta registrar un nuevo sangrado real.",
        hasSymptomLogged = hasSymptom,
        hasIntimacyLogged = hasIntimacy,
        isPredicted = date.isAfter(today),
        isPeriodConfirmed = false
      )
    }

    // 5. Proyecciones futuras para fechas posteriores a la fecha esperada cuando hoy NO está en retraso
    if (date.isAfter(currentNextPeriod) && today < currentNextPeriod) {
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
          return DayCycleDetail(
            date = date,
            cycleDay = dayNum,
            highlightType = DayHighlightType.PREDICTED_PERIOD,
            phaseName = "Periodo Proyectado (Ciclo #${pred.cycleNumber})",
            hormoneInsight = "Inicio estimado del sangrado menstrual proyectado con tu ciclo de $roundedCycle días (variabilidad ±${String.format(java.util.Locale.US, "%.1f", pred.confidenceDays)}d).",
            hasSymptomLogged = hasSymptom,
            hasIntimacyLogged = hasIntimacy,
            isPredicted = true,
            isPeriodConfirmed = false
          )
        }

        if (date.isEqual(pred.expectedOvulationDate)) {
          return DayCycleDetail(
            date = date,
            cycleDay = roundedCycle - (LUTEAL_PHASE_DAYS - 1),
            highlightType = DayHighlightType.OVULATION_DAY,
            phaseName = "Ovulación Estimada (Ciclo #${pred.cycleNumber})",
            hormoneInsight = "Fecha estimada para la ovulación en el ciclo proyectado #${pred.cycleNumber}.",
            hasSymptomLogged = hasSymptom,
            hasIntimacyLogged = hasIntimacy,
            isPredicted = true,
            isPeriodConfirmed = false
          )
        }

        if (!date.isBefore(pred.fertileWindowStart) && !date.isAfter(pred.fertileWindowEnd)) {
          return DayCycleDetail(
            date = date,
            cycleDay = null,
            highlightType = DayHighlightType.FERTILE_WINDOW,
            phaseName = "Ventana Fértil Estimada (Ciclo #${pred.cycleNumber})",
            hormoneInsight = "Días de mayor probabilidad biológica calculados para tu ciclo proyectado #${pred.cycleNumber}.",
            hasSymptomLogged = hasSymptom,
            hasIntimacyLogged = hasIntimacy,
            isPredicted = true,
            isPeriodConfirmed = false
          )
        }
      }
    }

    // 6. Fases regulares del ciclo actual (Folicular o Lútea previa a la fecha esperada)
    val isFollicular = activeDayNum < (roundedCycle - (LUTEAL_PHASE_DAYS - 1))
    val phaseName = if (isFollicular) "Fase Folicular (Día $activeDayNum)" else "Fase Lútea (Día $activeDayNum)"
    val insight = if (isFollicular) {
      "Fase de ascenso de estrógeno. Suele acompañarse de energía creciente y motivación."
    } else {
      "Fase dominada por progesterona. Invita a un ritmo más pausado y autocuidado."
    }

    return DayCycleDetail(
      date = date,
      cycleDay = activeDayNum,
      highlightType = if (isFollicular) DayHighlightType.FOLLICULAR else DayHighlightType.LUTEAL,
      phaseName = phaseName,
      hormoneInsight = insight,
      hasSymptomLogged = hasSymptom,
      hasIntimacyLogged = hasIntimacy,
      isPredicted = date.isAfter(today),
      isPeriodConfirmed = false
    )
  }
}
