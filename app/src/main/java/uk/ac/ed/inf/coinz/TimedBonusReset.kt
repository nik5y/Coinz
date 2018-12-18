package uk.ac.ed.inf.coinz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class TimedBonusReset() : BroadcastReceiver() {

    val tag = "TimedBonusReset"

    override fun onReceive(p0: Context?, p1: Intent?) {

        val bonus = p1!!.getStringExtra("bonus")
        val timed = p1.getBooleanExtra("timed", false)


        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
        val path = firestore.collection("Users").document(email)



        path.collection("Bonuses").document(bonus).run {

            //if it is not a bonus with an exparation date, update the "updated" field. put activated to false for all

            if (!timed) {
                update("updated", todayYMD())
            }
            update("activated", false)
        }.addOnSuccessListener {
            Log.d(tag,"Resetting $bonus bonus to false")
        }.addOnFailureListener {
            Log.d(tag,"Failed to reset $bonus bonus!")
        }
        }
}