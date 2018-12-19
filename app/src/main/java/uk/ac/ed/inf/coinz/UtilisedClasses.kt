@file:Suppress("UNUSED")

package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
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

class Level(val level : Int = 1)

class CoinRecyclerViewClass(val id : String, val currency : String,
                                   val value : String, val iconId : Int, val sentBy:String, val collectedBy : String)

class Timing {

    fun millisUntilTomorrowStart(): Long {

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val tomorrowStart = OffsetDateTime.of(tomorrow, LocalTime.MIN, ZoneOffset.UTC)
        val d = Duration.between(now, tomorrowStart)

        return d.toMillis()
    }

    fun timeOfUpcomingMidnight() : Long {
        val calendar: Calendar = Calendar.getInstance().apply {
           timeInMillis = System.currentTimeMillis()
           set(Calendar.HOUR_OF_DAY, 0)
           set(Calendar.MINUTE, 0)
           set(Calendar.SECOND, 0)
           set(Calendar.MILLISECOND, 0)
           add(Calendar.DAY_OF_MONTH, 1)
       }
        return calendar.timeInMillis
    }
}

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)!!

@SuppressLint("SimpleDateFormat")
fun todayYMD() : String {
    return SimpleDateFormat("yyyy/MM/dd").format(Date())
}

class LevelingSystem {

    fun levelPrice(currentLevel : Int): Double{

        return when (currentLevel) {
            1 -> 500.0
            2 -> 1000.0
            3 -> 1500.0
            4 -> 2000.0
            5 -> 2500.0
            else -> 1010101.0
        }
    }

    fun allowedCoinCollect(currentLevel: Int): Int {

        return when (currentLevel) {
            1 -> 5
            2 -> 10
            3 -> 15
            4 -> 25
            5 -> 30
            else -> 50
        }

    }

}





