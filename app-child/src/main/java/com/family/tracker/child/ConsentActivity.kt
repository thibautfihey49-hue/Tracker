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
        try {
            setContentView(R.layout.activity_consent)
            val input = findViewById<EditText>(R.id.inputParent)
            val check = findViewById<CheckBox>(R.id.checkConsent)
            val btn = findViewById<Button>(R.id.btnActivate)
            val status = findViewById<TextView>(R.id.status)
            input.setText(getSharedPreferences("tracker", MODE_PRIVATE).getString("parent",""))

            btn.setOnClickListener {
                if (!check.isChecked) { Toast.makeText(this, "Coche le consentement", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                if (input.text.length < 10) { Toast.makeText(this, "Numéro invalide", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
                getSharedPreferences("tracker", MODE_PRIVATE).edit().putString("parent", input.text.toString()).putBoolean("consent", true).apply()
                status.text = "Demande de permissions..."
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Erreur: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    override fun onRequestPermissionsResult(c: Int, p: Array<out String>, r: IntArray) {
        super.onRequestPermissionsResult(c, p, r)
        if (r.all { it == PackageManager.PERMISSION_GRANTED }) {
            startForegroundService(Intent(this, LocationService::class.java))
            Toast.makeText(this, "Partage activé - 1 SMS Data / min", Toast.LENGTH_LONG).show()
            findViewById<TextView>(R.id.status).text = "Actif"
        } else {
            Toast.makeText(this, "Permissions refusées", Toast.LENGTH_LONG).show()
        }
    }
}
