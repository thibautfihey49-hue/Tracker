package com.family.tracker.child
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat

class ChildMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,80,40,40) }
        val title = TextView(this).apply { text="Tracker Enfant - Consentement"; textSize=20f }
        val input = EditText(this).apply { hint="Numero parent +33612345678" }
        val check = CheckBox(this).apply { text="J'autorise le partage de ma position" }
        val btn = Button(this).apply { text="Activer le partage" }
        val btnTest = Button(this).apply { text="TEST envoi SMS" }
        val status = TextView(this)
        layout.addView(title); layout.addView(input); layout.addView(check); layout.addView(btn); layout.addView(btnTest); layout.addView(status)
        setContentView(layout)
        val prefs = getSharedPreferences("tracker", Context.MODE_PRIVATE)
        input.setText(prefs.getString("parent",""))

        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, perms, 1)
        }

        btn.setOnClickListener {
            if (!check.isChecked) { Toast.makeText(this,"Coche le consentement", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val num = input.text.toString().trim()
            if (num.length < 10) { Toast.makeText(this,"Numero invalide", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            prefs.edit().putString("parent", num).putBoolean("consent", true).apply()
            startForegroundService(Intent(this, LocationService::class.java))
            status.text="✅ Partage actif vers $num - regarde logcat TrackerChild"
            Toast.makeText(this,"Demarre", Toast.LENGTH_SHORT).show()
        }
        btnTest.setOnClickListener {
            val num = input.text.toString().trim()
            if (num.isEmpty()) { Toast.makeText(this,"Mets un numero", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            try {
                val sms = android.telephony.SmsManager.getDefault()
                sms.sendTextMessage(num, null, "TRACKER:TEST 47.47,-0.55 Angers", null, null)
                status.text="SMS TEST envoye a $num - verifie si recu"
                Toast.makeText(this,"SMS TEST envoye", Toast.LENGTH_LONG).show()
            } catch(e:Exception){ status.text="Erreur envoi: ${e.message}"; Toast.makeText(this,"Erreur: ${e.message}", Toast.LENGTH_LONG).show() }
        }
    }
}
