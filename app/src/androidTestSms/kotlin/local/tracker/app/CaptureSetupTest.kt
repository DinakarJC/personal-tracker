package local.tracker.app

import android.Manifest
import android.content.pm.PackageManager
import android.view.WindowManager
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CaptureSetupTest {
    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    @Test
    fun explainsThatTheProbeDoesNotSaveALedger() {
        compose.onNodeWithText("Capture setup check").assertIsDisplayed()
        compose.onNodeWithText("It does not save a transaction ledger yet.", substring = true).assertIsDisplayed()
    }

    @Test
    fun protectsRecentAppsAndScreenshots() {
        compose.runOnUiThread {
            assertTrue(compose.activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE != 0)
        }
    }

    @Test
    fun declaresNoNetworkPermission() {
        val activity = compose.activity
        val info = activity.packageManager.getPackageInfo(activity.packageName, PackageManager.GET_PERMISSIONS)
        assertFalse(info.requestedPermissions.orEmpty().contains(Manifest.permission.INTERNET))
    }

    @Test
    fun deniedInboxAccessShowsAnActionableMessage() {
        assumeTrue(compose.activity.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED)
        compose.onNodeWithText("Preview last seven days").performScrollTo().performClick()
        compose.onNodeWithText("Allow inbox access first.", substring = true).performScrollTo().assertIsDisplayed()
    }
}
