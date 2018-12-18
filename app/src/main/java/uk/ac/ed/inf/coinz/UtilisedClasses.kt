package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.sql.Timestamp
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class User(var username:String, var pictureURL:String)

class Bonus(val activated: Boolean, val updated: String = todayYMD())

class BonusTimed(val activated: Boolean, val expires : Date =  Date())

class Bank(val goldBalance:Double = 0.0)

class Created(val created : Date = Date())

class CoinCounter(val count : Int = 0, val initialised: String = todayYMD())

class Timing() {

    fun millisUntilTomorrowStart(): Long {

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val tomorrowStart = OffsetDateTime.of(tomorrow, LocalTime.MIN, ZoneOffset.UTC)
        val d = Duration.between(now, tomorrowStart)
        val millisUntilTomorrowStart = d.toMillis()

        return millisUntilTomorrowStart
    }

    fun timeOfUpcomingMidnight() : Long {
        val calendar: Calendar = Calendar.getInstance().apply {
           timeInMillis = System.currentTimeMillis()
           set(Calendar.HOUR_OF_DAY, 0)
           set(Calendar.MINUTE, 0)
           set(Calendar.SECOND, 0)
           set(Calendar.MILLISECOND, 0)
           //add(Calendar.DAY_OF_MONTH, 1)
       }
        return calendar.timeInMillis
    }
}

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

@SuppressLint("SimpleDateFormat")
fun todayYMD() : String {
    return SimpleDateFormat("yyyy/MM/dd").format(Date())
}
