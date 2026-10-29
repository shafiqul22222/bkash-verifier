package com.dreamsacademy.bkashverify

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var logView: TextView
    private lateinit var testBox: EditText
    private lateinit var testResult: TextView
    private lateinit var enabledSwitch: Switch
    private val fields = LinkedHashMap<String, EditText>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this)
        root.orientation = LinearLayout.VERTICAL
        root.setPadding(40, 48, 40, 48)

        fun label(t: String): TextView {
            val v = TextView(this)
            v.text = t
            v.textSize = 13f
            v.setPadding(0, 24, 0, 4)
            return v
        }
        fun field(key: String, title: String, def: String, secret: Boolean = false) {
            root.addView(label(title))
            val e = EditText(this)
            e.setSingleLine()
            e.setText(Store.get(this, key, def))
            if (secret) e.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            fields[key] = e
            root.addView(e)
        }
        fun button(t: String, action: () -> Unit) {
            val b = Button(this)
            b.text = t
            b.setOnClickListener { action() }
            root.addView(b)
        }

        val title = TextView(this)
        title.text = "Dream's bKash Verifier  v1.1"
        title.textSize = 22f
        root.addView(title)

        status = TextView(this)
        status.setPadding(0, 16, 0, 8)
        root.addView(status)

        enabledSwitch = Switch(this)
        enabledSwitch.text = "Auto-verify enabled"
        enabledSwitch.isChecked = Store.enabled(this)
        root.addView(enabledSwitch)

        field("projectId", "Firebase Project ID", "go-pixel-f1c13")
        field("apiKey", "Firebase Web API Key", "AIzaSyAu6Lo33AH6GWFDHusaP14fmAHb0LEzWOQ")
        field("email", "Device login email (Firebase Auth user)", "bkash-device@dreamsacademy.app")
        field("password", "Device login password", "", true)
        field("sender", "SMS sender names (comma separated, partial match)", "bKash")

        button("Save settings") { save(); refresh(); toast("Saved") }
        button("1) Allow SMS permissions (receive + read)") {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), 1)
        }
        button("2) Disable battery optimization") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        button("3) Open app settings (Xiaomi: Allow restricted settings / Autostart)") {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, android.net.Uri.parse("package:$packageName")))
        }
        button("Scan SMS inbox now (finds missed bKash SMS)") {
            save()
            if (checkSelfPermission(Manifest.permission.READ_SMS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS), 1)
            } else {
                val n = InboxScanner.scan(this, System.currentTimeMillis() - 3L * 24 * 3600 * 1000)
                Store.log(this, "Inbox scan finished: $n new transaction(s)")
                if (n > 0) SmsReceiver.enqueue(this)
                refresh()
            }
        }
        button("Send test transaction (Tk 0.01)") {
            save()
            val id = "TEST" + (System.currentTimeMillis() % 100000000L)
            Store.addPending(this, JSONObject().put("trxId", id).put("amount", 0.01).put("sender", "test").put("smsTime", "").put("source", "test"))
            Store.log(this, "Test $id queued")
            SmsReceiver.enqueue(this)
            refresh()
        }
        button("Retry pending uploads") { save(); SmsReceiver.enqueue(this); toast("Retrying..."); refresh() }
        button("Refresh") { refresh() }

        root.addView(label("Check an SMS: paste a real bKash message here"))
        testBox = EditText(this)
        testBox.minLines = 3
        testBox.hint = "You have received Tk 500.00 from 01XXXXXXXXX. ... TrxID ABC123DEF4 at ..."
        root.addView(testBox)
        button("Test this SMS text") {
            val (t, why) = Parser.explain(testBox.text.toString())
            testResult.text = if (t != null) "OK - TrxID ${t.id}, Tk ${t.amount}" else "NOT recognised: $why"
        }
        testResult = TextView(this)
        root.addView(testResult)

        root.addView(label("Recent activity"))
        logView = TextView(this)
        logView.textSize = 12f
        root.addView(logView)

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)

        // safety net: scan the inbox every ~15 minutes even if the live SMS broadcast is blocked
        val req = PeriodicWorkRequestBuilder<ScanWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork("scan", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    override fun onResume() {
        super.onResume()
        refresh()
        Thread {
            if (Store.enabled(this) && checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED) {
                val n = InboxScanner.scan(this, System.currentTimeMillis() - 3L * 24 * 3600 * 1000)
                if (n > 0) SmsReceiver.enqueue(this)
            }
            runOnUiThread { refresh() }
        }.start()
    }

    private fun save() {
        for ((k, e) in fields) Store.put(this, k, e.text.toString().trim())
        Store.setEnabled(this, enabledSwitch.isChecked)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun ok(b: Boolean) = if (b) "OK" else "NOT GRANTED"

    private fun refresh() {
        val recv = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val read = checkSelfPermission(Manifest.permission.READ_SMS) == PackageManager.PERMISSION_GRANTED
        val ready = Store.get(this, "password").isNotEmpty()
        val pend = Store.pending(this).length()
        val last = Store.get(this, "lastSms").ifEmpty { "none yet" }
        status.text = "Receive SMS permission: ${ok(recv)}" +
            "\nRead SMS permission: ${ok(read)}" +
            "\nLogin saved: " + (if (ready) "yes" else "NO - enter password") +
            "\nLast SMS this app received: $last" +
            "\nPending uploads: $pend"
        logView.text = Store.logLines(this).joinToString("\n").ifEmpty { "Nothing yet." }
    }
}
