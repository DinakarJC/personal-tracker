package local.tracker.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.core.content.edit
import local.tracker.core.ParseOutcome
import local.tracker.core.SmsParser

internal fun createParser(): SmsParser {
    val templateText = requireNotNull(SmsParser::class.java.getResource("/templates/hdfc-v1.json")).readText()
    return SmsParser.fromJson(templateText, setOf("HDFCBN-S"))
}

/** Device feasibility probe. Keeps status metadata only, never SMS bodies or financial fields. */
class IncomingSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val sender = messages.first().originatingAddress ?: return
        if (messages.any { it.originatingAddress != sender }) return
        val result = createParser().parse(sender, messages.joinToString("") { it.messageBody.orEmpty() })
        if (result == ParseOutcome.IgnoredSender || result == ParseOutcome.Discarded) return
        val status = when (result) {
            is ParseOutcome.Accepted -> "Recognized ${result.transaction.messageType.replace('_', ' ')}"
            is ParseOutcome.Review -> "Financial message needs review"
            ParseOutcome.AdvanceNotice -> return
            else -> return
        }
        context.getSharedPreferences("capture_check", Context.MODE_PRIVATE).edit {
            putString("last_status", status)
            putLong("last_received", System.currentTimeMillis())
        }
    }
}

internal data class PreviewCounts(val recognized: Int, val review: Int)

internal fun previewRecentInbox(context: Context): PreviewCounts {
    val parser = createParser()
    val since = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
    var recognized = 0
    var review = 0
    context.contentResolver.query(
        Telephony.Sms.Inbox.CONTENT_URI,
        arrayOf(Telephony.Sms.ADDRESS, Telephony.Sms.BODY),
        "${Telephony.Sms.DATE} >= ? AND (${Telephony.Sms.ADDRESS} = ? OR ${Telephony.Sms.ADDRESS} LIKE ?)",
        arrayOf(since.toString(), "HDFCBN-S", "__-HDFCBN-S"),
        "${Telephony.Sms.DATE} ASC",
    )?.use { cursor ->
        val addressIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
        val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
        while (cursor.moveToNext()) {
            val sender = cursor.getString(addressIndex).orEmpty()
            if (SmsParser.normalizeSender(sender) != "HDFCBN-S") continue
            when (parser.parse(sender, cursor.getString(bodyIndex).orEmpty())) {
                is ParseOutcome.Accepted -> recognized++
                is ParseOutcome.Review -> review++
                else -> Unit
            }
        }
    } ?: throw IllegalStateException("SMS inbox is unavailable")
    return PreviewCounts(recognized, review)
}
