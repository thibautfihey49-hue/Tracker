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
                Log.d("TrackerParent", "Data 1min payload: $txt")
                val parts = txt.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].toDoubleOrNull() ?: continue
                    val lon = parts[1].toDoubleOrNull() ?: continue
                    val acc = parts.getOrNull(2)?.toFloatOrNull() ?: 10f
                    val speed = parts.getOrNull(3)?.toFloatOrNull() ?: 0f
                    val b = Intent("TRACKER_UPDATE").putExtra("lat", lat).putExtra("lon", lon).putExtra("acc", acc).putExtra("speed", speed)
                    b.setPackage(context.packageName)
                    context.sendBroadcast(b)
                }
            }
        } catch(e:Exception){ Log.e("TrackerParent", "Erreur", e) }
    }
}
