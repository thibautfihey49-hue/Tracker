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
        val notif = Notification.Builder(this, "tracker_channel").setContentTitle("Partage de position actif").setContentText("Envoi toutes les 30s").setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build()
        startForeground(1, notif)
        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        Log.d("TrackerChild", "Service demarre vers $parentNumber")
        if (parentNumber.isEmpty()) { stopSelf(); return START_NOT_STICKY }
        fun send(lat: Double, lon: Double) {
            val txt = "TRACKER:$lat,$lon,${System.currentTimeMillis()}"
            try {
                val sms = SmsManager.getDefault()
                // 1. SMS normal - fiable
                sms.sendTextMessage(parentNumber, null, txt, null, null)
                Log.d("TrackerChild", "SMS normal envoye: $txt")
                // 2. Data SMS - invisible si operateur autorise
                try {
                    val payload = "$lat,$lon,10,${System.currentTimeMillis()}".toByteArray()
                    sms.sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                    Log.d("TrackerChild", "Data SMS 8901 envoye")
                } catch(_:Exception){}
            } catch(e:Exception){ Log.e("TrackerChild", "Envoi fail: ${e.message}", e) }
        }
        try {
            fused.lastLocation.addOnSuccessListener { loc -> loc?.let { send(it.latitude, it.longitude) } }
        } catch(_:Exception){}
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000).setMinUpdateIntervalMillis(10000).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                Log.d("TrackerChild", "Nouvelle loc: ${loc.latitude},${loc.longitude}")
                send(loc.latitude, loc.longitude)
            }
        }
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch(e:SecurityException){ Log.e("TrackerChild", "Perm loc manquante", e) }
        return START_STICKY
    }
}
