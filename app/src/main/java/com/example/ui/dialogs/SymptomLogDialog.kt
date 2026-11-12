package com.example.ui.dialogs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.*
import com.example.ui.components.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SymptomLogDialog(
  initialSymptom: DailySymptom?,
  date: LocalDate = LocalDate.now(),
  onDismiss: () -> Unit,
  onSave: (flowLevel: FlowLevel, painLevel: Int, mood: Mood, energyLevel: Int,
    cramps: Boolean, headache: Boolean, backPain: Boolean, bloating: Boolean, cervicalMucus: CervicalMucus,
    tenderBreasts: Boolean, cravings: Boolean, acne: Boolean, publicNote: String?, privateNote: String?,
    isPrivate: Boolean, isMoodPrivate: Boolean, onComplete: (Boolean, String?) -> Unit) -> Unit
) {
  var flow by remember { mutableStateOf(FlowLevel.entries.firstOrNull { it.name == initialSymptom?.flowLevel } ?: FlowLevel.NONE) }
  var pain by remember { mutableStateOf(initialSymptom?.painLevel ?: 0) }
  var mood by remember { mutableStateOf(Mood.entries.firstOrNull { it.name == initialSymptom?.mood } ?: Mood.CALM) }
  var energy by remember { mutableStateOf(initialSymptom?.energyLevel?.takeIf { it in 1..5 } ?: 3) }
  var mucus by remember { mutableStateOf(CervicalMucus.entries.firstOrNull { it.name == initialSymptom?.cervicalMucus } ?: CervicalMucus.NONE) }
  var cramps by remember { mutableStateOf(initialSymptom?.cramps ?: false) }
  var headache by remember { mutableStateOf(initialSymptom?.headache ?: false) }
  var backPain by remember { mutableStateOf(initialSymptom?.backPain ?: false) }
  var bloating by remember { mutableStateOf(initialSymptom?.bloating ?: false) }
  var breasts by remember { mutableStateOf(initialSymptom?.tenderBreasts ?: false) }
  var cravings by remember { mutableStateOf(initialSymptom?.cravings ?: false) }
  var acne by remember { mutableStateOf(initialSymptom?.acne ?: false) }
  var publicNote by remember { mutableStateOf(initialSymptom?.publicNote.orEmpty()) }
  var privateNote by remember { mutableStateOf(initialSymptom?.privateNote.orEmpty()) }
  var physicalPrivate by remember { mutableStateOf(initialSymptom?.isPrivate ?: false) }
  var moodPrivate by remember { mutableStateOf(initialSymptom?.let { it.isMoodPrivate ?: it.isPrivate } ?: false) }
  var busy by remember { mutableStateOf(false) }
  var error by remember { mutableStateOf<String?>(null) }
  var showDetails by remember { mutableStateOf(false) }
  var showMoods by remember { mutableStateOf(false) }
  val locale = Locale.forLanguageTag("es-ES")
  val dateLabel = date.format(DateTimeFormatter.ofPattern("EEEE, d 'de' MMMM 'de' yyyy", locale))
    .replaceFirstChar { it.titlecase(locale) }
  val largeText = LocalDensity.current.fontScale > 1.15f
  val bloodLevels = listOf(FlowLevel.LIGHT, FlowLevel.MEDIUM, FlowLevel.HEAVY)

  LunaFlowDialog(if (error == null) "¿Cómo te sientes?" else "Tu registro sigue\naquí.", dateLabel, onDismiss, "symptom_log_dialog", busy = busy,
    subtitleModifier = Modifier.testTag("symptom_dialog_date_text"), actions = {
      error?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.testTag("symptom_error")) }
      LunaButton(if (error == null) "Guardar registro" else "Reintentar guardado", {
        if (!busy) {
          busy = true
          error = null
          onSave(flow, pain, mood, energy, cramps, headache, backPain, bloating, mucus, breasts, cravings, acne,
            publicNote.trim().takeIf { it.isNotEmpty() }, privateNote.trim().takeIf { it.isNotEmpty() }, physicalPrivate, moodPrivate) { success, failure ->
            busy = false
            if (success) onDismiss() else error = failure ?: "No se pudo guardar el registro. Intenta nuevamente."
          }
        }
      }, Modifier.testTag("save_symptom_button"), loading = busy)
    }) {
    error?.let {
      Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.errorContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Icon(LunaIcons.ConfirmationShield, null, Modifier.size(22.dp), tint = MaterialTheme.colorScheme.primary)
          Text("No pudimos guardar", style = MaterialTheme.typography.titleMedium)
          Text("Conservamos lo que escribiste en este formulario. Revisa el aviso y vuelve a intentarlo.",
            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
      LunaBadge("Sin guardar todavía", style = LunaBadgeStyle.ESTIMATE)
    }
    LunaBadge("Fecha del registro · " + date.format(DateTimeFormatter.ofPattern("d MMM", locale)), style = LunaBadgeStyle.PRIVATE)
    Text("Sangrado", style = MaterialTheme.typography.titleLarge)
    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val choiceModifier = if (largeText) Modifier.widthIn(min = 64.dp) else Modifier.weight(1f)
      LunaOptionChip("Nada", flow == FlowLevel.NONE, { flow = FlowLevel.NONE }, choiceModifier.testTag("flow_chip_NONE"), enabled = !busy)
      LunaOptionChip("Manchado", flow == FlowLevel.SPOTTING, { flow = FlowLevel.SPOTTING }, choiceModifier.testTag("flow_chip_SPOTTING"), enabled = !busy)
      LunaOptionChip("Flujo", flow in bloodLevels, { if (flow !in bloodLevels) flow = FlowLevel.LIGHT },
        choiceModifier.testTag("flow_has_bleeding"), enabled = !busy)
    }
    if (flow in bloodLevels) {
      Text("Intensidad del flujo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
      FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val choiceModifier = if (largeText) Modifier.widthIn(min = 64.dp) else Modifier.weight(1f)
        bloodLevels.forEach { level ->
          LunaOptionChip(when (level) { FlowLevel.LIGHT -> "Ligero"; FlowLevel.MEDIUM -> "Medio"; else -> "Abundante" },
            flow == level, { flow = level }, choiceModifier.testTag("flow_chip_${level.name}"), enabled = !busy)
        }
      }
    }
    Text("Dolor", style = MaterialTheme.typography.titleLarge)
    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val choiceModifier = if (largeText) Modifier.widthIn(min = 64.dp) else Modifier.weight(1f)
      listOf(0 to "Nada", 1 to "Leve", 3 to "Medio", 5 to "Fuerte").forEach { (level, label) ->
        LunaOptionChip(label, pain == level, { pain = level }, choiceModifier.testTag("pain_level_$level"), enabled = !busy)
      }
    }
    if (pain in listOf(2, 4)) Text("Dolor registrado: $pain/5", style = MaterialTheme.typography.bodySmall)
    Text("Ánimo y energía", style = MaterialTheme.typography.titleLarge)
    FlowRow(Modifier.fillMaxWidth().selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)) {
      val choiceModifier = if (largeText) Modifier.widthIn(min = 64.dp) else Modifier.weight(1f)
      listOf(Mood.CALM to "En calma", Mood.SENSITIVE to "Sensible").forEach { (value, label) ->
        LunaOptionChip(label, mood == value, { mood = value }, choiceModifier, enabled = !busy)
      }
      TextButton({ showMoods = !showMoods }, enabled = !busy,
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
        modifier = choiceModifier.testTag("other_moods_button")) {
        Text("Otros estados", style = MaterialTheme.typography.bodySmall)
      }
    }
    if (showMoods) FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Mood.entries.forEach { value ->
        LunaOptionChip(moodLabel(value), mood == value, { mood = value }, Modifier.testTag("mood_chip_${value.name}"), enabled = !busy)
      }
    }
    Text("Ánimo: ${moodLabel(mood)} · Energía: $energy/5", style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant)
    Slider(energy.toFloat(), { energy = it.roundToInt() }, enabled = !busy, valueRange = 1f..5f, steps = 3,
      modifier = Modifier.fillMaxWidth().testTag("energy_slider"))
    Text("1 · Muy baja     5 · Muy alta", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    LunaFormField("Nota privada", privateNote, { privateNote = it }, Modifier.testTag("symptom_private_note_input"),
      enabled = !busy, placeholder = "Algo que quieras recordar para ti.")
    Text("Esta nota siempre es solo para ti.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    SymptomSwitch("Compartir síntomas", "Según tus permisos actuales.", !physicalPrivate, { physicalPrivate = !it }, "symptom_privacy_switch", !busy)
    SymptomSwitch("Compartir ánimo y energía", "Puedes decidirlo por separado.", !moodPrivate, { moodPrivate = !it }, "mood_privacy_switch", !busy)
    Text("También debe estar activo el permiso correspondiente en Privacidad. Tu nota privada no se comparte.",
      style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton({ showDetails = !showDetails }, enabled = !busy, modifier = Modifier.testTag("symptom_details_button")) {
      Text(if (showDetails) "Ocultar detalles" else "Añadir más detalles")
    }
    if (showDetails) {
      Text("Ajustar dolor · $pain/5", style = MaterialTheme.typography.titleMedium)
      Slider(pain.toFloat(), { pain = it.roundToInt() }, enabled = !busy, valueRange = 0f..5f, steps = 4,
        modifier = Modifier.testTag("pain_slider"))
      Text("Otras molestias", style = MaterialTheme.typography.titleLarge)
      SymptomSwitch("Cólicos", "", cramps, { cramps = it }, "symptom_cramps_switch", !busy)
      SymptomSwitch("Dolor de cabeza", "", headache, { headache = it }, "symptom_headache_switch", !busy)
      SymptomSwitch("Dolor lumbar", "", backPain, { backPain = it }, "symptom_back_pain_switch", !busy)
      SymptomSwitch("Hinchazón", "", bloating, { bloating = it }, "symptom_bloating_switch", !busy)
      SymptomSwitch("Senos sensibles", "", breasts, { breasts = it }, "symptom_breasts_switch", !busy)
      SymptomSwitch("Antojos", "", cravings, { cravings = it }, "symptom_cravings_switch", !busy)
      SymptomSwitch("Acné", "", acne, { acne = it }, "symptom_acne_switch", !busy)
      Text("Moco cervical", style = MaterialTheme.typography.titleLarge)
      FlowRow(Modifier.selectableGroup(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        CervicalMucus.entries.forEach { value ->
          LunaOptionChip(when (value) {
            CervicalMucus.NONE -> "Sin indicar"; CervicalMucus.DRY -> "Seco"; CervicalMucus.STICKY -> "Pegajoso"
            CervicalMucus.CREAMY -> "Cremoso"; CervicalMucus.EGG_WHITE -> "Clara de huevo"
          }, mucus == value, { mucus = value }, Modifier.testTag("mucus_chip_${value.name}"), enabled = !busy)
        }
      }
      LunaFormField("Nota para compartir con los síntomas", publicNote, { publicNote = it },
        Modifier.testTag("symptom_public_note_input"), enabled = !busy)
    }
  }
}

private fun moodLabel(mood: Mood): String = when (mood) {
  Mood.HAPPY -> "Radiante"; Mood.CALM -> "En calma"; Mood.SENSITIVE -> "Sensible"
  Mood.ENERGETIC -> "Con energía"; Mood.CRAVINGS -> "Con antojo"; Mood.IRRITABLE -> "Irritable"
  Mood.ANXIOUS -> "Ansiosa"; Mood.SAD -> "Con bajón"
}

@Composable
private fun SymptomSwitch(title: String, subtitle: String, checked: Boolean, onChange: (Boolean) -> Unit, tag: String, enabled: Boolean) {
  Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
      Text(title, style = MaterialTheme.typography.titleMedium)
      if (subtitle.isNotBlank()) Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    LunaToggle(checked, onChange, Modifier.testTag(tag), enabled, title)
  }
}
