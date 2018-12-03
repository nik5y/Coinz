package uk.ac.ed.inf.coinz

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

class DailyCoinDelete : BroadcastReceiver() {

    override fun onReceive(p0: Context?, p1: Intent?) {

        Log.d("Alarm","Deleting Coins at ${Date()}")

        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()

        //remove todaysbanked
        firestore.collection("Users").document(email).collection("Coins").document("Collected Coins").delete()



    }
}