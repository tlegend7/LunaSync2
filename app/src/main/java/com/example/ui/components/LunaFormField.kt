package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun LunaFormField(label: String, value: String, onValueChange: (String) -> Unit,
  modifier: Modifier = Modifier, enabled: Boolean = true, placeholder: String = "") {
  Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
    Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    OutlinedTextField(value, onValueChange, enabled = enabled, minLines = 1, maxLines = 5,
      placeholder = { Text(placeholder) }, shape = RoundedCornerShape(12.dp),
      textStyle = MaterialTheme.typography.bodyLarge,
      colors = OutlinedTextFieldDefaults.colors(
        focusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant),
      modifier = modifier.fillMaxWidth())
  }
}
