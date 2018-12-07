package uk.ac.ed.inf.coinz

import android.app.Dialog
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AlertDialog
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
import kotlinx.android.synthetic.main.coin_recycler_dialog.*
import kotlinx.android.synthetic.main.coin_recycler_item.view.*
import kotlin.coroutines.experimental.coroutineContext

private val tag = "CoinRecyclerAdapter"


class CoinRecyclerAdapter(var context: Context, val items : ArrayList<CoinRecyclerViewClass>)
    : RecyclerView.Adapter<CoinRecyclerAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val coin : CoinRecyclerViewClass = items[position]
        holder.coin_recycler_currency.text = coin.currency
        //rounds down
        holder.coin_recycler_value.text =coin.value.subSequence(0,5)
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



        //

        v.coin_recycler_send_coin.setOnClickListener {

            val coinId = items.get(v.adapterPosition).id
            val coinCurrency = items.get(v.adapterPosition).currency
            val coinValue = items.get(v.adapterPosition).value
            val iconId = items.get(v.adapterPosition).iconId

            //SENDING COINS

            //create Dialog for sending coins

            val dialog = Dialog(context)
            dialog.setContentView(R.layout.coin_recycler_dialog)
            dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            dialog.coin_recycler_dialog_coinValue.setText(coinValue)
            dialog.coin_recycler_dialog_coin_currency.setText(coinCurrency)
            dialog.coin_recycler_dialog_image.setImageResource(iconId)

            dialog.show()

            dialog.coin_recycler_dialog_send_coin.setOnClickListener {
                //Toast.makeText(context,"whey",Toast.LENGTH_SHORT).show()
                sendCoinToUser(v.adapterPosition, coinId, coinCurrency, coinValue, dialog)

            }
        }

        //deleting coins

        //todo remove coin from recycler view once banked
        v.coin_recycler_bank_coin.setOnClickListener{

            val coinId = items.get(v.adapterPosition).id
            val coinCurrency = items.get(v.adapterPosition).currency
            val coinValue = items.get(v.adapterPosition).value
            val iconId = items.get(v.adapterPosition).iconId

            //CONVERTING COINS

            addCoinToBank(coinId,coinCurrency,coinValue)

            items.removeAt(v.adapterPosition)

            notifyItemRemoved(v.adapterPosition)

        }

        return v
    }

    //todo just a reminder that the map is downloaded locally cause if another user logs in the map will already be donwloaded so thaths nice

    class ViewHolder (view : View) : RecyclerView.ViewHolder(view) {

        val coin_recycler_item = view.coin_recycler_item as LinearLayout
        val coin_recycler_bank_coin = view.coin_recycler_bank_coin as ImageView
        val coin_recycler_send_coin = view.coin_recycler_send_coin as ImageView
        val coin_recycler_image = view.coin_recycler_image as ImageView
        val coin_recycler_currency = view.coin_recycler_currency as TextView
        val coin_recycler_value = view.coin_recycler_value as TextView
    }

    fun addCoinToBank(coinId : String, coinCurrency : String, coinValue : String) {

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

            removeCoinFromWallet(firestore, email, coinId, coinCurrency, coinValue, "Banked Coins Today")

        }


    }

    fun removeCoinFromWallet(firestore: FirebaseFirestore, email : String, coinId : String, coinCurrency : String, coinValue : String, storeLocation : String) {

        //todo delete coins from collected and add a union to the coins to remove from map

        val walletReference = firestore.collection("Users").document(email).collection("Coins")

        walletReference.document("Collected Coins").get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        val coinMap: MutableMap<String, Any> = mutableMapOf<String, Any>()

                        coinMap.put(coinId, true)

                        walletReference.document(storeLocation).set(coinMap, SetOptions.merge())

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

    fun sendCoinToUser(position : Int, coinId : String, coinCurrency : String, coinValue : String, dialog : Dialog) {

        val receiverEmail = dialog.coin_recycler_dialog_enter_email.text.toString().toLowerCase()
        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()

        val alert = AlertDialog.Builder(context)
        alert.apply {

            setPositiveButton("OK", null)
            setCancelable(true)
            create()
        }

//todo cant send coins to urself

        if (receiverEmail.isEmpty()) {
            alert.setMessage("Please enter the email").show()
        } else if (email == receiverEmail) {
            alert.setMessage("Sorry, but you can't send coins to yourself").show()
        } else {
            val userWalletReference = firestore.collection("Users").document(email).collection("Coins")
            val receiverWalletReference = firestore.collection("Users").document(receiverEmail)
            receiverWalletReference.get().addOnSuccessListener {
                if (!it.exists()) {
                    alert.setMessage("User under this email does not exist").show()
                } else {
                    userWalletReference.document("Collected Coins").get()
                            .addOnSuccessListener { document ->
                                if (document != null) {

                                    val coinMap: MutableMap<String, Any> = mutableMapOf<String, Any>()
                                    coinMap.put(email + "_" + coinId, document[coinId]!!)

                                    //Send coin to other users database

                                    receiverWalletReference.collection("Coins")
                                            .document("Collected Coins").set(coinMap, SetOptions.merge())

                                    //Remove coin from current users database and but it to Sent

                                    removeCoinFromWallet(firestore, email, coinId, coinCurrency, coinValue, "Sent Coins Today")

                                }
                            }
                    items.removeAt(position)
                    notifyItemRemoved(position)
                    dialog.hide()
                }
            }
        }
    }
}

