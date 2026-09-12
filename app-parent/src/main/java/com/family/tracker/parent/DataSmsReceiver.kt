package com.family.tracker.parent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
class DataSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        try {
            val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
            for (p in pdus) {
                val msg = SmsMessage.createFromPdu(p as ByteArray)
                val data = msg.userData ?: continue
                val str = String(data)
                Log.d("TrackerParent", "SMS Data reçu: $str")
                val parts = str.split(",")
                if (parts.size >= 2) {
                    val lat = parts[0].toDoubleOrNull() ?: continue
                    val lon = parts[1].toDoubleOrNull() ?: continue
                    context.sendBroadcast(Intent("TRACKER_UPDATE").putExtra("lat",lat).putExtra("lon",lon).setPackage(context.packageName))
                }
            }
        } catch(e: Exception){ Log.e("TrackerParent", "Erreur reception", e) }
    }
}
