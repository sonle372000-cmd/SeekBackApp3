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
 * MacroDroid, adb...) va thuc hien next -> cho 2s -> lui bai, HOAN TOAN
 * KHONG mo bat ky man hinh/Activity nao. Vi vay, khi ban gan action nay
 * cho 1 nut bam, ung dung ban dang xem/nghe (YouTube, Spotify...) se
 * KHONG bi chuyen sang nen hay bi gian doan chut nao.
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
            // (co tinh KHONG mo Activity xin quyen o day de tranh gian doan
            // app dang xem; ban can tu mo app it nhat 1 lan de cap quyen truoc).
            return
        }

        // goAsync() cho phep receiver "song" them vai giay de hoan tat viec
        // cho 2s roi lui bai, thay vi bi he thong dong tien trinh ngay lap tuc.
        val pendingResult = goAsync()

        try {
            val manager = context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(context, NotifListenerService::class.java)

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
                pendingResult.finish()
            }, DEFAULT_WAIT_MS)

        } catch (e: SecurityException) {
            pendingResult.finish()
        }
    }

    companion object {
        const val ACTION_NEXT_BACK = "com.example.seekback.ACTION_NEXT_BACK"
        private const val DEFAULT_WAIT_MS = 2000L
    }
}
