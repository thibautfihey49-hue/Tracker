package com.family.tracker.parent
import android.app.*
import android.content.*
import android.graphics.PixelFormat
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.*
import android.widget.ImageView
import android.widget.TextView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
class FloatingMapService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var mapView: MapView? = null
    private var marker: Marker? = null
    private var accuracyCircle: Polygon? = null
    private var statusMini: TextView? = null
    private var handler: Handler? = null // FIX: pas init dans <init>

    private fun updateMap(lat: Double, lon: Double, acc: Float) {
        handler?.post {
            try {
                val pos = GeoPoint(lat, lon)
                marker?.position = pos
                mapView?.let { mv ->
                    accuracyCircle?.let { mv.overlays.remove(it) }
                    accuracyCircle = Polygon().apply {
                        points = Polygon.pointsAsCircle(pos, acc.toDouble())
                        fillColor = 0x2200B0FF; strokeColor = 0x4400B0FF.toInt(); strokeWidth = 1f
                    }
                    mv.overlays.add(accuracyCircle)
                    marker?.let { mv.overlays.remove(it); mv.overlays.add(it) }
                    mv.controller.animateTo(pos)
                    mv.invalidate()
                }
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)
                statusMini?.text = "±${acc.toInt()}m ${sdf.format(Date())}"
            } catch(_:Exception){}
        }
    }
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: return
            val lon = intent?.getDoubleExtra("lon", 0.0) ?: return
            val acc = intent?.getFloatExtra("acc", 10f) ?: 10f
            if (lat==0.0) return
            updateMap(lat, lon, acc)
        }
    }
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onCreate() {
        super.onCreate()
        handler = Handler(Looper.getMainLooper()) // FIX ici
        val ch = NotificationChannel("floating_map","Carte Flottante", NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(ch)
        Configuration.getInstance().apply { userAgentValue = "TrackerParent/1.0"; osmdroidBasePath = File(cacheDir, "osmdroid"); osmdroidTileCache = File(cacheDir, "osmdroid/tiles") }
    }
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "TRACKER_UPDATE" && intent.hasExtra("lat")) {
            val lat = intent.getDoubleExtra("lat", 0.0)
            val lon = intent.getDoubleExtra("lon", 0.0)
            val acc = intent.getFloatExtra("acc", 10f)
            if (lat!=0.0) updateMap(lat, lon, acc)
        }
        val notif = Notification.Builder(this, "floating_map").setContentTitle("Mini-carte 1min").setSmallIcon(android.R.drawable.ic_dialog_map).setOngoing(true).build()
        startForeground(2, notif)
        if (floatingView == null) createFloatingWindow()
        try { registerReceiver(receiver, IntentFilter("TRACKER_UPDATE"), RECEIVER_NOT_EXPORTED) } catch(_:Exception){ try { registerReceiver(receiver, IntentFilter("TRACKER_UPDATE")) } catch(_:Exception){} }
        val prefs = getSharedPreferences("tracker_parent", Context.MODE_PRIVATE)
        val lastLat = prefs.getString("last_lat","")?.toDoubleOrNull()
        val lastLon = prefs.getString("last_lon","")?.toDoubleOrNull()
        val lastAcc = prefs.getFloat("last_acc", 10f)
        if (lastLat!=null && lastLon!=null) updateMap(lastLat, lastLon, lastAcc)
        return START_STICKY
    }
    private fun createFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_map, null)
        mapView = floatingView!!.findViewById(R.id.floatingMap)
        statusMini = floatingView!!.findViewById(R.id.miniStatus)
        mapView!!.setTileSource(TileSourceFactory.MAPNIK)
        mapView!!.setMultiTouchControls(true)
        mapView!!.controller.setZoom(18.0)
        mapView!!.controller.setCenter(GeoPoint(47.4736, -0.5517))
        marker = Marker(mapView).apply { position = GeoPoint(47.4736, -0.5517); title = "Enfant" }
        mapView!!.overlays.add(marker)
        floatingView!!.findViewById<ImageView>(R.id.btnClose).setOnClickListener { stopSelf() }
        floatingView!!.findViewById<ImageView>(R.id.btnExpand).setOnClickListener { startActivity(Intent(this, MapActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
        val params = WindowManager.LayoutParams(420, 420, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL, PixelFormat.TRANSLUCENT)
        params.gravity = Gravity.TOP or Gravity.START; params.x = 20; params.y = 150
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        floatingView!!.findViewById<View>(R.id.dragHandle).setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> { initialX = params.x; initialY = params.y; initialTouchX = event.rawX; initialTouchY = event.rawY; true }
                MotionEvent.ACTION_MOVE -> { params.x = initialX + (event.rawX - initialTouchX).toInt(); params.y = initialY + (event.rawY - initialTouchY).toInt(); windowManager?.updateViewLayout(floatingView, params); true }
                else -> false
            }
        }
        windowManager?.addView(floatingView, params)
    }
    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(receiver) } catch(_:Exception){}
        try { mapView?.onPause() } catch(_:Exception){}
        if (floatingView != null) try { windowManager?.removeView(floatingView) } catch(_:Exception){}
        floatingView = null
    }
}
