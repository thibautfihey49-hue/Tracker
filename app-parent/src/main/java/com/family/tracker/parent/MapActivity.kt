package com.family.tracker.parent
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.floatingactionbutton.FloatingActionButton
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    private var marker: Marker? = null
    private var accuracyCircle: Polygon? = null
    private lateinit var statusText: TextView
    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val lat = intent?.getDoubleExtra("lat", 0.0) ?: return
            val lon = intent?.getDoubleExtra("lon", 0.0) ?: return
            val acc = intent?.getFloatExtra("acc", 10f) ?: 10f
            if (lat==0.0 && lon==0.0) return
            runOnUiThread {
                val pos = GeoPoint(lat, lon)
                marker?.position = pos
                // cercle de precision comme Google Maps
                accuracyCircle?.let { map.overlays.remove(it) }
                accuracyCircle = Polygon().apply {
                    points = Polygon.pointsAsCircle(pos, acc.toDouble())
                    fillColor = 0x2200B0FF
                    strokeColor = 0x4400B0FF.toInt()
                    strokeWidth = 2f
                }
                map.overlays.add(accuracyCircle)
                // remet marker au dessus
                marker?.let { map.overlays.remove(it); map.overlays.add(it) }
                map.controller.animateTo(pos)
                map.controller.setZoom(18.0) // zoom Google Maps
                map.invalidate()
                val sdf = SimpleDateFormat("HH:mm:ss", Locale.FRANCE)
                statusText.text = "📍 ${String.format("%.6f", lat)}, ${String.format("%.6f", lon)} ±${acc.toInt()}m - ${sdf.format(Date())} (1 min)"
            }
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS, Manifest.permission.ACCESS_FINE_LOCATION), 1)
        }
        val osmConf = Configuration.getInstance()
        osmConf.userAgentValue = "TrackerParent/1.0"
        osmConf.osmdroidBasePath = File(cacheDir, "osmdroid")
        osmConf.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")
        setContentView(R.layout.activity_map)
        map = findViewById(R.id.map)
        statusText = findViewById(R.id.status)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.isTilesScaledToDpi = true
        map.controller.setZoom(17.0)
        map.controller.setCenter(GeoPoint(47.4736, -0.5517))
        marker = Marker(map).apply { position = GeoPoint(47.4736, -0.5517); title = "Enfant"; setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM) }
        map.overlays.add(marker)
        statusText.text = "En attente - précision Google Maps - 1 min"
        findViewById<FloatingActionButton>(R.id.fabFloat).setOnClickListener {
            if (!Settings.canDrawOverlays(this)) {
                startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName")))
            } else {
                startForegroundService(Intent(this, FloatingMapService::class.java))
            }
        }
    }
    override fun onResume() { super.onResume(); try { map.onResume() } catch(_:Exception){}; registerReceiver(receiver, IntentFilter("TRACKER_UPDATE"), RECEIVER_NOT_EXPORTED) }
    override fun onPause() { super.onPause(); try { map.onPause() } catch(_:Exception){}; try { unregisterReceiver(receiver) } catch(_:Exception){} }
}
