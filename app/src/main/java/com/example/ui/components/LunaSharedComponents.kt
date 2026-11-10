package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppleHairlineBorder
import com.example.ui.theme.BrandTerracottaRose
import com.example.ui.theme.PhaseFertileSage
import com.example.ui.theme.PhaseFollicularPeach
import com.example.ui.theme.PhaseLutealLavender
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusSuccess

enum class LunaBadgeStyle {
  FACT,
  ESTIMATE,
  NEUTRAL
}

/**
 * LunaCard: Tarjeta unificada para contener secciones con elevación suave,
 * esquinas redondeadas generosas y borde cálido anti-fatiga.
 */
@Composable
fun LunaCard(
  modifier: Modifier = Modifier,
  shape: Shape = RoundedCornerShape(20.dp),
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  borderWidth: Dp = 0.5.dp,
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    shape = shape,
    color = backgroundColor,
    tonalElevation = 1.dp,
    modifier = modifier
      .fillMaxWidth()
      .border(borderWidth, borderColor, shape)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      content()
    }
  }
}

/**
 * LunaBadge: Píldora de estado o categoría con distinción explícita de HECHO vs ESTIMACIÓN.
 */
@Composable
fun LunaBadge(
  text: String,
  modifier: Modifier = Modifier,
  style: LunaBadgeStyle = LunaBadgeStyle.NEUTRAL,
  icon: ImageVector? = null,
  shape: Shape = RoundedCornerShape(10.dp)
) {
  val (bgColor, txtColor) = when (style) {
    LunaBadgeStyle.FACT -> Pair(StatusSuccess.copy(alpha = 0.12f), StatusSuccess)
    LunaBadgeStyle.ESTIMATE -> Pair(PhaseFollicularPeach.copy(alpha = 0.18f), BrandTerracottaRose)
    LunaBadgeStyle.NEUTRAL -> Pair(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f), MaterialTheme.colorScheme.onSurfaceVariant)
  }

  Surface(
    shape = shape,
    color = bgColor,
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
        fontWeight = FontWeight.Bold,
        color = txtColor,
        letterSpacing = 0.4.sp
      )
    }
  }
}

/**
 * LunaBadge con colores directos personalizados.
 */
@Composable
fun LunaBadge(
  text: String,
  backgroundColor: Color,
  textColor: Color,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  shape: Shape = RoundedCornerShape(10.dp)
) {
  Surface(
    shape = shape,
    color = backgroundColor,
    modifier = modifier
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
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
        fontWeight = FontWeight.Bold,
        color = textColor,
        letterSpacing = 0.4.sp
      )
    }
  }
}

/**
 * LunaPrimaryButton: Botón de acción principal con altura táctil adecuada (48-50dp).
 */
@Composable
fun LunaPrimaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  loading: Boolean = false,
  containerColor: Color = BrandTerracottaRose,
  contentColor: Color = Color.White
) {
  Button(
    onClick = onClick,
    enabled = enabled && !loading,
    shape = RoundedCornerShape(14.dp),
    colors = ButtonDefaults.buttonColors(
      containerColor = containerColor,
      contentColor = contentColor,
      disabledContainerColor = containerColor.copy(alpha = 0.5f),
      disabledContentColor = contentColor.copy(alpha = 0.7f)
    ),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
  ) {
    if (loading) {
      CircularProgressIndicator(
        modifier = Modifier.size(20.dp),
        strokeWidth = 2.dp,
        color = contentColor
      )
    } else {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
      }
      Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
    }
  }
}

/**
 * LunaSecondaryButton: Botón de acción secundaria con borde tenue.
 */
@Composable
fun LunaSecondaryButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true,
  containerColor: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = MaterialTheme.colorScheme.onSurface,
  borderColor: Color = AppleHairlineBorder
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(16.dp),
    border = BorderStroke(1.dp, borderColor),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = containerColor,
      contentColor = contentColor
    ),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = contentColor)
      Spacer(modifier = Modifier.width(6.dp))
    }
    Text(text = text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
  }
}

/**
 * LunaDestructiveButton: Botón para acciones irreversibles o desvinculación con aviso de riesgo visual claro.
 */
@Composable
fun LunaDestructiveButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  enabled: Boolean = true
) {
  OutlinedButton(
    onClick = onClick,
    enabled = enabled,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, StatusError.copy(alpha = 0.5f)),
    colors = ButtonDefaults.outlinedButtonColors(
      containerColor = Color.Transparent,
      contentColor = StatusError
    ),
    modifier = modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
  ) {
    if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = StatusError)
      Spacer(modifier = Modifier.width(6.dp))
    }
    Text(text = text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = StatusError)
  }
}
