package com.example

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.data.model.CheckIn
import com.example.data.model.CheckInPromptType
import com.example.data.model.QuickResponseOption
import com.example.data.model.SpaceStatus
import com.example.data.model.PairingInvite
import com.google.firebase.Timestamp
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.Date
import com.example.data.model.SharingPermissions
import com.example.data.model.UserRole
import com.example.data.preferences.AppAppearanceMode
import com.example.ui.dialogs.IntimacyLogDialog
import com.example.ui.dialogs.PrivacySettingsDialog
import com.example.ui.dialogs.SettingsDialog
import com.example.ui.dialogs.CheckInResponseDialog
import com.example.ui.screens.PairingScreen
import com.example.ui.theme.LunaSyncTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PrivacyAndSettingsComposeTest {
  @get:Rule val compose = createComposeRule()

  @Test fun privacyWaitsForSuccessAndKeepsTheChangedPermissionAfterFailure() {
    var dismissed = false
    var saveCalls = 0
    var captured: SharingPermissions? = null
    var completion: ((Boolean, String?) -> Unit)? = null
    compose.setContent {
      LunaSyncTheme {
        PrivacySettingsDialog(
          currentPermissions = SharingPermissions(shareSymptoms = false),
          onSavePermissions = { permissions, done ->
            saveCalls++
            captured = permissions
            completion = done
          },
          onDismiss = { dismissed = true }
        )
      }
    }
    compose.onNodeWithTag("toggle_share_symptoms").performScrollTo().performClick()
    compose.onNodeWithTag("save_permissions_button").assertIsDisplayed().performClick()
    compose.onNodeWithTag("save_permissions_button").assertIsNotEnabled()
    compose.onNodeWithTag("toggle_share_symptoms").assertIsNotEnabled()
    assertFalse(dismissed)
    assertEquals(1, saveCalls)
    compose.runOnIdle { completion!!(false, "Sin conexión. Reintenta.") }
    compose.onNodeWithText("Sin conexión. Reintenta.").assertIsDisplayed()
    compose.onNodeWithTag("toggle_share_symptoms").assertIsOn()
    assertFalse(dismissed)
    compose.onNodeWithTag("save_permissions_button").assertIsDisplayed().performClick()
    assertEquals(2, saveCalls)
    assertTrue(captured!!.shareSymptoms)
    compose.runOnIdle { completion!!(true, null) }
    assertTrue(dismissed)
  }

  @Test fun partnerCanReadPermissionsButCannotChangeOrSaveThem() {
    var writes = 0
    compose.setContent {
      LunaSyncTheme {
        PrivacySettingsDialog(SharingPermissions(), { _, _ -> writes++ }, {}, canEdit = false)
      }
    }
    compose.onNodeWithTag("toggle_share_symptoms").assertIsOn().assertIsNotEnabled()
    compose.onNodeWithTag("toggle_share_mood").assertIsNotEnabled()
    compose.onNodeWithTag("toggle_share_intimacy").assertIsNotEnabled()
    compose.onNodeWithTag("save_permissions_button").assertDoesNotExist()
    assertEquals(0, writes)
  }

  @Test fun authenticatedSettingsHaveNoRoleSwitchAction() {
    compose.setContent {
      LunaSyncTheme {
        SettingsDialog(AppAppearanceMode.SYSTEM, {}, UserRole.SUPPORT_PARTNER, {}, {}, {},
          "partner@example.test", {}, isGuestMode = false)
      }
    }
    compose.onNodeWithTag("switch_experience_row").assertDoesNotExist()
  }

  @Test fun aNewIntimacyRecordIsPrivateWithoutAnExplicitSharingDecision() {
    var capturedPrivacy: Boolean? = null
    compose.setContent {
      LunaSyncTheme {
        IntimacyLogDialog(onDismiss = {}, onSave = { _, _, _, _, _, privateRecord, _ ->
          capturedPrivacy = privateRecord
        })
      }
    }
    compose.onNodeWithTag("save_intimacy_button").performClick()
    assertEquals(true, capturedPrivacy)
  }

  @Test fun aReplyKeepsItsNoteAfterFailureAndWaitsForConfirmedSuccess() {
    var dismissed = false
    var completion: ((Boolean, String?) -> Unit)? = null
    var capturedNote: String? = null
    compose.setContent {
      LunaSyncTheme {
        CheckInResponseDialog(CheckIn(id = "opened-message", promptType = CheckInPromptType.NEED_SPACE_OR_CUDDLES.name),
          onSendResponse = { _, note, done -> capturedNote = note; completion = done },
          onDismiss = { dismissed = true })
      }
    }
    compose.onNodeWithTag("submit_reply_button").assertIsNotEnabled()
    compose.onNodeWithText("¿Prefieres compañía o un rato para ti?").assertIsDisplayed()
    compose.onNodeWithText("Compañía").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Un rato sola").performScrollTo().assertIsDisplayed()
    compose.onNodeWithTag("reply_option_ALL_GOOD_JUST_CRAMPS").assertDoesNotExist()
    compose.onNodeWithTag("reply_option_NEED_SILENCE").performScrollTo().performClick()
    compose.onNodeWithTag("reply_note_input").performScrollTo().performTextInput("Luego hablamos con calma.")
    compose.onNodeWithTag("submit_reply_button").assertIsDisplayed().performClick()
    assertFalse(dismissed)
    compose.onNodeWithTag("submit_reply_button").assertIsNotEnabled()
    compose.runOnIdle { completion!!(false, "No se pudo enviar. Reintenta.") }
    compose.onNodeWithTag("reply_note_input").assertTextContains("Luego hablamos con calma.")
    compose.onNodeWithTag("submit_reply_button").assertIsDisplayed().performClick()
    assertEquals("Luego hablamos con calma.", capturedNote)
    compose.runOnIdle { completion!!(true, null) }
    assertTrue(dismissed)
  }

  @Test fun unlinkConfirmationRemainsOpenUntilItsOperationSucceeds() {
    var calls = 0
    var completion: ((Boolean, String?) -> Unit)? = null
    compose.setContent {
      LunaSyncTheme {
        PairingScreen(UserRole.PRIMARY_ELLA, null, SpaceStatus.PAIRED.name, "partner", false, {}, {},
          onRevokePairing = { done -> calls++; completion = done })
      }
    }
    compose.onNodeWithTag("open_unlink_confirm_cta").performScrollTo().performClick()
    compose.onNodeWithTag("confirm_unlink_button").performClick()
    compose.onNodeWithTag("confirm_unlink_button").assertIsNotEnabled()
    compose.onNodeWithTag("cancel_unlink_button").assertIsNotEnabled()
    assertEquals(1, calls)
    compose.runOnIdle { completion!!(false, "No se pudo finalizar. Reintenta.") }
    compose.onNodeWithText("No se pudo finalizar. Reintenta.").assertIsDisplayed()
    compose.onNodeWithTag("confirm_unlink_button").performClick()
    assertEquals(2, calls)
    compose.runOnIdle { completion!!(true, null) }
    compose.onNodeWithTag("confirm_unlink_button").assertDoesNotExist()
  }

  private fun openInvite(invite: PairingInvite, version: Long, paired: Boolean = false, generate: () -> Unit = {}) {
    val clock = Clock.fixed(Instant.parse("2026-06-03T12:00:00Z"), ZoneOffset.UTC)
    compose.setContent {
      LunaSyncTheme {
        PairingScreen(UserRole.PRIMARY_ELLA, invite,
          if (paired) SpaceStatus.PAIRED.name else SpaceStatus.UNPAIRED.name,
          if (paired) "partner" else null, false, generate, {},
          activeInviteVersion = version, clock = clock)
      }
    }
    compose.onNodeWithTag("open_invite_row").performScrollTo().performClick()
  }

  @Test fun expiredInviteCanBeRegeneratedAndOpeningItsDetailsDoesNotGenerateAnotherCode() {
    var generated = 0
    openInvite(PairingInvite(inviteCode = "LUNA-OLD1", version = 2,
      expiresAt = Timestamp(Date.from(Instant.parse("2026-06-01T12:00:00Z")))), 2, generate = { generated++ })
    assertEquals(0, generated)
    compose.onNodeWithTag("copy_pairing_code_button").assertIsNotEnabled()
    compose.onNodeWithTag("regenerate_pairing_code_button").performScrollTo().assertIsEnabled().performClick()
    assertEquals(1, generated)
  }

  @Test fun aSupersededInviteCannotBeCopiedEvenBeforeItsExpiry() {
    openInvite(PairingInvite(inviteCode = "LUNA-OLD1", version = 2,
      expiresAt = Timestamp(Date.from(Instant.parse("2026-06-05T12:00:00Z")))), 3)
    compose.onNodeWithText("Este código fue sustituido. Genera uno nuevo.").assertIsDisplayed()
    compose.onNodeWithTag("copy_pairing_code_button").assertIsNotEnabled()
  }

  @Test fun anActivePairingShowsItsStatusInsteadOfOfferingAnUnredeemableInvitation() {
    openInvite(PairingInvite(inviteCode = "LUNA-OLD1", version = 2,
      expiresAt = Timestamp(Date.from(Instant.parse("2026-06-05T12:00:00Z")))), 2, paired = true)
    compose.onNodeWithText("Tu vínculo está activo").assertIsDisplayed()
    compose.onNodeWithTag("copy_pairing_code_button").assertIsNotEnabled()
    compose.onNodeWithTag("regenerate_pairing_code_button").assertDoesNotExist()
  }
}
