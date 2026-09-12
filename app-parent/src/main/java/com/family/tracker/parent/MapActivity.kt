package com.family.tracker.parent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import java.io.File

class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        try {
            // Config OSMDroid obligatoire sinon crash blanc
            val osmConf = Configuration.getInstance()
            osmConf.userAgentValue = "TrackerParent/1.0"
            osmConf.osmdroidBasePath = File(cacheDir, "osmdroid")
            osmConf.osmdroidTileCache = File(cacheDir, "osmdroid/tiles")

            setContentView(R.layout.activity_map)
            map = findViewById(R.id.map)
            map.setTileSource(TileSourceFactory.MAPNIK)
            map.setMultiTouchControls(true)
            map.controller.setZoom(16.0)
            map.controller.setCenter(GeoPoint(47.4736, -0.5517))

            val marker = Marker(map)
            marker.position = GeoPoint(47.4736, -0.5517)
            marker.title = "Enfant - Angers"
            map.overlays.add(marker)
            map.invalidate()
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur Map: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }
    override fun onResume() { super.onResume(); try { map.onResume() } catch(_:Exception){} }
    override fun onPause() { super.onPause(); try { map.onPause() } catch(_:Exception){} }
}
