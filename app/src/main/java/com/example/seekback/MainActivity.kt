package com.example.seekback

import android.content.ComponentName
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * App duy nhat "Next Bai".
 *
 * - Lan dau mo app (chua cap quyen "Notification access"): hien 1 man
 *   hinh don gian yeu cau cap quyen, kem nut mo thang toi man hinh
 *   cai dat he thong de bat quyen.
 * - Sau khi da cap quyen: MOI LAN mo app se KHONG hien giao dien gi ca,
 *   tu dong chuyen bai/video dang phat sang bai tiep theo (skipToNext),
 *   cho DEFAULT_WAIT_MS (2 giay) roi tu dong quay lai bai/video cu
 *   (skipToPrevious), sau do tu dong dong app.
 */
class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var actionStarted = false

    companion object {
        // Thoi gian cho mac dinh truoc khi lui lai bai cu: 2 giay
        private const val DEFAULT_WAIT_MS = 2000L
    }

    override fun onResume() {
        super.onResume()

        // Tranh chay 2 lan neu onResume duoc goi lai nhieu lan truoc khi finish()
        if (actionStarted) return

        if (isNotificationAccessGranted()) {
            actionStarted = true
            runNextThenBack()
        } else {
            // Chua cap quyen -> hien man hinh xin quyen
            setContentView(R.layout.activity_main)
            findViewById<Button>(R.id.btnGrantPermission).setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }
    }

    private fun runNextThenBack() {
        val controller = getActiveController()
        if (controller == null) {
            Toast.makeText(this, "Không tìm thấy bài hát/video nào đang phát", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // Buoc 1: chuyen sang bai/video tiep theo
        controller.transportControls.skipToNext()

        // Buoc 2: sau 2 giay, tu dong quay lai bai/video cu roi dong app
        handler.postDelayed({
            val c = getActiveController() ?: controller
            c.transportControls.skipToPrevious()
            finish()
        }, DEFAULT_WAIT_MS)
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
}
