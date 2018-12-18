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

        Log.d("Alarm","Performing Daily Reset at ${Date()}")

        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
        val path = firestore.collection("Users").document(email)

        //Delete Coins solely used for map from Database:

        path.collection("Coins").document("Sent Coins Today").delete().addOnCompleteListener {
            Log.d("Alarm", "Sent Coins Today Deleted")
        }.addOnFailureListener {
            Log.d("Alarm", "Sent Coins Today NOT Deleted")
        }

        path.collection("Coins").document("Banked Coins Today").delete().addOnCompleteListener {
            Log.d("Alarm", "Banked Coins Today Deleted")
        }.addOnFailureListener {
            Log.d("Alarm", "Banked Coins Today NOT Deleted")
        }

        }

    }
