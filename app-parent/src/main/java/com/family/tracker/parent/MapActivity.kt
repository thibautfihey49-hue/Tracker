package com.family.tracker.parent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
class MapActivity : AppCompatActivity() {
    private lateinit var map: MapView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Configuration.getInstance().userAgentValue = "TrackerParent/1.0"
        map = MapView(this); setContentView(map)
        map.setTileSource(TileSourceFactory.MAPNIK); map.setMultiTouchControls(true)
        map.controller.setZoom(16.0); map.controller.setCenter(GeoPoint(47.4736, -0.5517))
        val marker = Marker(map); marker.position = GeoPoint(47.4736, -0.5517); marker.title="Enfant"; map.overlays.add(marker)
    }
}
