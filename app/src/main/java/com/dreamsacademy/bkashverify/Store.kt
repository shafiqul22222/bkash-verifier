package com.dreamsacademy.bkashverify

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Store {
    private fun prefs(c: Context) = c.getSharedPreferences("cfg", Context.MODE_PRIVATE)

    fun get(c: Context, key: String, def: String = ""): String = prefs(c).getString(key, def) ?: def
    fun put(c: Context, key: String, value: String) { prefs(c).edit().putString(key, value).apply() }
    fun enabled(c: Context): Boolean = prefs(c).getBoolean("enabled", true)
    fun setEnabled(c: Context, v: Boolean) { prefs(c).edit().putBoolean("enabled", v).apply() }

    @Synchronized
    fun pending(c: Context): JSONArray = JSONArray(get(c, "pending", "[]"))

    @Synchronized
    fun addPending(c: Context, o: JSONObject) {
        val a = pending(c)
        for (i in 0 until a.length()) {
            if (a.getJSONObject(i).getString("trxId") == o.getString("trxId")) return
        }
        a.put(o)
        put(c, "pending", a.toString())
    }

    @Synchronized
    fun removePending(c: Context, trxId: String) {
        val a = pending(c)
        val n = JSONArray()
        for (i in 0 until a.length()) {
            val o = a.getJSONObject(i)
            if (o.getString("trxId") != trxId) n.put(o)
        }
        put(c, "pending", n.toString())
    }

    @Synchronized
    fun log(c: Context, msg: String) {
        val old = JSONArray(get(c, "log", "[]"))
        val n = JSONArray()
        val stamp = SimpleDateFormat("dd MMM HH:mm:ss", Locale.getDefault()).format(Date())
        n.put("$stamp  $msg")
        for (i in 0 until minOf(old.length(), 39)) n.put(old.get(i))
        put(c, "log", n.toString())
    }

    fun logLines(c: Context): List<String> {
        val a = JSONArray(get(c, "log", "[]"))
        return (0 until a.length()).map { a.getString(it) }
    }
}
