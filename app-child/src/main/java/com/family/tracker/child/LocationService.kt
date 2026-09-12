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
    private var lastLat = 0.0
    private var lastLon = 0.0
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        val ch = NotificationChannel("tracker_channel","Tracker Enfant", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, "tracker_channel").setContentTitle("Partage 1min FORCE").setContentText("Envoi meme si faible").setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build()
        startForeground(1, notif)
        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        Log.d("TrackerChild", "Service FORCE 1min vers $parentNumber")
        if (parentNumber.isEmpty()) { stopSelf(); return START_NOT_STICKY }

        fun sendForce(lat: Double, lon: Double, acc: Float, speed: Float, source: String) {
            val now = System.currentTimeMillis()
            // anti-spam mais on force quand meme si meme position bouge pas - on envoie quand meme
            if (now - lastSentTime < 55000 && lat==lastLat && lon==lastLon) {
                Log.d("TrackerChild", "Skip duplicate meme position")
                // return // commente pour debug
            }
            lastSentTime = now
            lastLat = lat; lastLon = lon
            try {
                val payload = "$lat,$lon,$acc,$speed,$now".toByteArray(Charsets.UTF_8)
                SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                Log.d("TrackerChild", "✅ Data SMS FORCE 1min [$source] $lat,$lon acc=${acc}m")
            } catch(e:Exception){ Log.e("TrackerChild", "Fail ${e.message}", e) }
        }

        // Envoi immediat lastLocation meme si pourrie
        try {
            fused.lastLocation.addOnSuccessListener { loc ->
                loc?.let { sendForce(it.latitude, it.longitude, it.accuracy, it.speed, "lastLocation") }
            }
        } catch(_:Exception){}

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(10000)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false) // FALSE pour forcer meme en interieur
            .setMaxUpdateDelayMillis(60000)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                Log.d("TrackerChild", "Loc recue: ${loc.latitude},${loc.longitude} acc=${loc.accuracy} time=${loc.time}")
                sendForce(loc.latitude, loc.longitude, loc.accuracy, loc.speed, "live")
            }
        }
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch(e:SecurityException){ Log.e("TrackerChild","Perm", e) }
        return START_STICKY
    }
}
