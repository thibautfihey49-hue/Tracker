package com.family.tracker.child
import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.telephony.SmsManager
import com.google.android.gms.location.*
import android.util.Log

class LocationService : Service() {
    private lateinit var fused: FusedLocationProviderClient
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        val ch = NotificationChannel("tracker_channel","Tracker Enfant", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, "tracker_channel")
            .setContentTitle("Partage de position actif")
            .setContentText("Envoi position réelle toutes les 60s")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build()
        startForeground(1, notif)

        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        if (parentNumber.isEmpty()) { stopSelf(); return START_NOT_STICKY }

        // 1. Envoi immediat lastLocation si dispo
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc?.let {
                    val payload = "${it.latitude},${it.longitude},${it.accuracy},${System.currentTimeMillis()}".toByteArray()
                    try { SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                        Log.d("Tracker", "LastLocation envoyée: ${it.latitude},${it.longitude}")
                    } catch(e:Exception){ Log.e("Tracker", "SMS fail", e) }
                }
            }
        } catch(_:Exception){}

        // 2. Updates temps réel toutes les 30s (test) / 60s prod
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000).setMinUpdateIntervalMillis(10000).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                val payload = "${loc.latitude},${loc.longitude},${loc.accuracy},${System.currentTimeMillis()}".toByteArray()
                try {
                    SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                    Log.d("Tracker", "Location envoyée: ${loc.latitude},${loc.longitude}")
                } catch(e:Exception){ Log.e("Tracker", "SMS fail", e) }
            }
        }
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch(e:SecurityException){ e.printStackTrace() }
        return START_STICKY
    }
}
