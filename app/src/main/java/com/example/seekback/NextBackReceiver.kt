package com.example.seekback

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings

/**
 * Nhan mot broadcast (tin hieu ngam) tu ben ngoai (Button Mapper, Tasker,
 * MacroDroid, adb...) va thuc hien next -> cho 2s -> lui bai -> cho them 1s
 * -> mo ung dung da chon (neu co), HOAN TOAN KHONG mo Activity/man hinh
 * cua app Next Bai. Vi vay khi gan action nay cho 1 nut bam, ung dung ban
 * dang xem/nghe (YouTube, Spotify...) se KHONG bi chuyen sang nen hay bi
 * gian doan chut nao (tru buoc mo ung dung da chon o cuoi, neu ban co chon).
 *
 * De kich hoat, gui 1 broadcast voi action:
 *     com.example.seekback.ACTION_NEXT_BACK
 * Vi du test bang adb (may tinh, cam dien thoai qua USB):
 *     adb shell am broadcast -a com.example.seekback.ACTION_NEXT_BACK -p com.example.seekback
 *
 * Trong cac app gan phim tat (Button Mapper, Tasker, MacroDroid...),
 * chon kieu hanh dong "Send Broadcast" / "Gui Intent" (KHONG phai
 * "Open App"/"Mo ung dung"), roi dien:
 *   - Action:  com.example.seekback.ACTION_NEXT_BACK
 *   - Package: com.example.seekback
 */
class NextBackReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_NEXT_BACK) return

        // Kiem tra quyen truy cap thong bao (bat buoc phai cap 1 lan truoc do)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        if (flat == null || !flat.contains(context.packageName)) {
            // Chua cap quyen -> khong the dieu khien media, bo qua trong im lang
            return
        }

        // goAsync() cho phep receiver "song" them vai giay de hoan tat cac
        // buoc cho + mo app, thay vi bi he thong dong tien trinh ngay lap tuc.
        val pendingResult = goAsync()
        val appContext = context.applicationContext

        try {
            val manager = appContext.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(appContext, NotifListenerService::class.java)

            val controllers = manager.getActiveSessions(componentName)
            val controller = controllers.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
                ?: controllers.firstOrNull()

            if (controller == null) {
                pendingResult.finish()
                return
            }

            // Buoc 1: chuyen bai/video tiep theo — thuc hien ngay tren app dang mo
            controller.transportControls.skipToNext()

            // Buoc 2: sau 2 giay, tu dong lui lai bai/video cu
            Handler(Looper.getMainLooper()).postDelayed({
                val latestControllers = try {
                    manager.getActiveSessions(componentName)
                } catch (e: SecurityException) {
                    emptyList()
                }
                val c = latestControllers.firstOrNull() ?: controller
                c.transportControls.skipToPrevious()

                // Buoc 3: cho them 1 giay, roi mo ung dung da chon (neu co)
                Handler(Looper.getMainLooper()).postDelayed({
                    openSelectedAppIfAny(appContext)
                    pendingResult.finish()
                }, WAIT_BEFORE_OPEN_APP_MS)

            }, WAIT_BEFORE_BACK_MS)

        } catch (e: SecurityException) {
            pendingResult.finish()
        }
    }

    private fun openSelectedAppIfAny(context: Context) {
        val savedPackage = Prefs.getOpenAfterPackage(context) ?: return
        val launchIntent = context.packageManager.getLaunchIntentForPackage(savedPackage) ?: return
        // Bat buoc phai co FLAG_ACTIVITY_NEW_TASK vi dang khoi chay Activity
        // tu Context cua BroadcastReceiver, khong phai tu 1 Activity dang co san.
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(launchIntent)
    }

    companion object {
        const val ACTION_NEXT_BACK = "com.example.seekback.ACTION_NEXT_BACK"
        private const val WAIT_BEFORE_BACK_MS = 2000L
        private const val WAIT_BEFORE_OPEN_APP_MS = 1000L
    }
}
