package com.family.tracker.parent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
class DataSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("TrackerParent", "SMS recu action=${intent.action}")
        try {
            val bundle = intent.extras ?: return
            val pdus = bundle.get("pdus") as? Array<*> ?: return
            val format = bundle.getString("format")
            for (p in pdus) {
                val msg = SmsMessage.createFromPdu(p as ByteArray, format)
                val body = msg.messageBody ?: ""
                val userData = try { String(msg.userData ?: ByteArray(0)) } catch(_:Exception){ "" }
                Log.d("TrackerParent", "Body=$body UserData=$userData from=${msg.originatingAddress}")
                var lat: Double? = null
                var lon: Double? = null
                // Format 1: SMS normal TRACKER:lat,lon
                if (body.startsWith("TRACKER:")) {
                    val parts = body.removePrefix("TRACKER:").split(",")
                    lat = parts.getOrNull(0)?.toDoubleOrNull()
                    lon = parts.getOrNull(1)?.toDoubleOrNull()
                }
                // Format 2: Data SMS lat,lon,...
                if (lat == null && userData.contains(",")) {
                    val parts = userData.split(",")
                    lat = parts.getOrNull(0)?.toDoubleOrNull()
                    lon = parts.getOrNull(1)?.toDoubleOrNull()
                }
                // Format 3: body direct lat,lon (ancien)
                if (lat == null && body.contains(",") && !body.startsWith("TRACKER:TEST")) {
                    val parts = body.split(",")
                    if (parts.size>=2) {
                        lat = parts[0].toDoubleOrNull()
                        lon = parts[1].toDoubleOrNull()
                    }
                }
                if (lat != null && lon != null && lat!=0.0) {
                    Log.d("TrackerParent", "Position extraite $lat,$lon -> broadcast")
                    val i = Intent("TRACKER_UPDATE").putExtra("lat", lat).putExtra("lon", lon)
                    i.setPackage(context.packageName)
                    context.sendBroadcast(i)
                }
            }
        } catch(e: Exception){ Log.e("TrackerParent", "Erreur parse SMS", e) }
    }
}
