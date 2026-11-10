package com.example.ui.screens

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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CheckIn
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.EncounterType
import com.example.data.model.IntimacyLog
import com.example.data.model.PartnerObservation
import com.example.data.model.QuickResponseOption
import com.example.ui.components.CheckInBannerComponent
import com.example.ui.components.CycleDialComponent
import com.example.ui.components.SpottingSuggestionCard
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseFertileSage
import com.example.ui.theme.PhaseFertileSubtle
import com.example.ui.theme.PhaseFollicularPeach
import com.example.ui.theme.PhaseFollicularSubtle
import com.example.ui.theme.PhaseLutealLavender
import com.example.ui.theme.PhaseLutealSubtle
import com.example.ui.theme.PhaseMenstrualRose
import com.example.ui.theme.PhaseMenstrualSubtle

@OptIn(ExperimentalLayoutApi::class)
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
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 16.dp, vertical = 6.dp)
  ) {
    // Central Cycle Dial
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .padding(vertical = 6.dp)
    ) {
      CycleDialComponent(cycleStatus = cycleStatus)
    }

    Spacer(modifier = Modifier.height(6.dp))

    // If no cycle status registered yet, show dedicated Onboarding Period Card
    if (cycleStatus == null) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = PhaseMenstrualSubtle,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
          .border(1.dp, PhaseMenstrualRose.copy(alpha = 0.4f), RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = PhaseMenstrualRose, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Bienvenida a tu seguimiento",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = PhaseMenstrualRose
            )
          }
          Spacer(modifier = Modifier.height(6.dp))
          Text(
            text = "Registra el primer día de tu última menstruación para que LunaSync aprenda tus patrones reales y calcule tus fases.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = onOpenPeriodDialog,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = PhaseMenstrualRose),
            modifier = Modifier.fillMaxWidth().testTag("register_period_start_cta")
          ) {
            Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Registrar inicio de mi periodo", fontWeight = FontWeight.SemiBold)
          }
        }
      }
    }

    // Interactive Check-In Banner from Partner (if active)
    if (latestCheckIn != null) {
      CheckInBannerComponent(
        checkIn = latestCheckIn,
        isElla = true,
        onQuickReplySelected = onQuickReplySelected,
        modifier = Modifier.padding(bottom = 12.dp)
      )
    }

    // Spotting Suggestion Card (if partner noted verbal spotting)
    if (activeSpottingSuggestion != null) {
      SpottingSuggestionCard(
        onAccept = onAcceptSpotting,
        onDismiss = onDismissSpotting,
        modifier = Modifier.padding(bottom = 12.dp)
      )
    }

    // Fast Action Bar (3 balanced actions)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Button(
        onClick = { if (cycleStatus == null) onOpenPeriodDialog() else onOpenSymptomDialog() },
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
        modifier = Modifier
          .weight(1.3f)
          .height(48.dp)
          .testTag("log_symptoms_cta")
      ) {
        Icon(if (cycleStatus == null) Icons.Default.CalendarMonth else Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(if (cycleStatus == null) "Periodo" else "Registrar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }

      OutlinedButton(
        onClick = onNavigateToCalendar,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppleHairlineBorder),
        colors = ButtonDefaults.outlinedButtonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = MaterialTheme.colorScheme.onSurface
        ),
        modifier = Modifier
          .weight(1.2f)
          .height(48.dp)
          .testTag("open_calendar_cta")
      ) {
        Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = PhaseFertileSage, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("Calendario", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }

      FilledTonalButton(
        onClick = onOpenIntimacyDialog,
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = PhaseMenstrualRose
        ),
        modifier = Modifier
          .weight(1.1f)
          .height(48.dp)
          .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(16.dp))
          .testTag("log_intimacy_cta")
      ) {
        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp), tint = PhaseMenstrualRose)
        Spacer(modifier = Modifier.width(4.dp))
        Text("Intimidad", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Hormonal & Cycle Wisdom Card (Empathetic female health card)
    val isNull = cycleStatus == null
    val phase = cycleStatus?.currentPhase ?: CyclePhase.FOLLICULAR
    val dayNum = cycleStatus?.currentDay ?: 0
    val (wisdomTitle, wisdomText, wisdomIconTint, wisdomBg) = if (isNull) {
      Quad(
        "Tu ciclo con base en datos reales",
        "LunaSync no inventa datos menstruales. Una vez que ingreses la fecha de tu periodo, esta tarjeta te guiará con información hormonal precisa y recomendaciones adaptadas a tu biología.",
        BrandTerracottaRose,
        PhaseFollicularSubtle
      )
    } else {
      when (phase) {
        CyclePhase.MENSTRUAL -> Quad(
          "Fase Menstrual • Renovación",
          "El estrógeno y la progesterona están en su nivel basal. El cuerpo limpia y reinicia su ciclo. El magnesio, las bebidas tibias y las pausas te devolverán balance.",
          PhaseMenstrualRose,
          PhaseMenstrualSubtle
        )
        CyclePhase.FOLLICULAR -> Quad(
          "Fase Folicular • Energía Creciente",
          "Tu hormona folículo estimulante (FSH) y estrógeno van en aumento. Sentirás mayor lucidez mental, vitalidad física y apertura para proyectos creativos.",
          PhaseFollicularPeach,
          PhaseFollicularSubtle
        )
        CyclePhase.OVULATORY -> Quad(
          "Fase Ovulatoria • Máximo Resplandor",
          "Pico de LH y estrógeno. Estás en tus días de mayor magnetismo natural y apertura social. El moco cervical fértil nutre y protege los espermatozoides.",
          PhaseFertileSage,
          PhaseFertileSubtle
        )
        CyclePhase.LUTEAL -> Quad(
          "Fase Lútea • Calma & Cuidado",
          "La progesterona toma el mando para calmar el sistema nervioso. Puedes experimentar sensibilidad o antojos por la caída gradual de serotonina.",
          PhaseLutealLavender,
          PhaseLutealSubtle
        )
      }
    }

    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(34.dp)
              .clip(RoundedCornerShape(10.dp))
              .background(wisdomBg)
          ) {
            Icon(Icons.Default.Spa, contentDescription = null, tint = wisdomIconTint, modifier = Modifier.size(18.dp))
          }
          Spacer(modifier = Modifier.width(10.dp))
          Column {
            Text(
              text = if (isNull) "INFORMACIÓN HORMONAL" else "TU CUERPO HOY (DÍA $dayNum)",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = wisdomIconTint,
              letterSpacing = 0.5.sp
            )
            Text(
              text = wisdomTitle,
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurface
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = wisdomText,
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurface,
          lineHeight = 18.sp
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Partner Sweet Message or Care Action (if logged)
    val latestObservationWithCare = partnerObservations.firstOrNull {
      !it.careActionTaken.isNullOrBlank() || !it.sweetMessage.isNullOrBlank()
    }
    if (latestObservationWithCare != null) {
      Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        modifier = Modifier
          .fillMaxWidth()
          .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(PhaseFertileSubtle)
            ) {
              Icon(Icons.Default.VolunteerActivism, contentDescription = null, tint = PhaseFertileSage, modifier = Modifier.size(16.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column {
              Text(
                text = "DETALLE DE TU PAREJA",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = PhaseFertileSage,
                letterSpacing = 0.5.sp
              )
              Text(
                text = "Cuidado registrado con amor",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
              )
            }
          }

          if (!latestObservationWithCare.careActionTaken.isNullOrBlank()) {
            Text(
              text = "• Acción: ${latestObservationWithCare.careActionTaken}",
              style = MaterialTheme.typography.bodyMedium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(top = 8.dp)
            )
          }

          if (!latestObservationWithCare.sweetMessage.isNullOrBlank()) {
            Text(
              text = "\"${latestObservationWithCare.sweetMessage}\"",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = PhaseLutealLavender,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
    }

    // Today's Registered Symptoms Summary
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Column {
            Text(
              text = "REGISTRO DE HOY",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              letterSpacing = 0.5.sp
            )
            Text(
              text = if (todaySymptom != null) "Síntomas y sensaciones" else "Sin registro de hoy aún",
              style = MaterialTheme.typography.titleSmall,
              fontWeight = FontWeight.Bold
            )
          }

          if (todaySymptom?.isPrivate == true) {
            Surface(
              shape = RoundedCornerShape(8.dp),
              color = PhaseLutealSubtle,
              modifier = Modifier.border(0.5.dp, PhaseLutealLavender.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
              ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = PhaseLutealLavender, modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(3.dp))
                Text("Privado", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = PhaseLutealLavender)
              }
            }
          }
        }

        if (todaySymptom != null) {
          Spacer(modifier = Modifier.height(12.dp))

          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            if (todaySymptom.flowLevel != "NONE") {
              SymptomBadge(label = "Flujo: ${todaySymptom.flowLevel}", color = PhaseMenstrualRose)
            }
            if (todaySymptom.cramps) {
              SymptomBadge(label = "Cólicos", color = PhaseMenstrualRose)
            }
            if (todaySymptom.tenderBreasts) {
              SymptomBadge(label = "Senos sensibles", color = PhaseLutealLavender)
            }
            if (todaySymptom.cravings) {
              SymptomBadge(label = "Antojos", color = PhaseFollicularPeach)
            }
            if (todaySymptom.cervicalMucus != "NONE") {
              SymptomBadge(label = "Moco: ${todaySymptom.cervicalMucus}", color = PhaseFertileSage)
            }
            if (todaySymptom.headache) {
              SymptomBadge(label = "Cefalea", color = BrandTerracottaRose)
            }
            if (todaySymptom.backPain) {
              SymptomBadge(label = "Espalda baja", color = BrandTerracottaRose)
            }
            if (todaySymptom.bloating) {
              SymptomBadge(label = "Hinchazón", color = BrandTerracottaRose)
            }
            if (todaySymptom.acne) {
              SymptomBadge(label = "Acné/Piel", color = PhaseFollicularPeach)
            }
            SymptomBadge(label = "Ánimo: ${todaySymptom.mood}", color = BrandTerracottaRose)
            SymptomBadge(label = "Energía: ${todaySymptom.energyLevel}/5", color = PhaseFollicularPeach)
          }

          if (!todaySymptom.publicNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Nota compartida: \"${todaySymptom.publicNote}\"",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        } else {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Toca 'Registrar' arriba para anotar cómo te sientes hoy.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Intimacy Log Overview
    Surface(
      shape = RoundedCornerShape(20.dp),
      color = MaterialTheme.colorScheme.surface,
      tonalElevation = 1.dp,
      modifier = Modifier
        .fillMaxWidth()
        .border(0.5.dp, AppleHairlineBorder, RoundedCornerShape(20.dp))
    ) {
      Column(modifier = Modifier.padding(16.dp)) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "REGISTRO DE INTIMIDAD",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
          )
          Text(
            text = "${intimacyLogs.size} registrados",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        if (intimacyLogs.isEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Text(
            text = "No hay encuentros íntimos registrados recientemente.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
          )
        } else {
          intimacyLogs.take(3).forEach { log ->
            Spacer(modifier = Modifier.height(10.dp))
            Column {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Favorite, contentDescription = null, tint = PhaseMenstrualRose, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(8.dp))
                val typeLabel = when (log.encounterType) {
                  "PROTECTED" -> "Con protección"
                  "UNPROTECTED" -> "Sin protección"
                  "OUTERCOURSE" -> "Caricias / Tocamientos"
                  else -> log.encounterType
                }
                Text(
                  text = "${log.date}: $typeLabel",
                  style = MaterialTheme.typography.bodySmall,
                  fontWeight = FontWeight.SemiBold,
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (log.isPrivate) {
                  Spacer(modifier = Modifier.width(6.dp))
                  Icon(Icons.Default.Lock, contentDescription = "Privado", tint = PhaseLutealLavender, modifier = Modifier.size(12.dp))
                }
              }

              // Physiological tags (Lube, Orgasm, Post Contact Effect)
              FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(start = 22.dp, top = 4.dp)
              ) {
                if (log.hadOrgasm) {
                  SymptomBadge("Orgasmo ✨", BrandTerracottaRose)
                }
                if (log.usedLubricant) {
                  SymptomBadge("Con lubricante", PhaseFertileSage)
                }
                if (log.postContactEffect != "NONE") {
                  val effectLabel = when (log.postContactEffect) {
                    "AROUSAL_FLUID" -> "Fluido de excitación (trasudado)"
                    "PH_SHIFT" -> "Flujo acuoso (semen/pH)"
                    "CONTACT_SPOTTING" -> "Manchado leve por roce"
                    "FRICTION_SENSITIVITY" -> "Sensibilidad por fricción"
                    else -> log.postContactEffect
                  }
                  SymptomBadge(effectLabel, PhaseFollicularPeach)
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = PhaseFertileSubtle,
            modifier = Modifier.fillMaxWidth().border(0.5.dp, PhaseFertileSage.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
          ) {
            Text(
              text = "🧬 Recuerda: El fluido de excitación y el semen alteran temporalmente la consistencia del flujo y el pH durante 24-48 horas.",
              fontSize = 11.sp,
              color = PhaseFertileSage,
              lineHeight = 15.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SymptomBadge(label: String, color: Color) {
  Surface(
    shape = RoundedCornerShape(8.dp),
    color = color.copy(alpha = 0.12f),
    modifier = Modifier.border(0.5.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
  ) {
    Text(
      text = label,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold,
      color = color,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
    )
  }
}

private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
