package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences

object ServerStorage {
    private const val PREFS_NAME = "server_storage"
    private const val KEY_PREFIX_FINGERPRINT = "fingerprint_"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveServerFingerprint(context: Context, serverAddress: String, fingerprint: String) {
        getPrefs(context).edit().putString(KEY_PREFIX_FINGERPRINT + serverAddress, fingerprint).apply()
    }

    fun getServerFingerprint(context: Context, serverAddress: String): String? {
        return getPrefs(context).getString(KEY_PREFIX_FINGERPRINT + serverAddress, null)
    }

    fun removeServer(context: Context, serverAddress: String) {
        getPrefs(context).edit().remove(KEY_PREFIX_FINGERPRINT + serverAddress).apply()
    }
}
