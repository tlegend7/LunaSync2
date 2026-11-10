package com.example

import com.example.data.engine.CycleCalculationEngine
import com.example.data.model.CycleEntry
import com.example.data.model.CyclePhase
import com.example.data.model.DayHighlightType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class CycleCalculationEngineTest {

  @Test
  fun testEmptyHistoryFallback() {
    // When no past cycles exist, fallback average is respected
    val avg = CycleCalculationEngine.calculateWeightedAverage(emptyList(), fallbackAverage = 30)
    assertEquals(30.0, avg, 0.001)

    val sigma = CycleCalculationEngine.calculateStandardDeviation(emptyList(), 28.0)
    assertEquals(1.5, sigma, 0.001) // default minimum expected deviation
  }

  @Test
  fun testSingleIncompleteCycleIgnoredInWeightedAverage() {
    // Active cycle without completed cycleLength is not factored into average length
    val incomplete = listOf(
      CycleEntry(id = "c1", startDate = "2026-05-01", cycleLength = null)
    )
    val avg = CycleCalculationEngine.calculateWeightedAverage(incomplete, fallbackAverage = 29)
    assertEquals(29.0, avg, 0.001)
  }

  @Test
  fun testOrderInvarianceAndRecencyWeighting() {
    // 3 completed cycles: oldest=26, middle=28, newest=30
    val c1 = CycleEntry(id = "c1", startDate = "2026-01-01", cycleLength = 26)
    val c2 = CycleEntry(id = "c2", startDate = "2026-01-27", cycleLength = 28)
    val c3 = CycleEntry(id = "c3", startDate = "2026-02-24", cycleLength = 30)

    val ascendingList = listOf(c1, c2, c3)
    val descendingList = listOf(c3, c2, c1)
    val randomList = listOf(c2, c1, c3)

    val avgAsc = CycleCalculationEngine.calculateWeightedAverage(ascendingList, 28)
    val avgDesc = CycleCalculationEngine.calculateWeightedAverage(descendingList, 28)
    val avgRand = CycleCalculationEngine.calculateWeightedAverage(randomList, 28)

    // Order in input list MUST NOT change the result (deterministic sorting by startDate)
    assertEquals(avgAsc, avgDesc, 0.0001)
    assertEquals(avgAsc, avgRand, 0.0001)

    // Newest cycle (c3: 30) gets highest weight (weight=3)
    // weights: 1 for 26, 2 for 28, 3 for 30 -> (26*1 + 28*2 + 30*3)/6 = (26+56+90)/6 = 172/6 = 28.6666...
    assertEquals(28.6666, avgAsc, 0.001)
  }

  @Test
  fun testStandardDeviationCalculation() {
    val identicalCycles = listOf(
      CycleEntry(startDate = "2026-01-01", cycleLength = 28),
      CycleEntry(startDate = "2026-01-29", cycleLength = 28),
      CycleEntry(startDate = "2026-02-26", cycleLength = 28)
    )
    val sigmaZero = CycleCalculationEngine.calculateStandardDeviation(identicalCycles, 28.0)
    assertEquals(0.0, sigmaZero, 0.001)

    val varyingCycles = listOf(
      CycleEntry(startDate = "2026-01-01", cycleLength = 26),
      CycleEntry(startDate = "2026-01-27", cycleLength = 30)
    )
    // mean = 28, variance = ((26-28)^2 + (30-28)^2)/(2-1) = (4+4)/1 = 8, stdDev = sqrt(8) ~ 2.8284
    val sigmaVarying = CycleCalculationEngine.calculateStandardDeviation(varyingCycles, 28.0)
    assertEquals(2.8284, sigmaVarying, 0.01)
  }

  @Test
  fun testCycleStatusDay1AndPhases() {
    val lastPeriod = LocalDate.of(2026, 11, 1)
    // Exactly on start date -> Day 1
    val statusDay1 = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      pastCycles = listOf(CycleEntry(startDate = "2026-11-01", cycleLength = null)),
      configuredAverageCycle = 28,
      today = lastPeriod
    )
    assertEquals(1, statusDay1.currentDay)
    assertEquals(CyclePhase.MENSTRUAL, statusDay1.currentPhase)
    assertEquals(28, statusDay1.daysUntilNextPeriod)
    assertFalse(statusDay1.isDelayed)
    assertEquals(0, statusDay1.delayedDays)

    // Day 14 -> Ovulation / Fertile
    val todayDay14 = lastPeriod.plusDays(13)
    val statusDay14 = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      pastCycles = listOf(CycleEntry(startDate = "2026-11-01", cycleLength = null)),
      configuredAverageCycle = 28,
      today = todayDay14
    )
    assertEquals(14, statusDay14.currentDay)
    assertEquals(CyclePhase.OVULATORY, statusDay14.currentPhase)
    assertEquals(LocalDate.of(2026, 11, 15), statusDay14.fertileWindow.ovulationDate)
  }

  @Test
  fun testCycleStatusExpectedTodayAndDelayedWithoutModuloWrap() {
    val lastPeriod = LocalDate.of(2026, 10, 1)
    // 28 days later is Oct 29 -> expected period day (daysUntilNext == 0)
    val todayExact = LocalDate.of(2026, 10, 29)
    val statusToday = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      pastCycles = emptyList(),
      configuredAverageCycle = 28,
      today = todayExact
    )
    assertEquals(29, statusToday.currentDay)
    assertEquals(0, statusToday.daysUntilNextPeriod)
    assertFalse(statusToday.isDelayed)

    // 35 days later (Nov 5) -> 7 days delayed (no modulo reset to day 7 of menstrual!)
    val todayDelayed = LocalDate.of(2026, 11, 5)
    val statusDelayed = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = lastPeriod,
      averagePeriodDays = 5,
      pastCycles = emptyList(),
      configuredAverageCycle = 28,
      today = todayDelayed
    )
    assertEquals(36, statusDelayed.currentDay)
    assertEquals(-7, statusDelayed.daysUntilNextPeriod)
    assertTrue(statusDelayed.isDelayed)
    assertEquals(7, statusDelayed.delayedDays)
    assertEquals(CyclePhase.LUTEAL, statusDelayed.currentPhase) // remains in extended luteal phase until new period is registered
  }

  @Test
  fun testBleedingDurationOneDayInclusive() {
    val start = LocalDate.of(2026, 6, 10)
    // If period duration is 1 day, end date is the exact same day
    val predictions = CycleCalculationEngine.predictFutureCycles(
      lastPeriodStart = start,
      averagePeriodDays = 1,
      pastCycles = emptyList(),
      configuredAverageCycle = 28,
      cyclesCount = 2
    )

    assertEquals(start.plusDays(28), predictions[0].expectedStartDate)
    assertEquals(start.plusDays(28), predictions[0].expectedEndDate) // span = 0 days added
  }

  @Test
  fun testPredictionHorizonSixCycles() {
    val start = LocalDate.of(2026, 1, 1)
    val predictions = CycleCalculationEngine.predictFutureCycles(
      lastPeriodStart = start,
      averagePeriodDays = 5,
      pastCycles = emptyList(),
      configuredAverageCycle = 28,
      cyclesCount = 6
    )

    assertEquals(6, predictions.size)
    for (i in 0 until 6) {
      assertEquals(i + 1, predictions[i].cycleNumber)
      assertTrue(predictions[i].expectedEndDate.isAfter(predictions[i].expectedStartDate) || predictions[i].expectedEndDate.isEqual(predictions[i].expectedStartDate))
      assertTrue(predictions[i].fertileWindowEnd.isAfter(predictions[i].fertileWindowStart))
    }
  }

  @Test
  fun testLeapYearAndMonthBoundaries() {
    // 2028 is a leap year (February has 29 days)
    val startLeap = LocalDate.of(2028, 2, 15)
    val status = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = startLeap,
      averagePeriodDays = 5,
      pastCycles = emptyList(),
      configuredAverageCycle = 28,
      today = LocalDate.of(2028, 3, 1)
    )
    // Days from Feb 15 to Mar 1 in 2028: (29 - 15) + 1 = 15 days passed -> Day 16
    assertEquals(16, status.currentDay)
    assertEquals(LocalDate.of(2028, 3, 14), status.nextPeriodDate)
  }

  @Test
  fun testRangeLimitsCoercion() {
    // Abnormally short cycle length (e.g. 15) is clamped to MIN_CYCLE_DAYS = 20
    val shortCycle = listOf(CycleEntry(startDate = "2026-01-01", cycleLength = 15))
    val avgShort = CycleCalculationEngine.calculateWeightedAverage(shortCycle, fallbackAverage = 28)
    val statusShort = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = LocalDate.of(2026, 2, 1),
      averagePeriodDays = 5,
      pastCycles = shortCycle,
      configuredAverageCycle = 28
    )
    assertEquals(20, statusShort.totalDays)

    // Abnormally long cycle length (e.g. 60) is clamped to MAX_CYCLE_DAYS = 45
    val longCycle = listOf(CycleEntry(startDate = "2026-01-01", cycleLength = 60))
    val statusLong = CycleCalculationEngine.calculateCycleStatus(
      lastPeriodStart = LocalDate.of(2026, 2, 1),
      averagePeriodDays = 5,
      pastCycles = longCycle,
      configuredAverageCycle = 28
    )
    assertEquals(45, statusLong.totalDays)
  }

  @Test
  fun testDayDetailNullLastPeriodStart() {
    val date = LocalDate.of(2026, 8, 15)
    val detail = CycleCalculationEngine.getDayDetail(
      date = date,
      lastPeriodStart = null,
      pastCycles = emptyList()
    )
    assertNull(detail.cycleDay)
    assertEquals(DayHighlightType.NONE, detail.highlightType)
    assertFalse(detail.isPredicted)
    assertFalse(detail.isPeriodConfirmed)
  }

  @Test
  fun testDayDetailHistoricalCycleConfirmed() {
    val cycleOld = CycleEntry(
      id = "old_1",
      startDate = "2026-03-01",
      endDate = "2026-03-05",
      periodDuration = 5,
      cycleLength = 28
    )
    val cycleNew = CycleEntry(
      id = "new_1",
      startDate = "2026-03-29",
      endDate = "2026-04-02",
      periodDuration = 5,
      cycleLength = null
    )

    // Date during old cycle bleeding: March 3
    val detailBleed = CycleCalculationEngine.getDayDetail(
      date = LocalDate.of(2026, 3, 3),
      lastPeriodStart = LocalDate.of(2026, 3, 29),
      pastCycles = listOf(cycleOld, cycleNew)
    )
    assertEquals(DayHighlightType.ACTUAL_PERIOD, detailBleed.highlightType)
    assertTrue(detailBleed.isPeriodConfirmed)
    assertFalse(detailBleed.isPredicted)
    assertEquals(3, detailBleed.cycleDay)

    // Date during old cycle post-bleed: March 15
    val detailHistoricalLuteal = CycleCalculationEngine.getDayDetail(
      date = LocalDate.of(2026, 3, 15),
      lastPeriodStart = LocalDate.of(2026, 3, 29),
      pastCycles = listOf(cycleOld, cycleNew)
    )
    assertEquals(DayHighlightType.NONE, detailHistoricalLuteal.highlightType)
    assertFalse(detailHistoricalLuteal.isPeriodConfirmed)
    assertEquals(15, detailHistoricalLuteal.cycleDay)
  }
}
