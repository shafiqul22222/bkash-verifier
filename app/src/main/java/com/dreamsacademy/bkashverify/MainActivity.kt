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
import org.json.JSONObject

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var logView: TextView
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
        title.text = "Dream's bKash Verifier"
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
        field("sender", "SMS sender name", "bKash")

        button("Save settings") { save(); refresh(); toast("Saved") }
        button("1) Allow SMS permission") {
            requestPermissions(arrayOf(Manifest.permission.RECEIVE_SMS), 1)
        }
        button("2) Disable battery optimization") {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
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
        button("Refresh log") { refresh() }

        root.addView(label("Recent activity"))
        logView = TextView(this)
        logView.textSize = 12f
        root.addView(logView)

        val sv = ScrollView(this)
        sv.addView(root)
        setContentView(sv)
    }

    override fun onResume() { super.onResume(); refresh() }

    private fun save() {
        for ((k, e) in fields) Store.put(this, k, e.text.toString().trim())
        Store.setEnabled(this, enabledSwitch.isChecked)
    }

    private fun toast(m: String) = Toast.makeText(this, m, Toast.LENGTH_SHORT).show()

    private fun refresh() {
        val perm = checkSelfPermission(Manifest.permission.RECEIVE_SMS) == PackageManager.PERMISSION_GRANTED
        val ready = Store.get(this, "password").isNotEmpty()
        val pend = Store.pending(this).length()
        status.text = "SMS permission: " + (if (perm) "OK" else "NOT GRANTED") +
            "\nLogin saved: " + (if (ready) "yes" else "NO - enter password") +
            "\nPending uploads: $pend"
        logView.text = Store.logLines(this).joinToString("\n").ifEmpty { "Nothing yet." }
    }
}
