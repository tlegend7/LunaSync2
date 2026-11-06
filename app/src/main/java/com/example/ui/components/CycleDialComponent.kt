package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CyclePhase
import com.example.data.model.CycleStatusData
import com.example.ui.theme.AppleHealthFollicular
import com.example.ui.theme.AppleHealthFertile
import com.example.ui.theme.AppleHealthLuteal
import com.example.ui.theme.AppleHealthMenstrual
import com.example.ui.theme.AppleSystemBlue
import com.example.ui.theme.AppleSystemGray5
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun CycleDialComponent(
  cycleStatus: CycleStatusData?,
  modifier: Modifier = Modifier
) {
  val currentDay = cycleStatus?.currentDay ?: 1
  val totalDays = cycleStatus?.totalDays ?: 28
  val phase = cycleStatus?.currentPhase ?: CyclePhase.FOLLICULAR
  val daysUntilPeriod = cycleStatus?.daysUntilNextPeriod ?: 14

  val progress = (currentDay.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
  val animatedProgress by animateFloatAsState(
    targetValue = progress,
    animationSpec = spring(dampingRatio = 0.85f, stiffness = 300f),
    label = "dial_progress"
  )

  val (phaseName, phaseColor, phaseBg) = when (phase) {
    CyclePhase.MENSTRUAL -> Triple("Menstrual", AppleHealthMenstrual, AppleHealthMenstrual.copy(alpha = 0.12f))
    CyclePhase.FOLLICULAR -> Triple("Folicular", AppleHealthFollicular, AppleHealthFollicular.copy(alpha = 0.12f))
    CyclePhase.OVULATORY -> Triple("Fértil • Ovulación", AppleHealthFertile, AppleHealthFertile.copy(alpha = 0.12f))
    CyclePhase.LUTEAL -> Triple("Lútea", AppleHealthLuteal, AppleHealthLuteal.copy(alpha = 0.12f))
  }

  Box(
    contentAlignment = Alignment.Center,
    modifier = modifier
      .size(270.dp)
      .testTag("cycle_dial_component")
  ) {
    // Subtle Apple-like inner ambient halo
    Box(
      modifier = Modifier
        .size(190.dp)
        .clip(CircleShape)
        .background(
          Brush.radialGradient(
            colors = listOf(
              phaseColor.copy(alpha = 0.10f),
              Color.Transparent
            )
          )
        )
    )

    Canvas(modifier = Modifier.fillMaxSize().padding(14.dp)) {
      val strokeWidth = 10.dp.toPx()
      val diameter = size.minDimension - strokeWidth - 20.dp.toPx()
      val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)
      val arcSize = Size(diameter, diameter)
      val centerX = size.width / 2
      val centerY = size.height / 2
      val radius = diameter / 2

      // Discrete day ticks (Apple Health / Watch signature dial)
      val tickRadius = radius + 12.dp.toPx()
      for (i in 0 until totalDays) {
        val angle = Math.toRadians((i.toDouble() / totalDays.toDouble() * 360.0) - 90.0)
        val x = (centerX + tickRadius * cos(angle)).toFloat()
        val y = (centerY + tickRadius * sin(angle)).toFloat()
        val isPassed = i < currentDay
        val isToday = i == (currentDay - 1)

        val dotColor = when {
          isToday -> phaseColor
          isPassed -> phaseColor.copy(alpha = 0.5f)
          else -> Color(0xFFD1D1D6).copy(alpha = 0.6f)
        }
        val dotSize = if (isToday) 4.5.dp.toPx() else 2.5.dp.toPx()
        drawCircle(color = dotColor, radius = dotSize, center = Offset(x, y))
      }

      // Base Track (Apple neutral subtle track)
      drawArc(
        color = Color(0xFFE5E5EA).copy(alpha = 0.7f),
        startAngle = -90f,
        sweepAngle = 360f,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // Active Arc with refined gradient
      val sweepAngle = animatedProgress * 360f
      drawArc(
        brush = Brush.sweepGradient(
          colors = listOf(
            phaseColor.copy(alpha = 0.5f),
            phaseColor,
            phaseColor
          )
        ),
        startAngle = -90f,
        sweepAngle = sweepAngle,
        useCenter = false,
        topLeft = topLeft,
        size = arcSize,
        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
      )

      // Glowing Indicator Knob (Apple Watch style with crisp shadow ring)
      val angleRad = Math.toRadians((sweepAngle - 90).toDouble())
      val knobX = (centerX + radius * cos(angleRad)).toFloat()
      val knobY = (centerY + radius * sin(angleRad)).toFloat()

      // Shadow layer
      drawCircle(
        color = Color(0x33000000),
        radius = strokeWidth * 0.9f,
        center = Offset(knobX, knobY + 1.5f)
      )
      // Outer crisp white
      drawCircle(
        color = Color.White,
        radius = strokeWidth * 0.8f,
        center = Offset(knobX, knobY)
      )
      // Inner jewel color
      drawCircle(
        color = phaseColor,
        radius = strokeWidth * 0.5f,
        center = Offset(knobX, knobY)
      )
    }

    // Central Display (Apple HIG Minimal Typography)
    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier.padding(horizontal = 24.dp)
    ) {
      Text(
        text = "DÍA DEL CICLO",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold
      )
      Text(
        text = "$currentDay",
        fontSize = 54.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        lineHeight = 58.sp,
        letterSpacing = (-1.5).sp
      )

      // Apple Health Style Phase Pill
      Surface(
        shape = RoundedCornerShape(12.dp),
        color = phaseBg,
        modifier = Modifier.padding(vertical = 4.dp)
      ) {
        Text(
          text = phaseName,
          style = MaterialTheme.typography.labelMedium,
          fontWeight = FontWeight.Bold,
          color = phaseColor,
          modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
      }

      Spacer(modifier = Modifier.height(4.dp))

      Text(
        text = if (daysUntilPeriod > 0) "Próximo periodo en $daysUntilPeriod días" else "Periodo esperado hoy",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }
  }
}
