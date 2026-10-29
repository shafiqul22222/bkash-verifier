package com.dreamsacademy.bkashverify

import android.content.Context
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Api {
    class Res(val ok: Boolean, val dup: Boolean, val msg: String)

    private fun post(url: String, body: String, token: String?): Pair<Int, String> {
        val con = URL(url).openConnection() as HttpURLConnection
        con.requestMethod = "POST"
        con.connectTimeout = 15000
        con.readTimeout = 15000
        con.doOutput = true
        con.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        if (token != null) con.setRequestProperty("Authorization", "Bearer $token")
        con.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
        val code = con.responseCode
        val stream = if (code in 200..299) con.inputStream else con.errorStream
        val text = stream?.bufferedReader()?.use { it.readText() } ?: ""
        con.disconnect()
        return Pair(code, text)
    }

    private fun errMsg(text: String): String = try {
        JSONObject(text).getJSONObject("error").getString("message")
    } catch (e: Exception) { text.take(120) }

    /** Signs in as the dedicated "device" Firebase user. Returns (idToken, errorMessage). */
    fun signIn(c: Context): Pair<String?, String> {
        return try {
            val key = Store.get(c, "apiKey")
            val body = JSONObject()
                .put("email", Store.get(c, "email"))
                .put("password", Store.get(c, "password"))
                .put("returnSecureToken", true).toString()
            val (code, text) = post("https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=$key", body, null)
            if (code == 200) Pair(JSONObject(text).getString("idToken"), "") else Pair(null, errMsg(text))
        } catch (e: Exception) { Pair(null, e.message ?: "network error") }
    }

    /** Saves one transaction as document automated_bkash_trx/{trxId}. */
    fun send(c: Context, o: JSONObject, token: String): Res {
        return try {
            val pid = Store.get(c, "projectId")
            val id = o.getString("trxId")
            val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }.format(Date())
            val fields = JSONObject()
                .put("trxId", JSONObject().put("stringValue", id))
                .put("amount", JSONObject().put("doubleValue", o.getDouble("amount")))
                .put("sender", JSONObject().put("stringValue", o.optString("sender")))
                .put("smsTime", JSONObject().put("stringValue", o.optString("smsTime")))
                .put("isUsed", JSONObject().put("booleanValue", false))
                .put("source", JSONObject().put("stringValue", o.optString("source", "android-app")))
                .put("receivedAt", JSONObject().put("timestampValue", iso))
            val url = "https://firestore.googleapis.com/v1/projects/$pid/databases/(default)/documents/automated_bkash_trx?documentId=$id"
            val (code, text) = post(url, JSONObject().put("fields", fields).toString(), token)
            when {
                code in 200..299 -> Res(true, false, "ok")
                code == 409 -> Res(true, true, "already exists")
                else -> Res(false, false, "HTTP $code: ${errMsg(text)}")
            }
        } catch (e: Exception) { Res(false, false, e.message ?: "network error") }
    }
}
