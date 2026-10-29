package com.dreamsacademy.bkashverify

import android.content.Context
import android.provider.Telephony
import org.json.JSONObject

object InboxScanner {
    fun senderMatches(c: Context, address: String?): Boolean {
        val a = (address ?: "").lowercase().trim()
        if (a.isEmpty()) return false
        val allowed = Store.get(c, "sender", "bKash").split(",").map { it.trim().lowercase() }.filter { it.isNotEmpty() }
        return allowed.any { a.contains(it) }
    }

    /** Reads the SMS inbox and queues any bKash "money received" message not seen before. */
    fun scan(c: Context, sinceMs: Long): Int {
        var queued = 0
        try {
            val cur = c.contentResolver.query(
                Telephony.Sms.Inbox.CONTENT_URI,
                arrayOf("address", "body", "date"),
                "date > ?",
                arrayOf(sinceMs.toString()),
                "date DESC"
            ) ?: return 0
            cur.use { cursor ->
                var n = 0
                while (cursor.moveToNext() && n < 300) {
                    n++
                    val addr = cursor.getString(0)
                    val body = cursor.getString(1) ?: ""
                    if (!senderMatches(c, addr)) continue
                    val t = Parser.parse(body) ?: continue
                    if (!Store.firstTime(c, t.id)) continue
                    Store.addPending(
                        c,
                        JSONObject().put("trxId", t.id).put("amount", t.amount).put("sender", t.sender).put("smsTime", t.time)
                    )
                    Store.log(c, "Found in inbox ${t.id}  Tk ${t.amount}  - uploading")
                    queued++
                }
            }
        } catch (e: SecurityException) {
            Store.log(c, "Inbox scan needs the SMS (read) permission")
        } catch (e: Exception) {
            Store.log(c, "Inbox scan error: ${e.message}")
        }
        return queued
    }
}
