package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Intent
import android.content.BroadcastReceiver
import android.content.Context
import android.util.Log
import java.util.*

@SuppressLint("LogNotTimber")
class DailyCoinDeleteOnReboot : BroadcastReceiver() {

    @SuppressLint("UnsafeProtectedBroadcastReceiver")
    override fun onReceive(context: Context?, intent: Intent) {

            val alarmIntent = Intent(context, DailyCoinDelete::class.java)
            val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT)
            val manager = context?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            manager.setRepeating(AlarmManager.RTC, Date().time + Timing().millisUntilTomorrowStart(),
                    24 * 60 * 60 * 1000, pendingIntent)
            Log.d("Alarm Reboot", "Alarm for daily coin deletion set after REBOOT")
    }
}

