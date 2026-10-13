package local.tracker.core

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class SmsParserTest {
    private val templateText = requireNotNull(javaClass.getResource("/templates/hdfc-v1.json")).readText()
    private val templates = Json.decodeFromString<List<SmsTemplate>>(templateText)
    private val parser = SmsParser.fromJson(templateText, setOf("HDFCBN-S"))
    private val debit = templates.first { it.templateId == "hdfc-upi-debit" }.examples.single().sms
    private val credit = templates.first { it.templateId == "hdfc-upi-credit" }.examples.single().sms

    @Test
    fun `test screen examples come from validated built-in templates`() {
        assertEquals(debit, parser.exampleMessage("hdfc-upi-debit"))
        assertNull(parser.exampleMessage("unknown-template"))
    }

    @Test
    fun `Appendix A debit has exactly the reported fields`() {
        val transaction = accepted(debit)
        assertEquals(45_000L, transaction.amountMinor)
        assertEquals("INR", transaction.currency)
        assertEquals(Direction.DEBIT, transaction.direction)
        assertEquals(Classification.EXPENSE, transaction.classification)
        assertEquals("debit", transaction.messageType)
        assertEquals("1234", transaction.accountSuffix)
        assertEquals("JOHN D", transaction.counterpartyName)
        assertNull(transaction.counterpartyUpiId)
        assertEquals("100000000001", transaction.reference)
        assertEquals(LocalDate.of(2026, 9, 15), transaction.date)
        assertEquals("UPI", transaction.paymentMethod)
        assertTrue(transaction.timeApproximate)
    }

    @Test
    fun `Appendix A mandate represents a completed debit`() {
        val body = templates.first { it.messageType == "mandate_debit" }.examples.single().sms
        val transaction = accepted(body)
        assertEquals(30_000L, transaction.amountMinor)
        assertEquals(Direction.DEBIT, transaction.direction)
        assertEquals(Classification.EXPENSE, transaction.classification)
        assertEquals("mandate_debit", transaction.messageType)
        assertEquals("mandate", transaction.paymentMethod)
        assertEquals("SAMPLE AUTOPAY", transaction.counterpartyName)
        assertEquals("1234", transaction.accountSuffix)
        assertEquals("100000000002", transaction.reference)
        assertEquals(LocalDate.of(2026, 9, 5), transaction.date)
        assertNull(transaction.counterpartyUpiId)
        assertTrue(transaction.timeApproximate)
    }

    @Test
    fun `Appendix A credit does not invent income or a payee`() {
        val transaction = accepted(credit)
        assertEquals(1250L, transaction.amountMinor)
        assertEquals(Direction.CREDIT, transaction.direction)
        assertEquals(Classification.UNCLASSIFIED, transaction.classification)
        assertNull(transaction.counterpartyName)
        assertEquals("merchant1@examplebank", transaction.counterpartyUpiId)
        assertEquals("1234", transaction.accountSuffix)
        assertEquals("100000000003", transaction.reference)
        assertEquals(LocalDate.of(2026, 9, 12), transaction.date)
        assertEquals("UPI", transaction.paymentMethod)
        assertTrue(transaction.timeApproximate)
    }

    @Test
    fun `Appendix A notice produces no transaction or retained body`() {
        val body = templates.first { it.messageType == "non_transaction_notice" }.examples.single().sms
        assertEquals(ParseOutcome.AdvanceNotice, parser.parse("AD-HDFCBN-S", body))
    }

    @Test
    fun `all data-file examples agree with expected extraction`() {
        templates.filter { it.messageType != "non_transaction_notice" }.forEach { template ->
            template.examples.forEach { example ->
                val transaction = accepted(example.sms)
                val fields = mapOf(
                    "amount" to transaction.amountMinor.toBigDecimal().movePointLeft(2).stripTrailingZeros().toPlainString(),
                    "account" to transaction.accountSuffix,
                    "payee" to transaction.counterpartyName,
                    "upiId" to transaction.counterpartyUpiId,
                    "date" to transaction.date.toString(),
                    "reference" to transaction.reference,
                )
                example.expected.forEach { (name, expected) ->
                    assertEquals("${template.templateId}: $name", expected.jsonPrimitive.content, fields[name])
                }
            }
        }
    }

    @Test
    fun `sender matching removes only a reviewed carrier prefix`() {
        assertTrue(parser.parse("AD-HDFCBN-S", debit) is ParseOutcome.Accepted)
        assertTrue(parser.parse("VM-HDFCBN-S", debit) is ParseOutcome.Accepted)
        assertTrue(parser.parse(" hdfcbn-s ", debit) is ParseOutcome.Accepted)
        listOf("HDFCBN", "HDFCBN-P", "OTHER-HDFCBN-S", "+911234567890", "AD-OTHER-S").forEach {
            assertEquals(ParseOutcome.IgnoredSender, parser.parse(it, debit))
        }
    }

    @Test
    fun `a template cannot grant permission to retain a sender`() {
        val disabled = SmsParser.fromJson(templateText, emptySet())
        assertEquals(ParseOutcome.IgnoredSender, disabled.parse("AD-HDFCBN-S", debit))
    }

    @Test
    fun `OTP promotion unrelated and ambiguous messages contain no retained text`() {
        listOf(
            "123456 is your one-time password. Do not share it with anyone.",
            "OTP 123456 for a payment of Rs.450.00. Do not share it.",
            "Get an offer of Rs.450.00 credited as rewards when you apply.",
            "Your monthly statement is ready.",
            "Your account has been updated.",
            "hello ".repeat(2000),
        ).forEach { assertEquals(ParseOutcome.Discarded, parser.parse("AD-HDFCBN-S", it)) }
    }

    @Test
    fun `unsupported plausible financial formats require review`() {
        assertTrue(parser.parse("AD-HDFCBN-S", "Your account was debited INR 450 at STORE.") is ParseOutcome.Review)
    }

    @Test
    fun `invalid calendar dates are not normalized silently`() {
        listOf("31/02/26", "29/02/26", "15/13/26", "00/09/26").forEach {
            assertTrue(parser.parse("AD-HDFCBN-S", debit.replace("15/09/26", it)) is ParseOutcome.Review)
        }
        assertEquals(LocalDate.of(2024, 2, 29), accepted(debit.replace("15/09/26", "29/02/24")).date)
    }

    @Test
    fun `currency validation handles grouping precision and overflow`() {
        mapOf("450.00" to 45_000L, "12.50" to 1250L, "1,234.56" to 123456L, "1,23,456.78" to 12345678L).forEach { (amount, expected) ->
            assertEquals(expected, SmsParser.parseMinorUnits(amount))
        }
        listOf("0", "-1", "1.234", "12,34", "NaN", "9223372036854775808", "1e3", "").forEach {
            assertNull(SmsParser.parseMinorUnits(it))
        }
        assertNull(SmsParser.parseMinorUnits(null))
    }

    @Test
    fun `missing reference and unsupported precision do not produce a fabricated accepted record`() {
        assertTrue(parser.parse("AD-HDFCBN-S", debit.replace("Ref 100000000001", "")) is ParseOutcome.Review)
        assertTrue(parser.parse("AD-HDFCBN-S", debit.replace("450.00", "450.001")) is ParseOutcome.Review)
    }

    @Test
    fun `provenance distinguishes parsed missing and system default values`() {
        val transaction = accepted(credit)
        assertEquals(FieldOrigin.PARSED, transaction.provenance["amount"])
        assertEquals(FieldOrigin.UNKNOWN, transaction.provenance["counterpartyName"])
        assertEquals(FieldOrigin.SYSTEM, transaction.provenance["classification"])
        assertEquals(FieldOrigin.UNKNOWN, transaction.provenance["time"])
        assertEquals(FieldOrigin.SYSTEM, transaction.provenance["timeApproximate"])
        assertEquals("hdfc-upi-credit", transaction.templateId)
        assertEquals(1, transaction.templateVersion)
        assertEquals(1, transaction.parserVersion)
    }

    @Test
    fun `distinct references remain distinct parsed events`() {
        val first = accepted(debit)
        val second = accepted(debit.replace("100000000001", "100000000099"))
        assertEquals(first.amountMinor, second.amountMinor)
        assertTrue(first.reference != second.reference)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `invalid template versions are rejected before use`() {
        SmsParser.fromJson(templateText.replace("\"schema_version\": 1", "\"schema_version\": 99"), setOf("HDFCBN-S"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `overbroad template matching a negative fixture is rejected`() {
        val invalid = templates.map { it.copy(pattern = ".*(?<amount>)(?<date>)") }
        SmsParser.fromJson(Json.encodeToString(invalid), setOf("HDFCBN-S"))
    }

    private fun accepted(body: String): ParsedTransaction = (parser.parse("AD-HDFCBN-S", body) as ParseOutcome.Accepted).transaction
}
