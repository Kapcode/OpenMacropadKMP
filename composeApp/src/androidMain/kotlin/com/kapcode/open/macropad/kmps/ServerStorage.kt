package com.kapcode.open.macropad.kmps

import android.content.Context
import android.content.SharedPreferences

object ServerStorage {
    private const val PREFS_NAME = "server_storage"
    private const val KEY_PREFIX_FINGERPRINT = "fingerprint_"
    private const val KEY_DEFAULT_SERVER = "default_server"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveServerFingerprint(context: Context, serverAddress: String, fingerprint: String) {
        getPrefs(context).edit().putString(KEY_PREFIX_FINGERPRINT + serverAddress, fingerprint).apply()
    }

    fun getServerFingerprint(context: Context, serverAddress: String): String? {
        return getPrefs(context).getString(KEY_PREFIX_FINGERPRINT + serverAddress, null)
    }

    fun setDefaultServer(context: Context, serverAddress: String?) {
        getPrefs(context).edit().putString(KEY_DEFAULT_SERVER, serverAddress).apply()
    }

    fun getDefaultServer(context: Context): String? {
        return getPrefs(context).getString(KEY_DEFAULT_SERVER, null)
    }

    fun removeServer(context: Context, serverAddress: String) {
        val prefs = getPrefs(context)
        val edit = prefs.edit()
        edit.remove(KEY_PREFIX_FINGERPRINT + serverAddress)
        if (prefs.getString(KEY_DEFAULT_SERVER, null) == serverAddress) {
            edit.remove(KEY_DEFAULT_SERVER)
        }
        edit.apply()
    }
}
