package com.example

import com.example.data.model.DailySymptom
import com.example.ui.components.moodRecordText
import com.example.ui.components.physicalRecordText
import org.junit.Assert.*
import org.junit.Test

class RecordPresentationTest {
  @Test fun physicalAndMoodPrivacyAreIndependentAndLegacyPrivacyStillApplies() {
    val moodShared = DailySymptom(isPrivate = true, isMoodPrivate = false, mood = "HAPPY")
    assertEquals("Sin registro físico compartido para esta fecha.", physicalRecordText(moodShared, false))
    assertTrue(moodRecordText(moodShared, false).contains("Radiante"))
    val physicalShared = DailySymptom(isPrivate = false, isMoodPrivate = true, mood = "ANXIOUS")
    assertTrue(physicalRecordText(physicalShared, false).contains("Sin sangrado"))
    assertFalse(moodRecordText(physicalShared, false).contains("Ansiosa"))
    assertFalse(moodRecordText(DailySymptom(isPrivate = true, mood = "HAPPY"), false).contains("Radiante"))
  }
  @Test fun unsharedIsDifferentFromNoSharedRecord() {
    assertEquals("Los síntomas físicos no se comparten contigo.", physicalRecordText(null, false, false))
    assertEquals("Sin registro físico compartido para esta fecha.", physicalRecordText(null, false, true))
    assertEquals("El ánimo y la energía no se comparten contigo.", moodRecordText(null, false, false))
  }

  @Test fun aMoodOnlyRecordNeverImpliesAnAbsenceOfPhysicalSymptoms() {
    val record = DailySymptom(mood = "SENSITIVE", energyLevel = 2, hasPhysicalRecord = false)
    assertEquals("Sin registro físico compartido para esta fecha.", physicalRecordText(record, false))
    assertEquals("Ánimo: Sensible · Energía 2/5", moodRecordText(record, false))
  }

  @Test fun privateDataAndMissingMoodAreNotPresentedAsFacts() {
    val privateRecord = DailySymptom(isPrivate = true, painLevel = 5, mood = "ANXIOUS")
    assertFalse(physicalRecordText(privateRecord, false).contains("5/5"))
    assertFalse(moodRecordText(privateRecord, false).contains("Ansiosa"))
    assertEquals("Sin ánimo registrado para esta fecha.", moodRecordText(DailySymptom(hasMoodRecord = false), true))
  }

  @Test fun physicalSummaryShowsFlowAndRecordedPainWithoutInventingSeverity() {
    val text = physicalRecordText(DailySymptom(flowLevel = "HEAVY", cramps = true, painLevel = 5), true)
    assertTrue(text.contains("Flujo abundante"))
    assertTrue(text.contains("Dolor 5/5"))
    assertTrue(text.contains("Cólicos"))
    assertFalse(text.contains("leves"))
  }
}
