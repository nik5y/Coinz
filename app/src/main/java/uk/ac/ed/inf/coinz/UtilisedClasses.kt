package uk.ac.ed.inf.coinz

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

class User(var username:String, var pictureURL:String)

class Bonus(var activated: Boolean)

class Bank(var goldBalance:Double = 0.0)

class dailyUpdate() : Runnable {
    override fun run() {

        //Remove coins from todaysbanked, todayssent; Set bonus features to false.

        /*var currValMap : MutableMap<String,String> = mutableMapOf<String,String>()
        val coinMap : MutableMap<String,Any> = mutableMapOf()

        currValMap.put("kek","check")
        coinMap.put("boob", currValMap)*/

        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()

        //remove todaysbanked
        firestore.collection("Users").document(email).collection("Coins").document("Collected Coins").delete()

        //remove todayssent
        firestore.collection("Users").document(email).collection("Coins").document("Sent Coins Today").delete()

        //set bonus features to false
        firestore.collection("Users").document(email).collection("Bonuses").document("Coin Currency").update("activated", false)
        firestore.collection("Users").document(email).collection("Bonuses").document("Coin Value").update("activated", false)
        firestore.collection("Users").document(email).collection("Bonuses").document("Range+").update("activated", false)

    }
}

class Created(val created : Date = Date())

class CoinCounter(val count : Int = 0, val initialised: String = SimpleDateFormat("yyyy/MM/dd").format(Date()))

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


}

fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)
