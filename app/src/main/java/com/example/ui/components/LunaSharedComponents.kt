package com.example.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.theme.LunaSandText
import com.example.ui.theme.LunaSurfaceLight
import com.example.ui.theme.LunaTextPrimary
import com.example.ui.theme.LunaTextSecondary

enum class LunaBadgeStyle {
  FACT,
  ESTIMATE,
  NEUTRAL,
  PRIVATE,
  CORAL
}

enum class LunaButtonType {
  PRIMARY,
  SECONDARY,
  DANGER,
  DISABLED
}

/**
 * LunaButton: Botón principal de 52dp de altura y 16dp de radio según Figma Component Set 4:14
 */
@Composable
fun LunaButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  type: LunaButtonType = LunaButtonType.PRIMARY,
  enabled: Boolean = true,
  loading: Boolean = false,
  icon: ImageVector? = null
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val actualEnabled = enabled && !loading && type != LunaButtonType.DISABLED

  val containerColor = when (type) {
    LunaButtonType.PRIMARY -> if (isDark) LunaIceBlueAccent else LunaNavyPrimary
    LunaButtonType.SECONDARY -> if (isDark) LunaDarkSurface else LunaSurfaceLight
    LunaButtonType.DANGER -> LunaCoralSoft
    LunaButtonType.DISABLED -> if (isDark) LunaDarkBorder else Color(0xFFEDF2F7)
  }

  val contentColor = when (type) {
    LunaButtonType.PRIMARY -> if (isDark) LunaNavyPrimary else Color.White
    LunaButtonType.SECONDARY -> if (isDark) Color.White else LunaNavyPrimary
    LunaButtonType.DANGER -> LunaCoralText
    LunaButtonType.DISABLED -> LunaTextSecondary
  }

  val border = when (type) {
    LunaButtonType.SECONDARY -> BorderStroke(1.dp, if (isDark) LunaDarkBorder else LunaBorderLight)
    else -> null
  }

  Button(
    onClick = onClick,
    enabled = actualEnabled,
    shape = RoundedCornerShape(16.dp),
    border = border,
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = if (isDark) LunaDarkBorder else Color(0xFFEDF2F7),
      disabledContentColor = LunaTextSecondary
    ),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 52.dp)
  ) {
    if (loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = contentColor
      )
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = "Guardando…",
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
      )
    } else {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.SemiBold
      )
    }
  }
}

// Aliases para compatibilidad con código existente
@Composable
fun LunaPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  loading: Boolean = false,
  containerColor: Color = LunaNavyPrimary,
  contentColor: Color = Color.White
) {
  LunaButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    type = LunaButtonType.PRIMARY,
    enabled = enabled,
    loading = loading,
    icon = icon
  )
}

@Composable
fun LunaSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
  borderColor: Color = LunaBorderLight
) {
  LunaButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    type = LunaButtonType.SECONDARY,
    enabled = enabled,
    icon = icon
  )
}

@Composable
fun LunaDestructiveButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true
) {
  LunaButton(
    text = text,
    onClick = onClick,
    modifier = modifier,
    type = LunaButtonType.DANGER,
    enabled = enabled,
    icon = icon
  )
}

/**
 * LunaBadge: Insignias oficiales según Component Set 4:26 de Figma
 * - Registrado: #EAF2EC / #376C59 (Salvia)
 * - Estimado: #F4EEE4 / #795D36 (Arena)
 * - Solo tú: #E6EFF9 / #173E68 (Azul Hielo)
 */
@Composable
fun LunaBadge(
  text: String,
  modifier: Modifier = Modifier,
  style: LunaBadgeStyle = LunaBadgeStyle.NEUTRAL,
  icon: ImageVector? = null,
  shape: Shape = RoundedCornerShape(12.dp)
) {
  val (bgColor, txtColor) = when (style) {
    LunaBadgeStyle.FACT -> Pair(LunaSageSoft, LunaSageText)
    LunaBadgeStyle.ESTIMATE -> Pair(LunaSandSoft, LunaSandText)
    LunaBadgeStyle.PRIVATE -> Pair(LunaIceBlueSubtle, LunaNavyPrimary)
    LunaBadgeStyle.CORAL -> Pair(LunaCoralSoft, LunaPeriodRed)
    LunaBadgeStyle.NEUTRAL -> Pair(MaterialTheme.colorScheme.surfaceVariant, MaterialTheme.colorScheme.onSurfaceVariant)
  }

  Surface(
    shape = shape,
    color = bgColor,
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = txtColor,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = txtColor,
        letterSpacing = 0.2.sp
      )
    }
  }
}

@Composable
fun LunaBadge(
  text: String,
  backgroundColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  shape: Shape = RoundedCornerShape(12.dp)
) {
  Surface(
    shape = shape,
    color = backgroundColor,
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
      if (icon != null) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = textColor,
          modifier = Modifier.size(12.dp)
        )
        Spacer(modifier = Modifier.width(4.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Medium,
        color = textColor,
        letterSpacing = 0.2.sp
      )
    }
  }
}

/**
 * LunaToggle: Interruptor oficial según Component Set 4:19 de Figma (51x31dp, 25dp thumb)
 */
@Composable
fun LunaToggle(
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val trackColor by animateColorAsState(
    targetValue = if (checked) {
      if (isDark) LunaIceBlueAccent else LunaNavyPrimary
    } else {
      if (isDark) LunaNavyBorderDark else LunaBorderLight
    },
    label = "trackColor"
  )

  val thumbOffset by animateDpAsState(
    targetValue = if (checked) 23.dp else 3.dp,
    label = "thumbOffset"
  )

  val thumbColor = if (checked && isDark) LunaNavyPrimary else Color.White

  Box(
    modifier = modifier
      .size(width = 51.dp, height = 31.dp)
      .clip(RoundedCornerShape(100.dp))
      .background(trackColor)
      .clickable(
        enabled = enabled,
        role = Role.Switch,
        interactionSource = remember { MutableInteractionSource() },
        indication = null,
        onClick = { onCheckedChange(!checked) }
      ),
    contentAlignment = Alignment.CenterStart
  ) {
    Box(
      modifier = Modifier
        .offset(x = thumbOffset)
        .size(25.dp)
        .clip(CircleShape)
        .background(thumbColor)
    )
  }
}

/**
 * LunaCard: Tarjeta unificada según Figma con 24dp o 16dp de radio y borde tenue
 */
@Composable
fun LunaCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  borderWidth: Dp = 1.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    shape = shape,
    color = backgroundColor,
    tonalElevation = 0.dp,
    border = BorderStroke(borderWidth, borderColor),
    modifier = modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      content()
    }
  }
}

/**
 * LunaNavigationRow: Fila navegable de interacción (Patrones 05, 06, 18, 19, 22, 32, 33, 34)
 */
@Composable
fun LunaNavigationRow(
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  iconTint: Color = LunaNavyPrimary,
  trailingText: String? = null,
  enabled: Boolean = true
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clickable(enabled = enabled, onClick = onClick)
      .padding(vertical = 12.dp)
  ) {
    if (icon != null) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(RoundedCornerShape(12.dp))
          .background(if (isDark) LunaDarkSurface else LunaIceBlueSubtle),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          tint = if (isDark) LunaIceBlueAccent else iconTint,
          modifier = Modifier.size(22.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
    }

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.onBackground
      )
      Spacer(modifier = Modifier.height(2.dp))
      Text(
        text = subtitle,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    if (trailingText != null) {
      Text(
        text = trailingText,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(end = 6.dp)
      )
    }

    Icon(
      imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
      contentDescription = null,
      tint = LunaTextSecondary,
      modifier = Modifier.size(20.dp)
    )
  }
}

/**
 * LunaContextTopBar: Barra superior de contexto ("LUNASYNC / TU ESPACIO", avatar "AS")
 */
@Composable
fun LunaContextTopBar(
  contextText: String,
  avatarInitials: String,
  onAvatarClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 8.dp)
  ) {
    Text(
      text = contextText.uppercase(),
      style = MaterialTheme.typography.labelSmall,
      fontWeight = FontWeight.SemiBold,
      color = if (isDark) LunaDarkTextSecondary else LunaTextSecondary,
      letterSpacing = 1.sp
    )

    Box(
      modifier = Modifier
        .size(36.dp)
        .clip(CircleShape)
        .background(if (isDark) LunaNavyBorderDark else Color(0xFFEDF2F7))
        .clickable(onClick = onAvatarClick),
      contentAlignment = Alignment.Center
    ) {
      Text(
        text = avatarInitials,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.SemiBold,
        color = if (isDark) Color.White else LunaNavyPrimary
      )
    }
  }
}

/**
 * LunaOptionChip: Chip de opción seleccionable (ej. Sangrado: Nada/Manchado/Flujo)
 */
@Composable
fun LunaOptionChip(
  text: String,
  selected: Boolean,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val bgColor = if (selected) {
    if (isDark) LunaNavyBorderDark else LunaIceBlueSubtle
  } else {
    if (isDark) LunaDarkSurface else LunaSurfaceLight
  }

  val textColor = if (selected) {
    if (isDark) LunaIceBlueAccent else LunaNavyPrimary
  } else {
    if (isDark) LunaDarkTextSecondary else LunaTextSecondary
  }

  val borderColor = if (selected) {
    if (isDark) LunaIceBlueAccent else LunaNavyPrimary
  } else {
    if (isDark) LunaDarkBorder else LunaBorderLight
  }

  Surface(
    shape = RoundedCornerShape(12.dp),
    color = bgColor,
    border = BorderStroke(1.dp, borderColor),
    modifier = modifier
      .clickable(onClick = onSelect)
  ) {
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.bodySmall,
        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
        color = textColor,
        textAlign = TextAlign.Center
      )
    }
  }
}

/**
 * LunaBottomNavPill: Navegación flotante según Patrones 03, 08, 16 de Figma (345x56dp aprox, 24dp radio)
 */
@Composable
fun LunaBottomNavPill(
  selectedTab: Int, // 0: Hoy, 1: Calendario, 2: Nosotros
  onTabSelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  val isDark = MaterialTheme.colorScheme.background.red < 0.2f
  val navBg = if (isDark) LunaNavySurfaceDark else LunaSurfaceLight
  val navBorder = if (isDark) LunaNavyBorderDark else LunaBorderLight

  Surface(
    shape = RoundedCornerShape(24.dp),
    color = navBg,
    border = BorderStroke(1.dp, navBorder),
    shadowElevation = 4.dp,
    modifier = modifier
      .fillMaxWidth()
      .height(64.dp)
  ) {
    Row(
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
      LunaNavTabItem(
        label = "Hoy",
        icon = Icons.Default.Home,
        selected = selectedTab == 0,
        onClick = { onTabSelected(0) },
        isDark = isDark,
        modifier = Modifier.weight(1f).testTag("nav_tab_dashboard")
      )

      LunaNavTabItem(
        label = "Calendario",
        icon = Icons.Default.CalendarMonth,
        selected = selectedTab == 1,
        onClick = { onTabSelected(1) },
        isDark = isDark,
        modifier = Modifier.weight(1f).testTag("nav_tab_calendar")
      )

      LunaNavTabItem(
        label = "Nosotros",
        icon = Icons.Default.Favorite,
        selected = selectedTab == 2,
        onClick = { onTabSelected(2) },
        isDark = isDark,
        modifier = Modifier.weight(1f).testTag("nav_tab_nosotros")
      )
    }
  }
}

@Composable
private fun LunaNavTabItem(
  label: String,
  icon: ImageVector,
  selected: Boolean,
  onClick: () -> Unit,
  isDark: Boolean,
  modifier: Modifier = Modifier
) {
  val activeColor = if (isDark) LunaIceBlueAccent else LunaNavyPrimary
  val inactiveColor = if (isDark) Color(0xFFABBACD) else LunaTextSecondary

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.Center,
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 6.dp)
  ) {
    Icon(
      imageVector = icon,
      contentDescription = label,
      tint = if (selected) activeColor else inactiveColor,
      modifier = Modifier.size(22.dp)
    )
    Spacer(modifier = Modifier.height(3.dp))
    Text(
      text = label,
      style = MaterialTheme.typography.labelSmall,
      fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
      color = if (selected) activeColor else inactiveColor
    )
  }
}
