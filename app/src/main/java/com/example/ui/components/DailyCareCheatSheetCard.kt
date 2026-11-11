package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.Spa
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.CareTip
import com.example.data.model.CyclePhase
import com.example.ui.theme.LunaNavyPrimary
import com.example.ui.theme.LunaPeriodRed
import com.example.ui.theme.LunaSageSoft
import com.example.ui.theme.LunaSageText
import com.example.ui.theme.LunaSandSoft
import com.example.ui.theme.LunaSandText

@Composable
fun DailyCareCheatSheetCard(
  cyclePhase: CyclePhase?,
  tips: List<CareTip>,
  modifier: Modifier = Modifier
) {
  DailyCareCheatSheetCard(careTips = tips, modifier = modifier)
}

@Composable
fun DailyCareCheatSheetCard(
  careTips: List<CareTip>,
  modifier: Modifier = Modifier
) {
  LunaCard(
    modifier = modifier
      .fillMaxWidth()
      .testTag("daily_care_cheatsheet")
  ) {
    Text(
      text = "GUÍA DE CUIDADOS DIARIOS",
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.Bold,
      color = LunaNavyPrimary,
      letterSpacing = 0.8.sp
    )
    Spacer(modifier = Modifier.height(2.dp))
    Text(
      text = "Opciones de apoyo sugeridas para hoy",
      style = MaterialTheme.typography.titleMedium,
      fontWeight = FontWeight.Bold,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.height(14.dp))

    careTips.forEachIndexed { index, tip ->
      val (icon, tintColor, bgTint) = when (tip.categoryIcon) {
        "tea" -> Triple(Icons.Default.Spa, LunaSageText, LunaSageSoft)
        "massage" -> Triple(Icons.Default.SelfImprovement, LunaNavyPrimary, LunaNavyPrimary.copy(alpha = 0.12f))
        "food" -> Triple(Icons.Default.Restaurant, LunaSandText, LunaSandSoft)
        "star" -> Triple(Icons.Default.Star, LunaNavyPrimary, LunaNavyPrimary.copy(alpha = 0.12f))
        else -> Triple(Icons.Default.Favorite, LunaPeriodRed, LunaPeriodRed.copy(alpha = 0.12f))
      }

      Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .size(36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bgTint)
        ) {
          Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tintColor,
            modifier = Modifier.size(20.dp)
          )
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = tip.title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = tip.description,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 18.sp
          )
        }
      }

      if (index < careTips.lastIndex) {
        HorizontalDivider(
          color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
          thickness = 0.5.dp,
          modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 4.dp)
        )
      }
    }
  }
}
