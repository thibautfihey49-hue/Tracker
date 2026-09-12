package com.family.tracker.parent
import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.*
import android.widget.ImageView
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class FloatingMapService : Service() {
    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var mapView: MapView? = null
    private var marker: Marker? = null

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: return
            val lon = intent?.getDoubleExtra("lon", 0.0) ?: return
            if (lat == 0.0 && lon == 0.0) return
            mapView?.let {
                val pos = GeoPoint(lat, lon)
                marker?.position = pos
                it.controller.animateTo(pos)
                it.invalidate()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val ch = NotificationChannel("floating_map","Carte Flottante", NotificationManager.IMPORTANCE_LOW)
        (getSystemService(NotificationManager::class.java)).createNotificationChannel(ch)

        Configuration.getInstance().apply {
            userAgentValue = "TrackerParent/1.0"
            osmdroidBasePath = File(cacheDir, "osmdroid")
            osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notif = Notification.Builder(this, "floating_map")
            .setContentTitle("Carte flottante active")
            .setContentText("Touche pour agrandir - X pour fermer")
            .setSmallIcon(android.R.drawable.ic_dialog_map)
            .setOngoing(true).build()
        startForeground(2, notif)

        if (floatingView == null) createFloatingWindow()
        registerReceiver(receiver, IntentFilter("TRACKER_UPDATE"), RECEIVER_NOT_EXPORTED)
        return START_STICKY
    }

    private fun createFloatingWindow() {
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val inflater = getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        floatingView = inflater.inflate(R.layout.floating_map, null)
        mapView = floatingView!!.findViewById(R.id.floatingMap)
        mapView!!.setTileSource(TileSourceFactory.MAPNIK)
        mapView!!.setMultiTouchControls(true)
        mapView!!.controller.setZoom(17.0)
        mapView!!.controller.setCenter(GeoPoint(47.4736, -0.5517))
        marker = Marker(mapView).apply { position = GeoPoint(47.4736, -0.5517); title = "Enfant" }
        mapView!!.overlays.add(marker)

        val btnClose = floatingView!!.findViewById<ImageView>(R.id.btnClose)
        val btnExpand = floatingView!!.findViewById<ImageView>(R.id.btnExpand)
        btnClose.setOnClickListener { stopSelf() }
        btnExpand.setOnClickListener {
            val i = Intent(this, MapActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(i)
        }

        val params = WindowManager.LayoutParams(
            600, 800,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        )
        params.gravity = Gravity.TOP or Gravity.START
        params.x = 50; params.y = 200

        // Drag
        var initialX = 0; var initialY = 0; var initialTouchX = 0f; var initialTouchY = 0f
        val dragView = floatingView!!.findViewById<View>(R.id.dragHandle)
        dragView.setOnTouchListener { _, event ->
            when(event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x; initialY = params.y
                    initialTouchX = event.rawX; initialTouchY = event.rawY
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    params.x = initialX + (event.rawX - initialTouchX).toInt()
                    params.y = initialY + (event.rawY - initialTouchY).toInt()
                    windowManager?.updateViewLayout(floatingView, params)
                    true
                }
                else -> false
            }
        }

        windowManager?.addView(floatingView, params)
    }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(receiver) } catch(_:Exception){}
        try { mapView?.onPause() } catch(_:Exception){}
        if (floatingView != null) windowManager?.removeView(floatingView)
        floatingView = null
    }
}
