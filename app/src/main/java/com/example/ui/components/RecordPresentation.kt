package com.example.ui.components

import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.IntimacyLog

fun physicalRecordText(record: DailySymptom?, isOwner: Boolean, canRead: Boolean = true): String {
  if (!canRead) return "Los síntomas físicos no se comparten contigo."
  if (record == null || !record.hasPhysicalRecord || (!isOwner && record.isPrivate)) {
    return if (isOwner) "Sin registro físico para esta fecha." else "Sin registro físico compartido para esta fecha."
  }
  return buildList {
    add(when (record.flowLevel) {
      "NONE" -> "Sin sangrado"
      "SPOTTING" -> "Manchado"
      "LIGHT" -> "Flujo ligero"
      "MEDIUM" -> "Flujo medio"
      "HEAVY" -> "Flujo abundante"
      else -> "Flujo sin especificar"
    })
    if (record.painLevel > 0) add("Dolor ${record.painLevel}/5")
    if (record.cramps) add("Cólicos")
    if (record.headache) add("Dolor de cabeza")
    if (record.backPain) add("Dolor lumbar")
    if (record.bloating) add("Hinchazón")
    if (record.tenderBreasts) add("Senos sensibles")
    if (record.cravings) add("Antojos")
    if (record.acne) add("Acné")
    when (record.cervicalMucus) {
      "DRY" -> add("Moco cervical: seco")
      "STICKY" -> add("Moco cervical: pegajoso")
      "CREAMY" -> add("Moco cervical: cremoso")
      "EGG_WHITE" -> add("Moco cervical: clara de huevo")
    }
    record.publicNote?.takeIf { it.isNotBlank() }?.let { add(it) }
  }.joinToString(" · ")
}

fun moodRecordText(record: DailySymptom?, isOwner: Boolean, canRead: Boolean = true): String {
  if (!canRead) return "El ánimo y la energía no se comparten contigo."
  if (record == null || !record.hasMoodRecord || record.mood.isBlank() || (!isOwner && (record.isMoodPrivate ?: record.isPrivate))) {
    return if (isOwner) "Sin ánimo registrado para esta fecha." else "Sin ánimo compartido para esta fecha."
  }
  val mood = when (record.mood) {
    "HAPPY" -> "Radiante"
    "CALM" -> "En calma"
    "SENSITIVE" -> "Sensible"
    "ENERGETIC" -> "Con energía"
    "CRAVINGS" -> "Con antojo"
    "IRRITABLE" -> "Irritable"
    "ANXIOUS" -> "Ansiosa"
    "SAD" -> "Con bajón"
    else -> "Registrado"
  }
  return "Ánimo: $mood" + if (record.energyLevel > 0) " · Energía ${record.energyLevel}/5" else ""
}

fun intimacyRecordText(record: IntimacyLog): String = buildList {
  add(when (record.encounterType) {
    "PROTECTED" -> "Con protección"
    "UNPROTECTED" -> "Sin protección"
    else -> "Contacto sin penetración"
  })
  add(if (record.usedLubricant) "Con lubricante" else "Sin lubricante")
  add(if (record.hadOrgasm) "Orgasmo registrado" else "Sin orgasmo registrado")
  when (record.postContactEffect) {
    "AROUSAL_FLUID" -> add("Mayor humedad")
    "CONTACT_SPOTTING" -> add("Manchado tras contacto")
    "FRICTION_SENSITIVITY" -> add("Sensibilidad tras contacto")
    "PH_SHIFT" -> add("Cambio de pH registrado")
  }
  record.notes?.takeIf { it.isNotBlank() }?.let { add(it) }
}.joinToString(" · ")

fun cycleEstimationText(status: CycleStatusData): String =
  if (status.isBasedOnHistory) "Las fechas pueden variar. Basado en ${status.historyCyclesUsed} " +
    if (status.historyCyclesUsed == 1) "ciclo registrado." else "ciclos registrados."
  else "Estimación inicial basada en tu configuración. Añade más registros para ajustarla."
