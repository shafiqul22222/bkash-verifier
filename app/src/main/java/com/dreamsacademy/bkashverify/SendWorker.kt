package com.dreamsacademy.bkashverify

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters

class SendWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val c = applicationContext
        val list = Store.pending(c)
        if (list.length() == 0) return Result.success()

        val (token, err) = Api.signIn(c)
        if (token == null) {
            Store.log(c, "Login failed: $err (will retry)")
            return Result.retry()
        }
        var failed = false
        for (i in 0 until list.length()) {
            val o = list.getJSONObject(i)
            val id = o.getString("trxId")
            val r = Api.send(c, o, token)
            if (r.ok) {
                Store.removePending(c, id)
                Store.log(c, (if (r.dup) "Already saved: " else "Saved: ") + id + "  Tk " + o.getDouble("amount"))
            } else {
                failed = true
                Store.log(c, "Failed $id: ${r.msg} (will retry)")
            }
        }
        return if (failed) Result.retry() else Result.success()
    }
}
