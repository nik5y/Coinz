package uk.ac.ed.inf.coinz

import android.os.Bundle
import android.app.Activity
import android.app.AlarmManager
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Intent

import kotlinx.android.synthetic.main.activity_messenger.*
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MessengerActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_messenger)




        /*val sc : ScheduledExecutorService =






        }



            fun deleteCoins() {

                //create new calendar instance

                val midnightCalendar  = Calendar.getInstance();

                //set the time to midnight tonight

                midnightCalendar.set(Calendar.HOUR_OF_DAY, 0);

                midnightCalendar.set(Calendar.MINUTE, 0);

                midnightCalendar.set(Calendar.SECOND, 0);

                val am : AlarmManager = this.getSystemService(android.content.Context.ALARM_SERVICE) as AlarmManager

                //create a pending intent to be called at midnight
                val midnightPI : PendingIntent = PendingIntent.getService(this, 0,  Intent("net.accella.sheduleexample.SilenceBroadcastReceiver"), PendingIntent.FLAG_UPDATE_CURRENT);

                //schedule time for pending intent, and set the interval to day so that this event will repeat at the selected time every day

                am.setRepeating(AlarmManager.RTC, midnightCalendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, midnightPI);
        }*/
    }


    // Task to be executed repeatedly, defined as a Runnable.
          // Initial delay, before first execution. Use this to get close to first moment of tomorrow in UTC per our code above.
      // Amount of time in each interval, between subsequent executions of our Runnable.
             // Unit of time intended by the numbers in previous two arguments.




}


