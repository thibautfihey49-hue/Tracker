package com.family.tracker.child
import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
class ChildMainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val layout = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(40,80,40,40) }
        val title = TextView(this).apply { text="Tracker Enfant - Data SMS"; textSize=20f }
        val input = EditText(this).apply { hint="Numero parent +336..." }
        val check = CheckBox(this).apply { text="J'autorise le partage (Data SMS invisible port 8901)" }
        val btn = Button(this).apply { text="Activer partage Data SMS" }
        val btnTest = Button(this).apply { text="TEST Data SMS" }
        val status = TextView(this)
        layout.addView(title); layout.addView(input); layout.addView(check); layout.addView(btn); layout.addView(btnTest); layout.addView(status)
        setContentView(layout)
        val prefs = getSharedPreferences("tracker", Context.MODE_PRIVATE)
        input.setText(prefs.getString("parent",""))
        val perms = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.SEND_SMS)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS)!=PackageManager.PERMISSION_GRANTED) ActivityCompat.requestPermissions(this, perms, 1)
        btn.setOnClickListener {
            if (!check.isChecked) { Toast.makeText(this,"Coche consentement", Toast.LENGTH_SHORT).show(); return@setOnClickListener }
            val num = input.text.toString().trim()
            if (num.length < 10) { Toast.makeText(this,"Numero invalide", Toast.LENGTH_LONG).show(); return@setOnClickListener }
            prefs.edit().putString("parent", num).putBoolean("consent", true).apply()
            startForegroundService(Intent(this, LocationService::class.java))
            status.text="✅ Data SMS actif vers $num (port 8901 invisible)"
        }
        btnTest.setOnClickListener {
            val num = input.text.toString().trim()
            try {
                val payload = "47.4736,-0.5517,${System.currentTimeMillis()}".toByteArray()
                SmsManager.getDefault().sendDataMessage(num, null, 8901.toShort(), payload, null, null)
                status.text="Data SMS TEST envoye a $num port 8901"
                Toast.makeText(this,"Data SMS TEST envoye (invisible)", Toast.LENGTH_LONG).show()
            } catch(e:Exception){ status.text="Erreur: ${e.message}" }
        }
    }
}
