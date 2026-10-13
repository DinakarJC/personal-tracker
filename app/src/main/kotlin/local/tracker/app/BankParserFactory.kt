package local.tracker.app

import local.tracker.core.SmsParser

internal fun readBuiltInTemplates(): String = requireNotNull(SmsParser::class.java.getResource("/templates/hdfc-v1.json")).readText()

internal fun createParser(): SmsParser = SmsParser.fromJson(readBuiltInTemplates(), setOf("HDFCBN-S"))
