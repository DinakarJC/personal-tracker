package local.tracker.core

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import java.math.BigDecimal
import java.time.LocalDate
import java.time.format.DateTimeFormatterBuilder
import java.time.format.ResolverStyle
import java.time.temporal.ChronoField
import java.util.Locale
import java.util.regex.Pattern

enum class Direction { DEBIT, CREDIT }

enum class Classification { EXPENSE, INCOME, TRANSFER, REFUND, REVERSAL, FEE, UNCLASSIFIED }

enum class FieldOrigin { PARSED, USER, UNKNOWN, SYSTEM }

data class ParsedTransaction(
    val amountMinor: Long,
    val currency: String,
    val direction: Direction,
    val classification: Classification,
    val bank: String,
    val accountSuffix: String?,
    val counterpartyName: String?,
    val counterpartyUpiId: String?,
    val reference: String?,
    val date: LocalDate,
    val timeApproximate: Boolean,
    val paymentMethod: String,
    val messageType: String,
    val templateId: String,
    val templateVersion: Int,
    val parserVersion: Int,
    val provenance: Map<String, FieldOrigin>,
)

sealed interface ParseOutcome {
    data object IgnoredSender : ParseOutcome
    data object Discarded : ParseOutcome
    data object AdvanceNotice : ParseOutcome
    data class Review(val reason: String) : ParseOutcome
    data class Accepted(val transaction: ParsedTransaction) : ParseOutcome
}

@Serializable
data class TemplateExample(
    val sms: String,
    val expected: Map<String, JsonElement>,
)

@Serializable
data class SmsTemplate(
    @SerialName("schema_version") val schemaVersion: Int,
    @SerialName("template_id") val templateId: String,
    val version: Int,
    val bank: String,
    @SerialName("sender_ids") val senderIds: List<String>,
    @SerialName("message_type") val messageType: String,
    val direction: String? = null,
    @SerialName("payment_method") val paymentMethod: String? = null,
    val pattern: String,
    val flags: List<String>,
    @SerialName("date_format") val dateFormat: String,
    val examples: List<TemplateExample>,
    @SerialName("negative_examples") val negativeExamples: List<String>,
)

/** Built-in templates only. Custom untrusted regex import is deliberately not exposed. */
class SmsParser private constructor(
    private val templates: List<Pair<SmsTemplate, Pattern>>,
    reviewedSenders: Set<String>,
) {
    private val allowedSenders = reviewedSenders.map(::normalizeSender).toSet()

    fun exampleMessage(templateId: String): String? = templates.firstOrNull { it.first.templateId == templateId }?.first?.examples?.firstOrNull()?.sms

    fun parse(sender: String, body: String): ParseOutcome {
        val normalizedSender = normalizeSender(sender)
        if (normalizedSender !in allowedSenders) return ParseOutcome.IgnoredSender
        // Bound work on the intake boundary. No message text is returned on discard.
        if (NON_FINANCIAL.containsMatchIn(body)) return ParseOutcome.Discarded
        if (ADVANCE_NOTICE.containsMatchIn(body)) return ParseOutcome.AdvanceNotice
        if (!FINANCIAL.containsMatchIn(body) || !CURRENCY_AMOUNT.containsMatchIn(body)) {
            return ParseOutcome.Discarded
        }
        if (body.length > MAX_MESSAGE_LENGTH) return ParseOutcome.Review("Financial message is too long to parse safely")

        val matches = templates.mapNotNull { (template, pattern) ->
            if (normalizedSender !in template.senderIds.map(::normalizeSender)) return@mapNotNull null
            val matcher = pattern.matcher(body)
            if (matcher.find()) template to matcher else null
        }
        if (matches.isEmpty()) return ParseOutcome.Review("The bank message format is not supported yet")
        if (matches.size > 1) return ParseOutcome.Review("More than one bank format matched")
        val (template, match) = matches.single()
        if (template.messageType == "non_transaction_notice") return ParseOutcome.AdvanceNotice

        fun field(name: String): String? = try {
            match.group(name)?.trim()?.takeIf { it.isNotEmpty() }
        } catch (_: IllegalArgumentException) {
            null
        }

        val amount = parseMinorUnits(field("amount"))
            ?: return ParseOutcome.Review("The amount could not be read confidently")
        val dateText = field("date")
            ?: return ParseOutcome.Review("The message did not include a transaction date")
        val date = parseDate(dateText, template.dateFormat)
            ?: return ParseOutcome.Review("The transaction date is invalid or ambiguous")
        val account = field("account")
        if (account != null && !account.matches(Regex("[0-9]{4}"))) {
            return ParseOutcome.Review("The account suffix is invalid")
        }
        val direction = when (template.direction) {
            "debit" -> Direction.DEBIT
            "credit" -> Direction.CREDIT
            else -> return ParseOutcome.Review("The direction could not be read confidently")
        }
        val classification = when (template.messageType) {
            "reversal_or_refund" -> Classification.UNCLASSIFIED
            else -> if (direction == Direction.DEBIT) Classification.EXPENSE else Classification.UNCLASSIFIED
        }
        val payee = field("payee")
        val upiId = field("upiId")
        val reference = field("reference")
        fun origin(value: String?) = if (value == null) FieldOrigin.UNKNOWN else FieldOrigin.PARSED
        return ParseOutcome.Accepted(
            ParsedTransaction(
                amountMinor = amount,
                currency = "INR",
                direction = direction,
                classification = classification,
                bank = template.bank,
                accountSuffix = account,
                counterpartyName = payee,
                counterpartyUpiId = upiId,
                reference = reference,
                date = date,
                timeApproximate = true,
                paymentMethod = requireNotNull(template.paymentMethod),
                messageType = template.messageType,
                templateId = template.templateId,
                templateVersion = template.version,
                parserVersion = PARSER_VERSION,
                provenance = mapOf(
                    "amount" to FieldOrigin.PARSED,
                    "currency" to FieldOrigin.PARSED,
                    "direction" to FieldOrigin.PARSED,
                    "classification" to FieldOrigin.SYSTEM,
                    "bank" to FieldOrigin.PARSED,
                    "accountSuffix" to origin(account),
                    "counterpartyName" to origin(payee),
                    "counterpartyUpiId" to origin(upiId),
                    "reference" to origin(reference),
                    "date" to FieldOrigin.PARSED,
                    "time" to FieldOrigin.UNKNOWN,
                    "timeApproximate" to FieldOrigin.SYSTEM,
                    "paymentMethod" to FieldOrigin.PARSED,
                ),
            ),
        )
    }

    companion object {
        const val PARSER_VERSION = 1
        private const val MAX_MESSAGE_LENGTH = 8192
        private val NON_FINANCIAL = Regex("\\b(?:OTP|one[- ]time password|verification code|offer|promotion|promotional)\\b", RegexOption.IGNORE_CASE)
        private val ADVANCE_NOTICE = Regex("\\b(?:will be deducted|will be debited|scheduled deduction)\\b", RegexOption.IGNORE_CASE)
        private val FINANCIAL = Regex("\\b(?:sent|debited|credited|deducted|paid|received|refund|refunded|reversed|reversal)\\b", RegexOption.IGNORE_CASE)
        private val CURRENCY_AMOUNT = Regex("(?:Rs\\.?|INR|₹)\\s*[0-9]", RegexOption.IGNORE_CASE)
        private val json = Json { ignoreUnknownKeys = false }

        fun fromJson(text: String, reviewedSenders: Set<String>): SmsParser {
            val templates = json.decodeFromString<List<SmsTemplate>>(text)
            require(templates.isNotEmpty()) { "At least one built-in template is required" }
            require(templates.map { it.templateId }.distinct().size == templates.size) { "Duplicate template ID" }
            val compiled = templates.map { template ->
                require(template.schemaVersion == 1 && template.version > 0)
                require(template.templateId.isNotBlank() && template.bank.isNotBlank())
                require(template.senderIds.isNotEmpty() && template.senderIds.all { it.isNotBlank() })
                require(template.examples.isNotEmpty() && template.negativeExamples.isNotEmpty())
                require(template.pattern.length <= 4096)
                require(template.flags.all { it == "case_insensitive" })
                require(template.dateFormat in setOf("dd/MM/yy", "dd-MM-yy"))
                require(template.messageType in setOf("debit", "credit", "mandate_debit", "reversal_or_refund", "non_transaction_notice"))
                if (template.messageType != "non_transaction_notice") {
                    require(template.direction in setOf("debit", "credit"))
                    require(!template.paymentMethod.isNullOrBlank())
                    require("(?<amount>" in template.pattern && "(?<date>" in template.pattern)
                }
                val flags = if ("case_insensitive" in template.flags) Pattern.CASE_INSENSITIVE else 0
                val pattern = Pattern.compile(template.pattern, flags)
                require(template.examples.all { pattern.matcher(it.sms).find() }) { "Positive example did not match" }
                require(template.negativeExamples.none { pattern.matcher(it).find() }) { "Negative example matched" }
                template to pattern
            }
            return SmsParser(compiled, reviewedSenders)
        }

        fun normalizeSender(sender: String): String {
            val normalized = sender.trim().uppercase(Locale.ROOT)
            return normalized.replaceFirst(Regex("^[A-Z]{2}-"), "")
        }

        fun parseMinorUnits(value: String?): Long? {
            if (value == null) return null
            val valid = Regex("(?:[0-9]+|[0-9]{1,3}(?:,[0-9]{3})+|[0-9]{1,2}(?:,[0-9]{2})*,[0-9]{3})(?:\\.[0-9]{1,2})?")
            if (!valid.matches(value)) return null
            return try {
                BigDecimal(value.replace(",", "")).movePointRight(2).longValueExact().takeIf { it > 0 }
            } catch (_: ArithmeticException) {
                null
            } catch (_: NumberFormatException) {
                null
            }
        }

        private fun parseDate(value: String, format: String): LocalDate? {
            val delimiter = if (format == "dd/MM/yy") '/' else '-'
            val formatter = DateTimeFormatterBuilder()
                .appendValue(ChronoField.DAY_OF_MONTH, 2).appendLiteral(delimiter)
                .appendValue(ChronoField.MONTH_OF_YEAR, 2).appendLiteral(delimiter)
                .appendValueReduced(ChronoField.YEAR, 2, 2, 2000)
                .toFormatter(Locale.ROOT).withResolverStyle(ResolverStyle.STRICT)
            return try {
                LocalDate.parse(value, formatter)
            } catch (_: java.time.DateTimeException) {
                null
            }
        }
    }
}
