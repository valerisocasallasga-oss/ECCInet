package co.edu.eccinet

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale

/** Conserva el código HTTP para decidir si realmente venció la sesión. */
class ApiHttpException(val statusCode: Int, override val message: String) : Exception(message)

class ApiClient {
    suspend fun get(action: String, token: String = "", params: Map<String, String> = emptyMap()): JSONObject =
        request("GET", action, token, null, params)

    suspend fun post(action: String, payload: JSONObject, token: String = ""): JSONObject =
        request("POST", action, token, payload)

    private suspend fun request(
        method: String, action: String, token: String,
        payload: JSONObject? = null, params: Map<String, String> = emptyMap()
    ): JSONObject = withContext(Dispatchers.IO) {
        val fields = mapOf("action" to action) + params
        val query = fields.entries.joinToString("&") { (k, v) ->
            URLEncoder.encode(k, "UTF-8") + "=" + URLEncoder.encode(v, "UTF-8")
        }
        val conn = URL("${BuildConfig.API_URL}?$query").openConnection() as HttpURLConnection
        try {
            conn.requestMethod = method
            conn.connectTimeout = 8000
            conn.readTimeout = 12000
            conn.setRequestProperty("Accept", "application/json")
            if (token.isNotBlank()) {
                conn.setRequestProperty("Authorization", "Bearer $token")
                // Algunos Apache/XAMPP no reenvían Authorization al proceso PHP.
                // Esta cabecera propia evita perder la sesión por ese motivo.
                conn.setRequestProperty("X-ECCINET-Token", token)
            }
            if (method == "POST") {
                conn.doOutput = true
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                conn.outputStream.use { it.write((payload ?: JSONObject()).toString().toByteArray(StandardCharsets.UTF_8)) }
            }
            val status = conn.responseCode
            val bytes = (if (status in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() } ?: ""
            val result = try { JSONObject(bytes) } catch (_: Exception) {
                throw IllegalStateException("El servidor respondió de manera inesperada (HTTP $status). Revisa que XAMPP y la URL funcionen.")
            }
            if (status !in 200..299 || !result.optBoolean("ok", false))
                throw ApiHttpException(status, result.optString("error", "Error HTTP $status"))
            result
        } finally { conn.disconnect() }
    }
}

fun JSONArray?.asObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { optJSONObject(it) }
}
fun money(cop: Int): String = "$" + String.format(Locale.GERMANY, "%,d", cop)
fun JSONObject.text(key: String): String = optString(key, "")
fun JSONObject.int(key: String): Int = optInt(key, 0)
fun payload(vararg kv: Pair<String, Any>): JSONObject = JSONObject().apply {
    kv.forEach { (key, value) -> put(key, value) }
}
