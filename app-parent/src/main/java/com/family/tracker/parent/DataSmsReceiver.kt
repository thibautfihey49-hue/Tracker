package com.family.tracker.parent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
class DataSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")
            for (p in pdus) {
                val msg = SmsMessage.createFromPdu(p as ByteArray, format)
                val data = msg.userData ?: continue
                val txt = String(data, Charsets.UTF_8)
                val parts = txt.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].toDoubleOrNull() ?: continue
                    val lon = parts[1].toDoubleOrNull() ?: continue
                    val acc = parts.getOrNull(2)?.toFloatOrNull() ?: 10f
                    val speed = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
                    Log.d("TrackerParent", "Data recu $lat,$lon acc $acc")
                    // 1. Sauve en prefs pour que la flottante relise meme si elle a rate le broadcast
                    val prefs = context.getSharedPreferences("tracker_parent", Context.MODE_PRIVATE)
                    prefs.edit().putString("last_lat", lat.toString()).putString("last_lon", lon.toString()).putFloat("last_acc", acc).putLong("last_time", System.currentTimeMillis()).apply()
                    // 2. Broadcast pour MapActivity + FloatingService
                    val b = Intent("TRACKER_UPDATE").putExtra("lat", lat).putExtra("lon", lon).putExtra("acc", acc).putExtra("speed", speed)
                    b.setPackage(context.packageName)
                    context.sendBroadcast(b)
                    // 3. Broadcast explicite pour le service (Android 14 bloque parfois le NOT_EXPORTED)
                    val b2 = Intent(context, FloatingMapService::class.java).setAction("TRACKER_UPDATE").putExtra("lat", lat).putExtra("lon", lon).putExtra("acc", acc)
                    context.startService(b2)
                }
            }
        } catch(e:Exception){ Log.e("TrackerParent","Err",e) }
    }
}
