@file:SuppressLint("LogNotTimber")

package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore


class TimedBonusReset : BroadcastReceiver() {

    private val tag = "TimedBonusReset"

    override fun onReceive(p0: Context?, p1: Intent?) {

        val bonus = p1!!.getStringExtra("bonus")


        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
        val path = firestore.collection("Users").document(email)



        path.collection("Bonuses").document(bonus).run {
            update("activated", false)
        }.addOnSuccessListener {
            Log.d(tag,"Resetting $bonus bonus to false")
        }.addOnFailureListener {
            Log.d(tag,"Failed to reset $bonus bonus!")
        }
        }
}