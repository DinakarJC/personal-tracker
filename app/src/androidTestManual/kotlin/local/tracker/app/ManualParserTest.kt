package local.tracker.app

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ManualParserTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun requestsNeitherSmsNorNetworkAccess() {
        val activity = compose.activity
        val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
        val permissions = info.requestedPermissions.orEmpty()
        assertFalse(permissions.contains(Manifest.permission.READ_SMS))
        assertFalse(permissions.contains(Manifest.permission.RECEIVE_SMS))
        assertFalse(permissions.contains(Manifest.permission.INTERNET))
    }

    @Test
    fun startsWithoutFakeTransactions() {
        compose.onNodeWithText("Parsed from pasted text (unverified)").assertDoesNotExist()
    }

    @Test
    fun sampleDebitCanBeParsedAndCleared() {
        compose.onNodeWithText("Use sample debit").performScrollTo().performClick()
        compose.onNodeWithText("Parse message").performScrollTo().performClick()
        compose.onNodeWithText("Payee: JOHN D").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Reference: 100000000001").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Clear message").performScrollTo().performClick()
        compose.onNodeWithText("Parsed from pasted text (unverified)").assertDoesNotExist()
    }
}
