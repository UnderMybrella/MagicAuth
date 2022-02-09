package dev.brella.magicauth

import android.R
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import android.widget.Toast
import com.google.firebase.messaging.FirebaseMessagingService


class MagicAuthFirebaseService: FirebaseMessagingService() {
    override fun onCreate() {}
    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)
    }
}