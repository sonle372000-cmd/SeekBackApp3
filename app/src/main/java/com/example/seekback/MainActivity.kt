package com.example.seekback

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

/**
 * App duy nhat "Next Bai".
 *
 * - Lan dau mo app (chua cap quyen "Notification access"): hien 1 man
 *   hinh yeu cau cap quyen, kem tuy chon CHON 1 UNG DUNG se tu dong
 *   mo sau khi thuc hien xong next + back (delay them 1 giay sau khi
 *   lui bai xong).
 * - Sau khi da cap quyen: MOI LAN mo app se KHONG hien giao dien gi ca,
 *   tu dong: next -> cho 2 giay -> lui bai -> cho them 1 giay -> mo
 *   ung dung da chon (neu co) -> tu dong dong app.
 */
class MainActivity : AppCompatActivity() {

    private val handler = Handler(Looper.getMainLooper())
    private var actionStarted = false

    companion object {
        // Thoi gian cho truoc khi lui lai bai cu: 2 giay
        private const val WAIT_BEFORE_BACK_MS = 2000L
        // Thoi gian cho THEM sau khi lui bai xong, truoc khi mo app da chon: 1 giay
        private const val WAIT_BEFORE_OPEN_APP_MS = 1000L
    }

    override fun onResume() {
        super.onResume()

        // Tranh chay 2 lan neu onResume duoc goi lai nhieu lan truoc khi finish()
        if (actionStarted) return

        if (isNotificationAccessGranted()) {
            actionStarted = true
            runNextThenBack()
        } else {
            // Chua cap quyen -> hien man hinh xin quyen + chon app mo sau
            setContentView(R.layout.activity_main)

            findViewById<Button>(R.id.btnGrantPermission).setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }

            updateSelectedAppLabel()
            findViewById<Button>(R.id.btnChooseApp).setOnClickListener {
                showAppPickerDialog()
            }
        }
    }

    /** Hien danh sach cac ung dung da cai de nguoi dung chon 1 app tu mo sau khi next+back */
    private fun showAppPickerDialog() {
        val pm = packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolveInfos = pm.queryIntentActivities(launcherIntent, 0)
            .distinctBy { it.activityInfo.packageName }
            .sortedBy { it.loadLabel(pm).toString().lowercase() }

        val labels = mutableListOf("Không chọn (không mở app nào)")
        val packageNames = mutableListOf<String?>(null)

        for (info in resolveInfos) {
            labels.add(info.loadLabel(pm).toString())
            packageNames.add(info.activityInfo.packageName)
        }

        AlertDialog.Builder(this)
            .setTitle("Chọn ứng dụng mở sau khi chuyển bài")
            .setItems(labels.toTypedArray()) { _, which ->
                Prefs.setOpenAfterPackage(this, packageNames[which])
                updateSelectedAppLabel()
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateSelectedAppLabel() {
        val tvSelectedApp = findViewById<TextView>(R.id.tvSelectedApp)
        val savedPackage = Prefs.getOpenAfterPackage(this)
        if (savedPackage.isNullOrEmpty()) {
            tvSelectedApp.text = "Chưa chọn ứng dụng nào"
            return
        }
        val label = try {
            val appInfo: ApplicationInfo = packageManager.getApplicationInfo(savedPackage, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            savedPackage
        }
        tvSelectedApp.text = "Đã chọn: $label"
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

        // Buoc 2: sau 2 giay, tu dong quay lai bai/video cu
        handler.postDelayed({
            val c = getActiveController() ?: controller
            c.transportControls.skipToPrevious()

            // Buoc 3: cho them 1 giay roi mo ung dung da chon (neu co)
            handler.postDelayed({
                openSelectedAppIfAny()
                finish()
            }, WAIT_BEFORE_OPEN_APP_MS)

        }, WAIT_BEFORE_BACK_MS)
    }

    private fun openSelectedAppIfAny() {
        val savedPackage = Prefs.getOpenAfterPackage(this) ?: return
        val launchIntent = packageManager.getLaunchIntentForPackage(savedPackage) ?: return
        startActivity(launchIntent)
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
