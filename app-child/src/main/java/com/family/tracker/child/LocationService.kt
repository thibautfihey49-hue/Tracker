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
        val notif = Notification.Builder(this, "tracker_channel").setContentTitle("Partage position (Data SMS)").setContentText("Invisible - port 8901").setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build()
        startForeground(1, notif)
        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        Log.d("TrackerChild", "Data SMS Service vers $parentNumber")
        if (parentNumber.isEmpty()) { stopSelf(); return START_NOT_STICKY }
        fun sendData(lat: Double, lon: Double) {
            try {
                val payload = "$lat,$lon,${System.currentTimeMillis()}".toByteArray(Charsets.UTF_8)
                SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                Log.d("TrackerChild", "Data SMS envoye port 8901: $lat,$lon")
            } catch(e:Exception){ Log.e("TrackerChild", "Data SMS fail ${e.message}", e) }
        }
        try { fused.lastLocation.addOnSuccessListener { loc -> loc?.let { sendData(it.latitude, it.longitude) } } } catch(_:Exception){}
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000).setMinUpdateIntervalMillis(10000).build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                val loc = res.lastLocation ?: return
                sendData(loc.latitude, loc.longitude)
            }
        }
        try { fused.requestLocationUpdates(request, callback, mainLooper) } catch(e:SecurityException){}
        return START_STICKY
    }
}
