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
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var tvStatus: TextView
    private lateinit var etWaitSeconds: EditText
    private lateinit var btnSeek: Button
    private lateinit var btnGrantPermission: Button

    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    
        tvStatus = findViewById(R.id.tvStatus)
        etWaitSeconds = findViewById(R.id.etWaitSeconds)
        btnSeek = findViewById(R.id.btnSeek)
        btnGrantPermission = findViewById(R.id.btnGrantPermission)
    
        btnGrantPermission.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
    
        btnSeek.setOnClickListener {
            doNextThenBack()
        }
    
        // THÊM DÒNG NÀY: tự động chạy ngay khi mở app, không cần bấm nút
        doNextThenBack()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun isNotificationAccessGranted(): Boolean {
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(packageName)
    }

    private fun updateStatus() {
        tvStatus.text = if (isNotificationAccessGranted()) {
            "Đã cấp quyền. Mở bài hát và bấm nút bên dưới."
        } else {
            "Chưa cấp quyền truy cập thông báo. Bấm nút bên dưới để cấp quyền."
        }
    }

    /**
     * Lay ra MediaController dau tien dang co (bai hat dang mo o app nhac nao do).
     * Tra ve null neu chua cap quyen hoac khong co app nhac nao dang chay.
     */
    private fun getActiveController(): MediaController? {
        return try {
            val manager = getSystemService(MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, NotifListenerService::class.java)
            val controllers = manager.getActiveSessions(componentName)
            // Uu tien phien dang PLAYING, neu khong co thi lay phien dau tien
            controllers.firstOrNull { it.playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()
        } catch (e: SecurityException) {
            null
        }
    }

    private fun doNextThenBack() {
        if (!isNotificationAccessGranted()) {
            Toast.makeText(this, "Bạn cần cấp quyền truy cập thông báo trước", Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            return
        }

        val controller = getActiveController()
        if (controller == null) {
            Toast.makeText(this, "Không tìm thấy bài hát/video nào đang phát", Toast.LENGTH_LONG).show()
            return
        }

        val waitSeconds = etWaitSeconds.text.toString().toLongOrNull() ?: 5L
        val waitMs = waitSeconds * 1000L

        // Buoc 1: chuyen sang bai/video tiep theo
        controller.transportControls.skipToNext()
        Toast.makeText(this, "Đã chuyển bài tiếp theo, sẽ quay lại bài cũ sau ${waitSeconds}s", Toast.LENGTH_SHORT).show()

        // Buoc 2: sau khoang thoi gian cho, tu dong quay lai bai/video cu
        handler.postDelayed({
            // Lay lai controller phong khi phien nhac da doi
            val c = getActiveController() ?: controller
            c.transportControls.skipToPrevious()
            Toast.makeText(this, "Đã quay lại bài cũ", Toast.LENGTH_SHORT).show()
        }, waitMs)
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
    }
}
