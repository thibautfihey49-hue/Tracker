package com.family.tracker.child
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
class ConsentActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation=LinearLayout.VERTICAL; setPadding(50,100,50,50) }
        val input = EditText(this).apply { hint="+33XXXXXXXXX" }
        val check = CheckBox(this).apply { text="Je consens a partager ma position" }
        val btn = Button(this).apply { text="ACTIVER" }
        btn.setOnClickListener {
            if(!check.isChecked) return@setOnClickListener
            getSharedPreferences("tracker", MODE_PRIVATE).edit().putString("parent", input.text.toString()).apply()
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.POST_NOTIFICATIONS),1)
        }
        layout.addView(input); layout.addView(check); layout.addView(btn); setContentView(layout)
    }
    override fun onRequestPermissionsResult(c:Int,p:Array<out String>,r:IntArray){
        super.onRequestPermissionsResult(c,p,r)
        if(r.all{it==PackageManager.PERMISSION_GRANTED}){ startForegroundService(Intent(this, LocationService::class.java)) }
    }
}
