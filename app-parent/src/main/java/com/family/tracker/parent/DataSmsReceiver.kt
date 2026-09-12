package com.family.tracker.parent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
class DataSmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pdus = intent.extras?.get("pdus") as? Array<*> ?: return
        for (p in pdus) {
            val msg = SmsMessage.createFromPdu(p as ByteArray)
            try { val parts = String(msg.userData).split(","); val lat=parts[0].toDouble(); val lon=parts[1].toDouble()
                context.sendBroadcast(Intent("TRACKER_UPDATE").putExtra("lat",lat).putExtra("lon",lon))
            } catch(e: Exception){}
        }
    }
}
