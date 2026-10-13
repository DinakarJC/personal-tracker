package local.tracker.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : ComponentActivity() {
    private var permissionStatus by mutableStateOf("SMS access is paused")
    private var captureStatus by mutableStateOf("No incoming HDFC message observed yet")
    private var previewStatus by mutableStateOf("")
    private var previewRunning by mutableStateOf(false)
    private val requestSms = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        refreshStatus()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        refreshStatus()
        setContent {
            MaterialTheme(colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                    ) {
                        Text("Personal Expense Tracker", style = MaterialTheme.typography.headlineMedium)
                        Text("Capture setup check", style = MaterialTheme.typography.titleLarge)
                        Text("This first test build checks SMS access on your phone. It does not save a transaction ledger yet.")
                        Text("Only HDFC sender HDFCBN-S is processed. Messages stay on this phone. OTPs, offers and advance notices are discarded.")
                        Text("SMS alerts can be missing or incomplete. The finished tracker will also support manual entry.")
                        Text(permissionStatus)
                        Button(onClick = {
                            requestSms.launch(arrayOf(Manifest.permission.READ_SMS, Manifest.permission.RECEIVE_SMS))
                        }) { Text("Allow SMS access") }
                        Button(onClick = {
                            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, "package:$packageName".toUri()))
                        }) { Text("Open app permissions") }
                        Text("If access remains blocked, check the installer and your phone's restricted settings. Permission is not guaranteed by sideloading.")
                        Text(captureStatus)
                        Button(onClick = { refreshStatus() }) { Text("Refresh capture status") }
                        Text("Preview reads eligible messages from the last seven days. It stores neither message text nor transactions and makes no changes to your inbox.")
                        Button(onClick = { runPreview() }, enabled = !previewRunning) {
                            Text(if (previewRunning) "Reading eligible messages…" else "Preview last seven days")
                        }
                        if (previewStatus.isNotEmpty()) Text(previewStatus)
                        Text("You can revoke SMS access in app permissions. Uninstalling this test build removes its capture-status metadata.")
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshStatus()
    }

    private fun refreshStatus() {
        val read = checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val receive = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        permissionStatus = "Inbox access: ${if (read) "allowed" else "paused"}. Incoming capture: ${if (receive) "allowed" else "paused"}."
        val preferences = getSharedPreferences("capture_check", MODE_PRIVATE)
        val received = preferences.getLong("last_received", 0)
        captureStatus = if (received == 0L) {
            "No incoming HDFC message observed yet"
        } else {
            "${preferences.getString("last_status", "Message observed")} · ${DateFormat.getDateTimeInstance().format(Date(received))}"
        }
    }

    private fun runPreview() {
        if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
            previewStatus = "Allow inbox access first. If Android blocks access, open app permissions."
            return
        }
        previewRunning = true
        lifecycleScope.launch {
            previewStatus = try {
                val counts = withContext(Dispatchers.IO) { previewRecentInbox(this@MainActivity) }
                "Preview only: ${counts.recognized} recognized messages; ${counts.review} need review. These are message counts, not unique transaction totals."
            } catch (_: SecurityException) {
                "Android blocked inbox access. Check app permissions and installer restrictions."
            } catch (_: android.database.SQLException) {
                "Your phone could not read the SMS inbox. Try again after reopening the app."
            } catch (_: IllegalArgumentException) {
                "Your phone's SMS provider could not read the preview."
            } catch (_: IllegalStateException) {
                "The SMS inbox is not available. Try again after reopening the app."
            } finally {
                previewRunning = false
            }
            refreshStatus()
        }
    }
}
