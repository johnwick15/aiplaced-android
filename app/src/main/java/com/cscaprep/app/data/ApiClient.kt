package com.cscaprep.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

class ApiClient(private val session: SessionStore) {
    private val root = "https://cscaprep.com/wp-json/cscaprep/v1"
    private fun enc(v: String) = URLEncoder.encode(v, "UTF-8")

    suspend fun login(email: String, password: String) = withContext(Dispatchers.IO) {
        parseAuth(request("/mobile/login", "POST", JSONObject().put("email", email).put("password", password), false))
    }
    suspend fun register(name: String, email: String, password: String, language: String) = withContext(Dispatchers.IO) {
        parseAuth(request("/mobile/register", "POST", JSONObject().put("name", name).put("email", email).put("password", password).put("language", language), false))
    }
    suspend fun authConfig() = withContext(Dispatchers.IO) {
        val o = request("/mobile/auth/config", "POST", JSONObject(), false)
        AuthConfig(o.optBoolean("googleEnabled"), o.optString("googleClientId"), o.optString("requestId"), o.optString("nonce"))
    }
    suspend fun googleLogin(idToken: String, config: AuthConfig) = withContext(Dispatchers.IO) {
        parseAuth(request("/mobile/google", "POST", JSONObject().put("idToken", idToken).put("requestId", config.requestId).put("nonce", config.nonce).put("language", session.language), false))
    }
    suspend fun me() = withContext(Dispatchers.IO) { parseUser(request("/mobile/me").getJSONObject("user")) }
    suspend fun resendVerification() = withContext(Dispatchers.IO) { request("/mobile/resend-verification", "POST", JSONObject()).optBoolean("sent") }
    suspend fun forgotPassword(email: String) = withContext(Dispatchers.IO) { request("/mobile/forgot-password", "POST", JSONObject().put("email", email), false); true }
    suspend fun logout() = withContext(Dispatchers.IO) { runCatching { request("/mobile/logout", "POST", JSONObject()) }; session.clear() }

    suspend fun bootstrap() = withContext(Dispatchers.IO) {
        val json = request("/bootstrap?guestKey=${enc(session.guestKey)}&language=${enc(session.language)}")
        val subjectsObject = json.optJSONObject("subjects") ?: JSONObject()
        val subjects = buildList {
            val keys = subjectsObject.keys()
            while (keys.hasNext()) { val id = keys.next(); val o = subjectsObject.getJSONObject(id); add(Subject(id, o.optString("label", id), o.optString("symbol"), o.optInt("questions"), o.optInt("minutes"))) }
        }
        val exams = parseExams(json.optJSONArray("exams") ?: JSONArray())
        val attemptsArray = json.optJSONArray("history") ?: JSONArray()
        val attempts = buildList { for (i in 0 until attemptsArray.length()) { val o = attemptsArray.getJSONObject(i); add(Attempt(o.optString("title", o.optString("subject")), o.optInt("score"), o.optInt("total"), o.optString("created_at"))) } }
        val a = json.optJSONObject("access") ?: JSONObject()
        Bootstrap(subjects, exams, attempts, Access(a.optString("plan", "guest"), a.optBoolean("isPro")), json.optBoolean("emailVerified"))
    }

    suspend fun prep(subject: String, difficulty: String) = withContext(Dispatchers.IO) {
        parseQuestion(request("/prep?subject=${enc(subject)}&difficulty=${enc(difficulty)}&guestKey=${enc(session.guestKey)}&language=${enc(session.language)}"))
    }

    suspend fun exam(id: Long, examLanguage: String) = withContext(Dispatchers.IO) {
        val json = request("/exam/$id?guestKey=${enc(session.guestKey)}&examLanguage=${enc(examLanguage)}")
        val arr = json.optJSONArray("questions") ?: JSONArray()
        val questions = buildList { for (i in 0 until arr.length()) add(parseQuestion(arr.getJSONObject(i))) }
        ExamContent(parseExam(json.getJSONObject("exam")), questions, json.optString("examLanguage", examLanguage))
    }

    suspend fun submit(examId: Long, answers: Map<Long, Int>, seconds: Int) = withContext(Dispatchers.IO) {
        val answerJson = JSONObject(); answers.forEach { (id, value) -> answerJson.put(id.toString(), value) }
        val body = JSONObject().put("examId", examId).put("answers", answerJson).put("durationSeconds", seconds).put("language", session.language).put("guestKey", session.guestKey)
        val o = request("/submit", "POST", body)
        Result(o.optInt("score"), o.optInt("total"), o.optInt("percent"), o.optInt("correct"), o.optInt("incorrect"), o.optInt("unanswered"))
    }

    suspend fun saveLanguage(language: String) = withContext(Dispatchers.IO) {
        if (session.loggedIn) request("/language", "POST", JSONObject().put("language", language).put("guestKey", session.guestKey))
        session.language = language
    }

    suspend fun billingConfig() = withContext(Dispatchers.IO) {
        val o = request("/mobile/billing/config"); val plansJson = o.optJSONArray("plans") ?: JSONArray()
        val plans = buildList { for (i in 0 until plansJson.length()) { val p = plansJson.getJSONObject(i); add(BillingPlan(p.optInt("months"), p.optString("label"), p.optInt("amount"), p.optString("currency"), p.optString("priceLabel"), p.optBoolean("featured"))) } }
        BillingConfig(o.optBoolean("enabled"), o.optString("publishableKey"), plans, o.optString("plan", "free"), o.optString("proUntil"))
    }

    suspend fun createPaymentIntent(months: Int) = withContext(Dispatchers.IO) {
        val o = request("/mobile/billing/payment-intent", "POST", JSONObject().put("months", months))
        PaymentIntentData(o.getString("clientSecret"), o.getString("publishableKey"), o.optInt("months", months))
    }

    private fun parseAuth(o: JSONObject) = AuthResult(o.getString("token"), parseUser(o.getJSONObject("user")))
    private fun parseUser(o: JSONObject) = User(o.optLong("id"), o.optString("name"), o.optString("email"), o.optBoolean("emailVerified"), o.optString("plan", "free"), o.optString("language", "en"))
    private fun parseExams(a: JSONArray) = buildList { for (i in 0 until a.length()) add(parseExam(a.getJSONObject(i))) }
    private fun parseExam(o: JSONObject) = Exam(o.optLong("id"), o.optString("title"), o.optString("subject"), o.optInt("duration", 60), o.optInt("count"), o.optString("difficulty", "standard"))
    private fun parseQuestion(o: JSONObject): Question {
        val a = o.optJSONArray("options") ?: JSONArray(); val options = buildList { for (i in 0 until a.length()) add(a.optString(i)) }
        return Question(o.optLong("id"), o.optString("subject"), o.optString("topic"), o.optString("prompt"), options, if (o.has("answer") && !o.isNull("answer")) o.optInt("answer") else null, o.optString("explanation"), o.optString("principle"))
    }

    private fun request(path: String, method: String = "GET", body: JSONObject? = null, authenticated: Boolean = true): JSONObject {
        val conn = (URL(root + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method; connectTimeout = 15000; readTimeout = 45000; useCaches = false
            setRequestProperty("Accept", "application/json"); setRequestProperty("Content-Type", "application/json; charset=utf-8"); setRequestProperty("Cache-Control", "no-store, no-cache")
            if (authenticated && session.token.isNotBlank()) setRequestProperty("Authorization", "Bearer ${session.token}")
            if (body != null && method != "GET") { doOutput = true; outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) } }
        }
        val status = conn.responseCode; val stream = if (status in 200..299) conn.inputStream else conn.errorStream
        val text = stream?.use { BufferedReader(InputStreamReader(it)).readText() }.orEmpty(); conn.disconnect()
        val json = if (text.isBlank()) JSONObject() else runCatching { JSONObject(text) }.getOrElse { JSONObject().put("message", text) }
        if (status !in 200..299) throw ApiException(json.optString("message", "Request failed ($status)"), status, json.optString("code"))
        return json
    }
}
