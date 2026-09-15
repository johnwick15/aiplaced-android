package com.cscaprep.app.data

import android.content.Context
import java.util.UUID

class SessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("cscaprep_native_session", Context.MODE_PRIVATE)
    var token: String get() = prefs.getString("token", "") ?: ""; set(v) { prefs.edit().putString("token", v).apply() }
    var name: String get() = prefs.getString("name", "") ?: ""; set(v) { prefs.edit().putString("name", v).apply() }
    var email: String get() = prefs.getString("email", "") ?: ""; set(v) { prefs.edit().putString("email", v).apply() }
    var language: String get() = prefs.getString("language", "en") ?: "en"; set(v) { prefs.edit().putString("language", v).apply() }
    val guestKey: String get() { val old = prefs.getString("guest", ""); if (!old.isNullOrBlank()) return old; val fresh = "android-" + UUID.randomUUID().toString().replace("-", ""); prefs.edit().putString("guest", fresh).apply(); return fresh }
    val loggedIn get() = token.isNotBlank()
    fun save(result: AuthResult) { token = result.token; name = result.user.name; email = result.user.email; language = result.user.language }
    fun clear() { prefs.edit().remove("token").remove("name").remove("email").apply() }
}
