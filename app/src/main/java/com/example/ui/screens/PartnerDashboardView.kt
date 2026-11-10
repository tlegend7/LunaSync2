package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CareTip
import com.example.data.model.CheckIn
import com.example.data.model.CoupleSpace
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.data.model.DailySymptom
import com.example.data.model.IntimacyLog
import com.example.data.model.PartnerObservation
import com.example.ui.components.CheckInBannerComponent
import com.example.ui.components.DailyCareCheatSheetCard
import com.example.ui.components.LunaBadge
import com.example.ui.components.LunaBadgeStyle
import com.example.ui.components.LunaCard
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.AppleHealthFertile
import com.example.ui.theme.AppleHealthFollicular
import com.example.ui.theme.AppleHealthLuteal
import com.example.ui.theme.AppleHealthMenstrual
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5
import com.example.ui.theme.AppleSystemIndigo
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.BrandWarmRose
import com.example.ui.theme.PhaseFertileSage
import com.example.ui.theme.PhaseFertileSubtle
import com.example.ui.theme.PhaseFollicularPeach
import com.example.ui.theme.PhaseFollicularSubtle
import com.example.ui.theme.PhaseLutealLavender
import com.example.ui.theme.PhaseLutealSubtle
import com.example.ui.theme.PhaseMenstrualRose
import com.example.ui.theme.PhaseMenstrualSubtle
import com.example.ui.theme.StatusError

@Composable
fun PartnerDashboardView(
  space: CoupleSpace?,
  cycleStatus: CycleStatusData?,
  todaySymptom: DailySymptom?,
  intimacyLogs: List<IntimacyLog>,
  partnerCareTips: List<CareTip>,
  latestCheckIn: CheckIn?,
  partnerObservations: List<PartnerObservation>,
  isLoading: Boolean = false,
  errorMessage: String? = null,
  onOpenCheckInDialog: () -> Unit,
  onOpenObservationDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  val shareSymptomsAllowed = space?.sharingPermissions?.shareSymptoms ?: true
  val shareMoodAllowed = space?.sharingPermissions?.shareMood ?: true
  val shareIntimacyAllowed = space?.sharingPermissions?.shareIntimacy ?: false

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 18.dp, vertical = 8.dp)
  ) {
    if (isLoading) {
      LunaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(12.dp)
        ) {
          CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = BrandTerracottaRose)
          Spacer(modifier = Modifier.width(14.dp))
          Text("Cargando información del ciclo...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    } else if (errorMessage != null) {
      LunaCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = StatusError.copy(alpha = 0.5f)
      ) {
        Text("ESTADO DEL CICLO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = StatusError)
        Spacer(modifier = Modifier.height(6.dp))
        Text("No se pudo cargar la información", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(4.dp))
        Text(errorMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    } else if (cycleStatus == null) {
      LunaCard(modifier = Modifier.fillMaxWidth()) {
        LunaBadge(
          text = "Sin registros de ciclo",
          style = LunaBadgeStyle.NEUTRAL
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
          text = "Esperando primer registro",
          style = MaterialTheme.typography.headlineSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "Tu pareja aún no ha registrado el inicio de su ciclo menstrual. Tan pronto anote su periodo, verás aquí su fase estimada, días calculados y sugerencias de acompañamiento.",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 21.sp
        )
      }
    } else {
      val phase = cycleStatus.currentPhase
      val currentDay = cycleStatus.currentDay
      val daysUntilPeriod = cycleStatus.daysUntilNextPeriod
      val isDelayed = cycleStatus.isDelayed

      val (phaseName, phaseColor, phaseBg) = when (phase) {
        CyclePhase.MENSTRUAL -> Triple("Fase Menstrual", PhaseMenstrualRose, PhaseMenstrualSubtle)
        CyclePhase.FOLLICULAR -> Triple("Fase Folicular", PhaseFollicularPeach, PhaseFollicularSubtle)
        CyclePhase.OVULATORY -> Triple("Fase Ovulatoria • Ventana Fértil", PhaseFertileSage, PhaseFertileSubtle)
        CyclePhase.LUTEAL -> Triple("Fase Lútea • Fase de Cuidado", PhaseLutealLavender, PhaseLutealSubtle)
        CyclePhase.DELAYED -> Triple("Fecha Estimada Superada", PhaseLutealLavender, PhaseLutealSubtle)
      }

      val phaseDescription = when {
        isDelayed || phase == CyclePhase.DELAYED -> "Se ha superado la fecha estimada para el próximo periodo (Día $currentDay, ${cycleStatus.delayedDays}d de retraso). Cada ciclo presenta variaciones normales por descanso o estrés. Bríndale empatía y apoyo sin presiones."
        phase == CyclePhase.MENSTRUAL -> "Niveles basales de energía y sensibilidad física. El mejor apoyo es calidez, bebidas tibias y espacio para descansar."
        phase == CyclePhase.FOLLICULAR -> "El estrógeno aumenta gradualmente. Su vitalidad y claridad mental suben. Excelente momento para salidas y proyectos compartidos."
        phase == CyclePhase.OVULATORY -> "Ventana de máxima fertilidad y conexión socioemocional."
        else -> "La progesterona acompaña esta etapa. Puede experimentar mayor necesidad de calma o sensibilidad. Escúchala con paciencia."
      }

      val periodStatusText = when {
        daysUntilPeriod > 0 -> "~$daysUntilPeriod días para periodo"
        daysUntilPeriod == 0 -> "Periodo estimado hoy"
        else -> {
          val delayedDays = -daysUntilPeriod
          "Retraso estimado (~$delayedDays ${if (delayedDays == 1) "día" else "días"})"
        }
      }

      LunaCard(modifier = Modifier.fillMaxWidth()) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = phaseBg
          ) {
            Text(
              text = if (isDelayed) "DÍA $currentDay • FECHA SUPERADA" else "DÍA $currentDay DEL CICLO",
              style = MaterialTheme.typography.labelSmall,
              fontWeight = FontWeight.Bold,
              color = phaseColor,
              letterSpacing = 0.5.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
          }

          Text(
            text = periodStatusText,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (isDelayed) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isDelayed) StatusError else MaterialTheme.colorScheme.onSurfaceVariant
          )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
          text = phaseName,
          style = MaterialTheme.typography.headlineMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = phaseDescription,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 21.sp
        )

        Spacer(modifier = Modifier.height(10.dp))
        LunaBadge(
          text = if (cycleStatus.isBasedOnHistory) "Basado en historial confirmado" else "Estimación preliminar (estándar ${cycleStatus.totalDays}d)",
          style = if (cycleStatus.isBasedOnHistory) LunaBadgeStyle.FACT else LunaBadgeStyle.ESTIMATE
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Check-in Banner Feedback
    if (latestCheckIn != null) {
      CheckInBannerComponent(
        checkIn = latestCheckIn,
        isElla = false,
        onQuickReplySelected = {},
        modifier = Modifier.padding(bottom = 12.dp)
      )
    }

    // Action Buttons
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      Button(
        onClick = onOpenCheckInDialog,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = BrandTerracottaRose),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .testTag("partner_send_checkin_cta")
      ) {
        Icon(Icons.Default.QuestionAnswer, contentDescription = null, modifier = Modifier.size(17.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Enviar Check-In", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }

      FilledTonalButton(
        onClick = onOpenObservationDialog,
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
          containerColor = MaterialTheme.colorScheme.surface,
          contentColor = PhaseLutealLavender
        ),
        modifier = Modifier
          .weight(1f)
          .height(48.dp)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
          .testTag("partner_log_observation_cta")
      ) {
        Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(17.dp), tint = PhaseLutealLavender)
        Spacer(modifier = Modifier.width(6.dp))
        Text("Anotar Cuidado", fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Daily Care Cheat Sheet
    if (partnerCareTips.isNotEmpty()) {
      DailyCareCheatSheetCard(careTips = partnerCareTips)
    } else {
      LunaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "GUÍA DE CUIDADOS DIARIOS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
          text = "Sin recomendaciones activas",
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = "Las sugerencias de apoyo diario se activarán automáticamente según la fase biológica cuando se registre el ciclo.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // What Ella is feeling today
    LunaCard(modifier = Modifier.fillMaxWidth()) {
      Text(
        text = "ESTADO REPORTADO POR ELLA",
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 0.5.sp
      )
      Text(
        text = "Bienestar Compartido",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold
      )

      Spacer(modifier = Modifier.height(12.dp))

      if (isLoading) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
          Spacer(modifier = Modifier.width(8.dp))
          Text("Cargando información compartida...", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      } else if (errorMessage != null) {
        Text(
          text = "No fue posible cargar el estado: $errorMessage",
          style = MaterialTheme.typography.bodySmall,
          color = StatusError
        )
      } else if (!shareSymptomsAllowed && !shareMoodAllowed) {
        // Explicit Privacy State: Categories NOT shared
        Surface(
          shape = RoundedCornerShape(12.dp),
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(12.dp)
          ) {
            Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(
              text = "Tu pareja ha desactivado la visibilidad de sus síntomas físicos y estado emocional. Respeta su privacidad y acompáñala con un Check-in.",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 18.sp
            )
          }
        }
      } else if (todaySymptom != null) {
        // If Ella set isPrivate = true on this specific entry
        if (todaySymptom.isPrivate) {
          Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(12.dp)
            ) {
              Icon(Icons.Default.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Ella prefirió mantener los detalles de hoy en privado. Acompáñala con calidez y dale su espacio si lo necesita.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
              )
            }
          }
        } else {
          Row(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            if (shareSymptomsAllowed) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Flujo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(todaySymptom.flowLevel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
              Column(modifier = Modifier.weight(1f)) {
                Text("Molestia", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${todaySymptom.painLevel} / 5", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
            } else {
              Column(modifier = Modifier.weight(2f)) {
                Text("Síntomas Físicos", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No compartidos", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }

            if (shareMoodAllowed) {
              Column(modifier = Modifier.weight(1f)) {
                Text("Ánimo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                val moodText = if (todaySymptom.mood.isNotBlank()) todaySymptom.mood else "Sin registrar"
                Text(moodText, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
              }
              if (todaySymptom.energyLevel > 0) {
                Column(modifier = Modifier.weight(1f)) {
                  Text("Energía", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  Text("${todaySymptom.energyLevel} / 5", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
              }
            } else {
              Column(modifier = Modifier.weight(1f)) {
                Text("Ánimo", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("No compartido", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
              }
            }
          }

          if (shareSymptomsAllowed && !todaySymptom.publicNote.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
              text = "Mensaje de tu pareja: \"${todaySymptom.publicNote}\"",
              style = MaterialTheme.typography.bodySmall,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      } else {
        Text(
          text = "Ella aún no ha registrado síntomas para hoy. Puedes consultarle con un Check-in afectivo.",
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Shared Intimacy History
    if (shareIntimacyAllowed && intimacyLogs.isNotEmpty()) {
      LunaCard(modifier = Modifier.fillMaxWidth()) {
        Text(
          text = "ENCUENTROS ÍNTIMOS COMPARTIDOS",
          style = MaterialTheme.typography.labelSmall,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          letterSpacing = 0.5.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        intimacyLogs.take(2).forEach { log ->
          val typeLabel = when (log.encounterType) {
            "PROTECTED" -> "Con protección"
            "UNPROTECTED" -> "Sin protección"
            "OUTERCOURSE" -> "Caricias íntimas"
            else -> log.encounterType
          }
          Text(
            text = "• $typeLabel (${log.date})",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(vertical = 3.dp)
          )
        }

        Spacer(modifier = Modifier.height(10.dp))
        Surface(
          shape = RoundedCornerShape(10.dp),
          color = PhaseFertileSubtle,
          modifier = Modifier
            .fillMaxWidth()
            .border(0.5.dp, PhaseFertileSage.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
        ) {
          Text(
            text = "💡 Para cuidar su salud íntima: Mantén manos muy limpias antes de cualquier contacto para proteger su flora protectora y su pH ácido natural.",
            fontSize = 11.sp,
            color = PhaseFertileSage,
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
          )
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
        Column(modifier = Modifier.padding(18.dp)) {
          Text(
            text = "ENCUENTROS ÍNTIMOS COMPARTIDOS",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.5.sp
          )
          Spacer(modifier = Modifier.height(8.dp))
          intimacyLogs.take(2).forEach { log ->
            val typeLabel = when (log.encounterType) {
              "PROTECTED" -> "Con protección"
              "UNPROTECTED" -> "Sin protección"
              "OUTERCOURSE" -> "Caricias íntimas"
              else -> log.encounterType
            }
            Text(
              text = "• $typeLabel (${log.date})",
              style = MaterialTheme.typography.bodySmall,
              fontWeight = FontWeight.Medium,
              color = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.padding(vertical = 3.dp)
            )
          }

          Spacer(modifier = Modifier.height(10.dp))
          Surface(
            shape = RoundedCornerShape(10.dp),
            color = AppleHealthFertile.copy(alpha = 0.12f),
            modifier = Modifier.fillMaxWidth().border(0.5.dp, AppleHealthFertile.copy(alpha = 0.25f), RoundedCornerShape(10.dp))
          ) {
            Text(
              text = "💡 Para cuidar su salud íntima: Mantén manos muy limpias antes de cualquier contacto para proteger su flora protectora y su pH ácido natural.",
              fontSize = 11.sp,
              color = AppleHealthFertile,
              lineHeight = 15.sp,
              modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
            )
          }
        }
      }
      Spacer(modifier = Modifier.height(14.dp))
    }
  }
}
