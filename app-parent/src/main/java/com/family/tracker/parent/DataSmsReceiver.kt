package com.family.tracker.parent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
class DataSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TrackerParent", "Data SMS recu intent=${intent.action}")
        try {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")
            for (p in pdus) {
                val msg = SmsMessage.createFromPdu(p as ByteArray, format)
                val data = msg.userData ?: continue
                val txt = String(data, Charsets.UTF_8)
                Log.d("TrackerParent", "Data SMS payload: $txt from ${msg.originatingAddress}")
                val parts = txt.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].toDoubleOrNull() ?: continue
                    val lon = parts[1].toDoubleOrNull() ?: continue
                    if (lat==0.0 && lon==0.0) continue
                    val b = Intent("TRACKER_UPDATE").putExtra("lat", lat).putExtra("lon", lon)
                    b.setPackage(context.packageName)
                    context.sendBroadcast(b)
                    Log.d("TrackerParent", "Broadcast TRACKER_UPDATE $lat,$lon")
                }
            }
        } catch(e:Exception){ Log.e("TrackerParent", "Erreur Data SMS", e) }
    }
}
