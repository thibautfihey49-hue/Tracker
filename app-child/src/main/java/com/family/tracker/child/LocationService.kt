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
    private var lastSentTime = 0L
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        val ch = NotificationChannel("tracker_channel","Tracker Enfant", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, "tracker_channel")
            .setContentTitle("Partage ultra-précis actif")
            .setContentText("GPS haute précision - 1 min")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setOngoing(true).build()
        startForeground(1, notif)
        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        Log.d("TrackerChild", "Service 1min demarre vers $parentNumber")
        if (parentNumber.isEmpty()) { stopSelf(); return START_NOT_STICKY }

        fun sendData(lat: Double, lon: Double, acc: Float, speed: Float) {
            val now = System.currentTimeMillis()
            if (now - lastSentTime < 55000) return // anti-spam 55s
            // filtre precision Google Maps style: on ignore si >50m
            if (acc > 50) {
                Log.d("TrackerChild", "Ignore precision faible $acc m")
                return
            }
            lastSentTime = now
            try {
                val payload = "$lat,$lon,$acc,$speed,$now".toByteArray(Charsets.UTF_8)
                SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                Log.d("TrackerChild", "Data SMS 1min envoye: $lat,$lon acc=${acc}m speed=${speed}m/s")
            } catch(e:Exception){ Log.e("TrackerChild", "Fail ${e.message}", e) }
        }

        // 1. D'abord lastLocation rapide
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc?.let { if (it.accuracy < 50) sendData(it.latitude, it.longitude, it.accuracy, it.speed) }
            }
        } catch(_:Exception){}

        // 2. Requete haute precision type Google Maps
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000) // 1 minute
            .setMinUpdateIntervalMillis(15000) // mais ecoute toutes les 15s pour avoir le meilleur fix
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(true) // attend un fix precis comme Maps
            .setMaxUpdateDelayMillis(65000)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                Log.d("TrackerChild", "Loc raw: ${loc.latitude},${loc.longitude} acc=${loc.accuracy} speed=${loc.speed} provider=${loc.provider}")
                // Perfectionnement type Google Maps: on garde seulement les meilleurs
                sendData(loc.latitude, loc.longitude, loc.accuracy, loc.speed)
            }
        }
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch(e:SecurityException){ Log.e("TrackerChild","Perm manquante", e) }
        return START_STICKY
    }
}
