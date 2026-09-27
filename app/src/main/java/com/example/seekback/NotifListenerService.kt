package com.example.seekback

import android.service.notification.NotificationListenerService

/**
 * App KHONG can doc noi dung thong bao.
 * Service nay ton tai chi de he thong Android cho phep
 * app dung MediaSessionManager.getActiveSessions() de lay
 * duoc phien nhac (MediaController) dang phat o cac app khac.
 *
 * Nguoi dung phai bat quyen nay 1 lan trong:
 * Cai dat -> Ung dung -> Truy cap dac biet -> Truy cap thong bao
 * (hoac Settings -> Apps -> Special access -> Notification access)
 */
class NotifListenerService : NotificationListenerService()
