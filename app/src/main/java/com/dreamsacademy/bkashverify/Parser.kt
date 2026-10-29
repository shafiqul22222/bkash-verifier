package com.dreamsacademy.bkashverify

data class Trx(val id: String, val amount: Double, val sender: String, val time: String)

object Parser {
    private val trxRe = Regex("TrxID\\s*[:\\-]?\\s*([A-Za-z0-9]{8,12})", RegexOption.IGNORE_CASE)
    private val amtRe = Regex("Tk\\s*([0-9,]+(?:\\.[0-9]{1,2})?)", RegexOption.IGNORE_CASE)
    private val fromRe = Regex("from\\s*(01[0-9]{9})", RegexOption.IGNORE_CASE)
    private val timeRe = Regex("at\\s*([0-9]{2}/[0-9]{2}/[0-9]{4}\\s*[0-9]{2}:[0-9]{2})", RegexOption.IGNORE_CASE)

    /** Returns a transaction only for "money received" bKash SMS. Sent / cash-out / bill SMS are ignored. */
    fun parse(body: String): Trx? {
        val low = body.lowercase()
        val idx = low.indexOf("received")
        if (idx < 0) return null
        val id = trxRe.find(body)?.groupValues?.get(1)?.uppercase() ?: return null
        val amount = amtRe.find(body, idx)?.groupValues?.get(1)?.replace(",", "")?.toDoubleOrNull() ?: return null
        val rawFrom = fromRe.find(body)?.groupValues?.get(1) ?: ""
        val masked = if (rawFrom.length == 11) rawFrom.substring(0, 3) + "****" + rawFrom.substring(8) else ""
        val time = timeRe.find(body)?.groupValues?.get(1) ?: ""
        return Trx(id, amount, masked, time)
    }
}
