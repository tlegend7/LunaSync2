package com.example

import com.example.data.engine.CycleCalculationEngine
import com.example.data.model.CycleEntry
import com.example.data.model.CyclePhase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleCalculationEngineTest {

  @Test
  fun testWeightedAverageCalculation() {
    val cycles = listOf(
      CycleEntry(cycleLength = 26),
      CycleEntry(cycleLength = 28),
      CycleEntry(cycleLength = 30)
    )
    // weights: 1 for 26, 2 for 28, 3 for 30 -> (26*1 + 28*2 + 30*3)/(1+2+3) = (26 + 56 + 90)/6 = 172/6 = 28.666...
    val avg = CycleCalculationEngine.calculateWeightedAverage(cycles, fallbackAverage = 28)
    assertEquals(28.666, avg, 0.01)
  }

  @Test
  fun testStandardDeviation() {
    val cycles = listOf(
      CycleEntry(cycleLength = 28),
      CycleEntry(cycleLength = 28)
    )
    val sigma = CycleCalculationEngine.calculateStandardDeviation(cycles, 28.0)
    assertEquals(0.0, sigma, 0.001)
  }

  @Test
  fun testCyclePhasesAndOvulation() {
    val lastPeriod = LocalDate.of(2026, 10, 20)
    val today = LocalDate.of(2026, 11, 2) // 13 days after period start
    val status = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      pastCycles = listOf(CycleEntry(cycleLength = 28)),
      configuredAverageCycle = 28,
      today = today
    )

    assertEquals(14, status.currentDay)
    assertEquals(CyclePhase.OVULATORY, status.currentPhase)
    assertTrue(status.daysUntilNextPeriod > 0)
    assertEquals(LocalDate.of(2026, 11, 3), status.fertileWindow.ovulationDate)
  }

  @Test
  fun testPartnerCareTipsGenerated() {
    val menstrualTips = CycleCalculationEngine.getPartnerCareTips(CyclePhase.MENSTRUAL, 2)
    assertEquals(3, menstrualTips.size)
    assertTrue(menstrualTips.any { it.categoryIcon == "tea" })

    val lutealTips = CycleCalculationEngine.getPartnerCareTips(CyclePhase.LUTEAL, 24)
    assertEquals(3, lutealTips.size)
    assertTrue(lutealTips.any { it.title.contains("Paciencia") })
  }

  @Test
  fun testFutureCyclePredictions() {
    val lastPeriod = LocalDate.of(2026, 11, 1)
    val predictions = CycleCalculationEngine.predictFutureCycles(
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      pastCycles = listOf(CycleEntry(cycleLength = 28)),
      configuredAverageCycle = 28,
      cyclesCount = 4
    )

    assertEquals(4, predictions.size)
    // First predicted cycle starts 28 days after lastPeriod -> Nov 29
    assertEquals(LocalDate.of(2026, 11, 29), predictions[0].expectedStartDate)
    assertEquals(LocalDate.of(2026, 12, 3), predictions[0].expectedEndDate)
    // Second predicted cycle starts 28 days later -> Dec 27
    assertEquals(LocalDate.of(2026, 12, 27), predictions[1].expectedStartDate)
  }

  @Test
  fun testGetDayDetailClassification() {
    val lastPeriod = LocalDate.of(2026, 11, 1)
    val detailActualPeriod = CycleCalculationEngine.getDayDetail(
      date = LocalDate.of(2026, 11, 2),
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      configuredAverageCycle = 28
    )
    assertEquals(com.example.data.model.DayHighlightType.ACTUAL_PERIOD, detailActualPeriod.highlightType)

    val detailFuturePeriod = CycleCalculationEngine.getDayDetail(
      date = LocalDate.of(2026, 11, 29),
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      configuredAverageCycle = 28
    )
    assertEquals(com.example.data.model.DayHighlightType.PREDICTED_PERIOD, detailFuturePeriod.highlightType)
  }
}
