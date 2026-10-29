package com.dreamsacademy.bkashverify

data class Trx(val id: String, val amount: Double, val sender: String, val time: String)

object Parser {
    private val trxRe = Regex("""(?:TrxID|Trx\s*ID|TxnID|Txn\s*ID|Transaction\s*ID|ট্রানজেকশন\s*আইডি)\s*[:\-]?\s*([A-Za-z0-9]{8,12})""", RegexOption.IGNORE_CASE)
    private val amtPre = Regex("""(?:Tk\.?|BDT|৳)\s*([0-9][0-9,]*(?:\.[0-9]+)?)""", RegexOption.IGNORE_CASE)
    private val amtPost = Regex("""([0-9][0-9,]*(?:\.[0-9]+)?)\s*(?:Tk|BDT|টাকা|৳)""", RegexOption.IGNORE_CASE)
    private val phoneRe = Regex("""(?:\+?88)?(01[0-9]{9})""")
    private val timeRe = Regex("""([0-9]{2}/[0-9]{2}/[0-9]{4}\s*[0-9]{2}:[0-9]{2})""")
    private val receivedWords = listOf("received", "cash in", "cashin", "পেয়েছেন", "জমা হয়েছে")

    private fun normalize(s: String): String {
        val digits = "০১২৩৪৫৬৭৮৯"
        val sb = StringBuilder()
        for (ch in s) {
            val i = digits.indexOf(ch)
            sb.append(if (i >= 0) ('0' + i) else ch)
        }
        return sb.toString()
    }

    /** Returns the parsed transaction (or null) plus a human readable reason. */
    fun explain(raw: String): Pair<Trx?, String> {
        val body = normalize(raw)
        val low = body.lowercase()
        if (receivedWords.none { low.contains(it) }) return Pair(null, "not a money-received message")
        val id = trxRe.find(body)?.groupValues?.get(1)?.uppercase() ?: return Pair(null, "TrxID not found")

        val cands = ArrayList<Pair<Int, String>>()
        amtPre.findAll(body).forEach { cands.add(Pair(it.range.first, it.groupValues[1])) }
        amtPost.findAll(body).forEach { cands.add(Pair(it.range.first, it.groupValues[1])) }
        cands.sortBy { it.first }
        var amount: Double? = null
        for ((pos, num) in cands) {
            val ctx = low.substring(maxOf(0, pos - 12), pos)
            if (ctx.contains("fee") || ctx.contains("balance") || ctx.contains("ফি") || ctx.contains("ব্যালেন্স")) continue
            amount = num.replace(",", "").toDoubleOrNull()
            if (amount != null) break
        }
        if (amount == null) return Pair(null, "amount not found")

        val rawFrom = phoneRe.find(body)?.groupValues?.get(1) ?: ""
        val masked = if (rawFrom.length == 11) rawFrom.substring(0, 3) + "****" + rawFrom.substring(8) else ""
        val time = timeRe.find(body)?.groupValues?.get(1) ?: ""
        return Pair(Trx(id, amount, masked, time), "OK")
    }

    fun parse(body: String): Trx? = explain(body).first
}
