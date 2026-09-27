package com.example.seekback

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * Day la "app thu 2" (icon rieng ngoai man hinh chinh, ten hien thi la
 * "Next Bai"). KHONG co giao dien nguoi dung (khong setContentView).
 *
 * Bam vao icon nay se:
 * 1. Tu dong lay bai hat/video dang phat o bat ky app nhac/video nao
 *    (khong can tu tim/chon bai).
 * 2. Chuyen sang bai/video tiep theo (skipToNext).
 * 3. Sau X giay cho (X duoc cau hinh tu app "SeekBack Cai dat"),
 *    tu dong quay lai bai/video cu (skipToPrevious).
 * 4. Tu dong dong lai (finish()) sau khi xong, tra nguoi dung ve man hinh chinh.
 */
class QuickNextBackActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Co y KHONG goi setContentView() -> nho theme trong suot,
        // nguoi dung se khong thay giao dien nao hien ra ca.

        if (!isNotificationAccessGranted()) {
            Toast.makeText(
                this,
                "Chưa cấp quyền. Mở app \"SeekBack Cài đặt\" để cấp quyền trước.",
                Toast.LENGTH_LONG
            ).show()
            finish()
            return
        }

        val controller = getActiveController()
        if (controller == null) {
            Toast.makeText(this, "Không tìm thấy bài hát/video nào đang phát", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val waitSeconds = prefs.getLong(KEY_WAIT_SECONDS, 5L)
        val waitMs = waitSeconds * 1000L

        // Buoc 1: chuyen sang bai/video tiep theo
        controller.transportControls.skipToNext()
        Toast.makeText(this, "Đã chuyển bài tiếp theo...", Toast.LENGTH_SHORT).show()

        // Buoc 2: sau khoang thoi gian cho, tu dong quay lai bai/video cu, roi dong app
        handler.postDelayed({
            val c = getActiveController() ?: controller
            c.transportControls.skipToPrevious()
            Toast.makeText(this, "Đã quay lại bài cũ", Toast.LENGTH_SHORT).show()
            finish()
        }, waitMs)
    }

    private fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun getActiveController(): MediaController? {
        return try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, NotifListenerService::class.java)
            val controllers = manager.getActiveSessions(componentName)
            controllers.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()
        } catch (e: SecurityException) {
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }

    companion object {
        const val PREFS_NAME = "seekback_prefs"
        const val KEY_WAIT_SECONDS = "wait_seconds"
    }
}
