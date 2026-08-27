package com.userapp.applock

import android.content.Context

object PrefsManager {
    private const val PREF = "app_lock_prefs"
    private const val KEY_PACKAGES = "blocked_packages"
    private const val KEY_ALLOW_PREFIX = "allow_until_"

    fun getBlockedPackages(context: Context): MutableSet<String> {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        return prefs.getStringSet(KEY_PACKAGES, mutableSetOf())?.toMutableSet() ?: mutableSetOf()
    }

    fun addPackage(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = getBlockedPackages(context)
        set.add(packageName)
        prefs.edit().putStringSet(KEY_PACKAGES, set).apply()
    }

    fun removePackage(context: Context, packageName: String) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val set = getBlockedPackages(context)
        set.remove(packageName)
        prefs.edit().putStringSet(KEY_PACKAGES, set).apply()
    }

    // 카운트다운 완료 또는 긴급실행 시 일정 시간 재감지를 건너뛰기 위한 허용 창구
    fun setAllowUntil(context: Context, packageName: String, timestampMillis: Long) {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        prefs.edit().putLong(KEY_ALLOW_PREFIX + packageName, timestampMillis).apply()
    }

    fun isAllowed(context: Context, packageName: String): Boolean {
        val prefs = context.getSharedPreferences(PREF, Context.MODE_PRIVATE)
        val until = prefs.getLong(KEY_ALLOW_PREFIX + packageName, 0L)
        return System.currentTimeMillis() < until
    }
}
