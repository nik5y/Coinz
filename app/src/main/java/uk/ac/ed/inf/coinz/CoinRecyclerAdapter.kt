package uk.ac.ed.inf.coinz

import android.content.Context
import android.content.SharedPreferences
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mapbox.mapboxsdk.annotations.IconFactory
import kotlinx.android.synthetic.main.coin_recycler_item.view.*
import kotlin.coroutines.experimental.coroutineContext

private lateinit var coinCurrency : String
private lateinit var coinValue : String
private lateinit var coinId : String
private val tag = "CoinRecyclerAdapter"

class CoinRecyclerAdapter(var context: Context, val items : ArrayList<CoinRecyclerViewClass>)
    : RecyclerView.Adapter<CoinRecyclerAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val coin : CoinRecyclerViewClass = items[position]
        coinCurrency = coin.currency
        coinValue = coin.value
        coinId = coin.id
        holder.coin_recycler_currency.text = coinCurrency
        holder.coin_recycler_value.text = coinValue
        holder.coin_recycler_image.setImageResource(coin.iconId)

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : CoinRecyclerAdapter.ViewHolder {

        val v = CoinRecyclerAdapter.ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.coin_recycler_item,parent,false))

        /*v.coin_recycler_item.setOnClickListener {
            println(v.coin_recycler_currency.text)
        }*/

        v.coin_recycler_bank_coin.setOnClickListener{
            addCoinToBank()
        }


        return v
    }


    class ViewHolder (view : View) : RecyclerView.ViewHolder(view) {

        val coin_recycler_item = view.coin_recycler_item as LinearLayout
        val coin_recycler_bank_coin = view.coin_recycler_bank_coin as ImageView
        val coin_recycler_image = view.coin_recycler_image as ImageView
        val coin_recycler_currency = view.coin_recycler_currency as TextView
        val coin_recycler_value = view.coin_recycler_value as TextView
    }

    fun addCoinToBank() {

        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
        val bankReference = firestore.collection("Users").document(email).collection("Account Information").document("Gold Balance")

        //todo implement coin addition to bank and recycler view update and moving coins to different folders.

        bankReference.get().addOnSuccessListener {
            var goldBalance = it.data!!.get("goldBalance") as Double


            val sharedPreferences = context.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
            val rate = sharedPreferences.getString(coinCurrency,"default")

            goldBalance += coinValue.toDouble() * rate.toDouble()

            bankReference.set(Bank(goldBalance))

            removeCoinFromWallet(firestore, email)

        }


    }

    fun removeCoinFromWallet(firestore: FirebaseFirestore, email : String) {

        //todo delete coins from collected and add a union to the coins to remove from map
        //todo figure out whats wrong with the clicks.
        val walletReference = firestore.collection("Users").document(email).collection("Coins")

        walletReference.document("Collected Coins").get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        val coinMap: MutableMap<String, Any> = mutableMapOf<String, Any>()

                        coinMap.put(coinId, "Banked")

                        walletReference.document("Banked Coins Today").set(coinMap, SetOptions.merge())

                        walletReference.document("Collected Coins").update(coinId, FieldValue.delete())

                        Log.d(tag, "Deleting $coinValue, $coinCurrency")

                    } else {
                        Log.d(tag, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.d(tag, "get failed with ", exception)
                }





    }


}

