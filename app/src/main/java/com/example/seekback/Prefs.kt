package com.example.seekback

import android.content.Context

/**
 * Luu va doc lua chon "ung dung se tu mo sau khi next + back xong"
 * bang SharedPreferences, de ca MainActivity (man hinh cai dat lan dau)
 * va NextBackReceiver (chay ngam khi gan nut bam) deu doc duoc.
 */
object Prefs {
    private const val PREFS_NAME = "next_back_prefs"
    private const val KEY_OPEN_APP_PACKAGE = "open_after_package"

    fun getOpenAfterPackage(context: Context): String? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_OPEN_APP_PACKAGE, null)
    }

    fun setOpenAfterPackage(context: Context, packageName: String?) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_OPEN_APP_PACKAGE, packageName).apply()
    }
}
