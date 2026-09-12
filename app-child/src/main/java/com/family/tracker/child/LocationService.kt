package com.family.tracker.child
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import com.google.android.gms.location.*
class LocationService : Service() {
    private lateinit var fused: FusedLocationProviderClient
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() { super.onCreate(); fused = LocationServices.getFusedLocationProviderClient(this)
        val ch = NotificationChannel("tracker_channel","Tracker", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, "tracker_channel").setContentTitle("Partage de position actif").setContentText("Partage toutes les minutes - clique pour arreter").setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build()
        startForeground(1, notif)
        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                val payload = "${loc.latitude},${loc.longitude},${loc.accuracy},${System.currentTimeMillis()}".toByteArray()
                try { SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null) } catch(e:Exception){}
            }
        }
        fused.requestLocationUpdates(request, callback, mainLooper)
        return START_STICKY
    }
}
