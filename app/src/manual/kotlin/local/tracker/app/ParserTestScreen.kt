package local.tracker.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import local.tracker.core.ParseOutcome
import local.tracker.core.ParsedTransaction
import local.tracker.core.SmsParser
import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun ParserTestScreen(parser: SmsParser) {
    var sender by remember { mutableStateOf("HDFCBN-S") }
    var body by remember { mutableStateOf("") }
    var inputError by remember { mutableStateOf<String?>(null) }
    var outcome by remember { mutableStateOf<ParseOutcome?>(null) }
    val example = remember {
        requireNotNull(parser.exampleMessage("hdfc-upi-debit"))
    }
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text("Personal Tracker Test", style = MaterialTheme.typography.headlineMedium)
            Text("Test a bank message", style = MaterialTheme.typography.titleLarge)
            Text("Paste a redacted HDFC message or choose the sample. This build cannot read your SMS inbox and requests no SMS access.")
            Text("Text is processed on this phone and kept only in this screen's memory. It is not saved or uploaded. This is a parser test, not a transaction ledger.")
            OutlinedTextField(
                value = sender,
                onValueChange = {
                    sender = it
                    outcome = null
                },
                label = { Text("Sender label (entered manually)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = body,
                onValueChange = {
                    outcome = null
                    if (it.length <= 8192) {
                        body = it
                        inputError = null
                    } else {
                        inputError = "Use a message of 8,192 characters or fewer. The oversized paste was not applied."
                    }
                },
                label = { Text("Bank message") },
                minLines = 5,
                maxLines = 12,
                isError = inputError != null,
                modifier = Modifier.fillMaxWidth(),
            )
            inputError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            Button(onClick = {
                sender = "HDFCBN-S"
                body = example
                inputError = null
                outcome = null
            }) { Text("Use sample debit") }
            Button(
                onClick = { outcome = parser.parse(sender, body) },
                enabled = body.isNotBlank() && sender.isNotBlank() && inputError == null,
            ) { Text("Parse message") }
            Button(onClick = {
                body = ""
                inputError = null
                outcome = null
            }) { Text("Clear message") }
            when (val result = outcome) {
                is ParseOutcome.Accepted -> ParsedDetails(result.transaction)
                is ParseOutcome.Review -> Text("Needs review: ${result.reason}")
                ParseOutcome.AdvanceNotice -> Text("Advance notice: no transaction is created.")
                ParseOutcome.Discarded -> Text("No supported transaction found. Nothing is saved.")
                ParseOutcome.IgnoredSender -> Text("Sender is not on the reviewed HDFC allowlist. Nothing is saved.")
                null -> Unit
            }
        }
    }
}

@Composable
private fun ParsedDetails(transaction: ParsedTransaction) {
    val amount = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
        .format(BigDecimal.valueOf(transaction.amountMinor, 2))
    Text("Parsed from pasted text (unverified)", style = MaterialTheme.typography.titleMedium)
    Text("Amount: $amount")
    Text("Direction: ${transaction.direction.name.lowercase(Locale.ROOT)}")
    Text("Classification: ${transaction.classification.name.lowercase(Locale.ROOT)}")
    Text("Date: ${transaction.date} · transaction time not included")
    Text("Account suffix: ${transaction.accountSuffix ?: "Not included"}")
    Text("Payee: ${transaction.counterpartyName ?: "Not included"}")
    Text("UPI ID: ${transaction.counterpartyUpiId ?: "Not included"}")
    Text("Reference: ${transaction.reference ?: "Not included"}")
    Text("Payment method: ${transaction.paymentMethod}")
    Text("These are extracted fields only. No payment or ledger record is created.")
}
