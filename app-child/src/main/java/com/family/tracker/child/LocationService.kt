package com.family.tracker.child
import android.app.*
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.IBinder
import android.os.Looper
import android.telephony.SmsManager
import com.google.android.gms.location.*
import android.util.Log
import kotlinx.coroutines.*
class LocationService : Service() {
    private lateinit var fused: FusedLocationProviderClient
    private var lastSent = 0L
    private val scope = CoroutineScope(Dispatchers.Default)
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        fused = LocationServices.getFusedLocationProviderClient(this)
        val ch = NotificationChannel("tracker_channel","Tracker Precis +-3m", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, "tracker_channel").setContentTitle("GPS Precision max").setContentText("3 fixes pour +-3m - 1min").setSmallIcon(android.R.drawable.ic_menu_mylocation).setOngoing(true).build()
        startForeground(1, notif)
        val parentNumber = getSharedPreferences("tracker", Context.MODE_PRIVATE).getString("parent","") ?: ""
        if (parentNumber.isEmpty()) { stopSelf(); return START_NOT_STICKY }

        fun send(lat: Double, lon: Double, acc: Float, speed: Float) {
            val now = System.currentTimeMillis()
            if (now - lastSent < 55000) return
            lastSent = now
            try {
                val payload = "$lat,$lon,$acc,$speed,$now".toByteArray(Charsets.UTF_8)
                SmsManager.getDefault().sendDataMessage(parentNumber, null, 8901.toShort(), payload, null, null)
                Log.d("TrackerChild", "SEND PRECIS $lat,$lon acc=${acc}m")
            } catch(e:Exception){}
        }

        // Methode Google Maps: prend 3 fixes en 20s et garde le meilleur
        fun requestBestFix() {
            scope.launch {
                val fixes = mutableListOf<android.location.Location>()
                repeat(3) {
                    try {
                        // getCurrentLocation = demande un fix frais GPS pur comme Maps
                        val loc = withContext(Dispatchers.Main) {
                            var result: android.location.Location? = null
                            val job = Job()
                            fused.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                                .addOnSuccessListener { l -> result = l; job.complete() }
                                .addOnFailureListener { job.complete() }
                            // timeout 10s
                            withTimeoutOrNull(10000) { job.join() }
                            result
                        }
                        loc?.let {
                            fixes.add(it)
                            Log.d("TrackerChild", "Fix ${it.accuracy}m: ${it.latitude},${it.longitude}")
                        }
                    } catch(_:Exception){}
                    delay(4000) // attend 4s entre chaque fix
                }
                // garde le meilleur (plus petite accuracy)
                val best = fixes.minByOrNull { it.accuracy }
                best?.let {
                    // si meme avec 3 fixes on a >20m, on force quand meme mais on log
                    if (it.accuracy > 20) Log.d("TrackerChild", "Precision moyenne ${it.accuracy}m - conseille sortir dehors")
                    send(it.latitude, it.longitude, it.accuracy, it.speed)
                }
            }
        }

        // Lance tout de suite + toutes les 60s
        requestBestFix()
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 60000)
            .setMinUpdateIntervalMillis(20000)
            .setWaitForAccurateLocation(true) // attend GPS vrai
            .setMinUpdateDistanceMeters(0f)
            .build()
        val callback = object : LocationCallback() {
            override fun onLocationResult(res: LocationResult) {
                requestBestFix() // chaque trigger 1min -> on relance les 3 fixes
            }
        }
        try { fused.requestLocationUpdates(request, callback, Looper.getMainLooper()) } catch(_:Exception){}

        // Fallback GPS pur si Play Services rate
        try {
            val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Log.d("TrackerChild", "GPS_PROVIDER active")
            }
        } catch(_:Exception){}

        return START_STICKY
    }
    override fun onDestroy() { super.onDestroy(); scope.cancel() }
}
