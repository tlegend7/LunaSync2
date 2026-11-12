package com.example.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import com.example.R

/** Figma's exported Lucide vectors, keeping each original viewport and stroke geometry. */
object LunaIcons {
  val ConfirmationShield: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_observation_shield)
  val Plus: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_plus)
  val Home: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_nav_home)
  val Calendar: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_nav_calendar)
  val Space: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_nav_heart)
  val Heart: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_heart)
  val HeroHeart: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_hero_heart)
  val ChevronRight: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_chevron_right)
  val CalendarPrevious: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_calendar_chevron_left)
  val CalendarNext: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_calendar_chevron_right)
  val Droplets: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_droplets)
  val Lock: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_record_lock)
  val Message: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_message_circle)
  val Check: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_check)
  val Shield: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_shield_check)
  val Link: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_link)
  val Sliders: ImageVector @Composable get() = ImageVector.vectorResource(R.drawable.luna_sliders_horizontal)
}
