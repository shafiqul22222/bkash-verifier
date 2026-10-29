package com.dreamsacademy.bkashverify

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.work.Worker
import androidx.work.WorkerParameters

/** Runs every ~15 minutes as a safety net in case the phone blocked the live SMS broadcast. */
class ScanWorker(ctx: Context, params: WorkerParameters) : Worker(ctx, params) {
    override fun doWork(): Result {
        val c = applicationContext
        if (!Store.enabled(c)) return Result.success()
        if (c.checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) return Result.success()
        val n = InboxScanner.scan(c, System.currentTimeMillis() - 3L * 24 * 3600 * 1000)
        if (n > 0) SmsReceiver.enqueue(c)
        return Result.success()
    }
}
