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
        val msgs = Telephony.Sms.Intents.getMessagesFromIntent(i) ?: return

        val bySender = LinkedHashMap<String, StringBuilder>()
        for (m in msgs) {
            bySender.getOrPut(m.originatingAddress ?: "") { StringBuilder() }.append(m.messageBody ?: "")
        }
        var queued = false
        for ((sender, body) in bySender) {
            // proves the phone really delivers SMS to this app
            Store.put(c, "lastSms", Store.stamp() + "  from " + sender)
            if (!Store.enabled(c)) continue
            if (!InboxScanner.senderMatches(c, sender)) continue
            val text = body.toString()
            val (t, why) = Parser.explain(text)
            if (t == null) {
                Store.log(c, "Ignored SMS from $sender: $why | " + text.replace("\n", " ").take(90))
                continue
            }
            if (!Store.firstTime(c, t.id)) continue
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
