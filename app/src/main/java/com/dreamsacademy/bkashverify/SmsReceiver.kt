package com.dreamsacademy.bkashverify

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        if (i.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!Store.enabled(c)) return
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(i) ?: return

        val bySender = LinkedHashMap<String, StringBuilder>()
        for (m in msgs) {
            bySender.getOrPut(m.originatingAddress ?: "") { StringBuilder() }.append(m.messageBody ?: "")
        }
        val allowed = Store.get(c, "sender", "bKash").trim()
        var queued = false
        for ((sender, body) in bySender) {
            if (!sender.trim().equals(allowed, ignoreCase = true)) continue
            val t = Parser.parse(body.toString())
            if (t == null) {
                Store.log(c, "Ignored a $allowed SMS (not a received-money message)")
                continue
            }
            Store.addPending(
                c,
                JSONObject().put("trxId", t.id).put("amount", t.amount).put("sender", t.sender).put("smsTime", t.time)
            )
            Store.log(c, "Received ${t.id}  Tk ${t.amount}  - uploading")
            queued = true
        }
        if (queued) enqueue(c)
    }

    companion object {
        fun enqueue(c: Context) {
            val req = OneTimeWorkRequestBuilder<SendWorker>()
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(c).enqueueUniqueWork("send", ExistingWorkPolicy.APPEND_OR_REPLACE, req)
        }
    }
}
