package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.support.v7.app.AlertDialog
import android.support.v7.widget.RecyclerView
import android.util.Log.*
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.coin_recycler_dialog.*
import kotlinx.android.synthetic.main.coin_recycler_item.view.*
import java.text.SimpleDateFormat
import java.util.*

private val tag = "CoinRecyclerAdapter"

class CoinRecyclerAdapter(var context: Context, val items : ArrayList<CoinRecyclerViewClass>)
    : RecyclerView.Adapter<CoinRecyclerAdapter.ViewHolder>() {


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val coin: CoinRecyclerViewClass = items[position]
        holder.coin_recycler_currency.text = coin.currency
        //rounds down
        //add double format
        holder.coin_recycler_value.text = coin.value.subSequence(0, 5)
        holder.coin_recycler_image.setImageResource(coin.iconId)

        //todo add if for from thing

        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
        if (coin.sentBy.isNotEmpty()) {
            holder.coin_recycler_item_collectedBy.text = "From: ${coin.sentBy}"
            holder.coin_recycler_item_collectedBy.visibility = View.VISIBLE
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : CoinRecyclerAdapter.ViewHolder {

        val v = CoinRecyclerAdapter.ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.coin_recycler_item, parent, false))

        /*v.coin_recycler_item.setOnClickListener {
            println(v.coin_recycler_currency.text)
        }*/


        //

        v.coin_recycler_send_coin.setOnClickListener { send ->

            //get the coin count

            val firestore = FirebaseFirestore.getInstance()
            val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
            var bankedCoinCount = 0

            val coinCounterPath = firestore.collection("Users").document(email).collection("Account Information")
                    .document("Coin Counter")

            coinCounterPath.get().addOnSuccessListener { count ->

                if (count.get("initialised") != SimpleDateFormat("yyyy/MM/dd").format(Date())) {
                    resetCoinCounter(coinCounterPath)
                    bankedCoinCount = count.get("count").toString().toInt()
                    setUpSendingCoin(bankedCoinCount, v)
                } else {
                    bankedCoinCount = count.get("count").toString().toInt()
                    if (bankedCoinCount >= 25) {
                        setUpSendingCoin(bankedCoinCount, v)
                    } else {
                        val alert = AlertDialog.Builder(context)
                        alert.apply {
                            setPositiveButton("OK", null)
                            setCancelable(true)
                            alert.setMessage("Only spare change can be sent to other users!" +
                                    " Bank ${25 - bankedCoinCount} more coin(s) to be able to send!")
                            create().show()
                        }
                    }
                }

            }

        }

        //deleting coins

        v.coin_recycler_bank_coin.setOnClickListener {

            val firestore = FirebaseFirestore.getInstance()
            val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
            var bankedCoinCount = 0
            val coinId = items.get(v.adapterPosition).id
            val coinCurrency = items.get(v.adapterPosition).currency
            val coinValue = items.get(v.adapterPosition).value

            val coinCounterPath = firestore.collection("Users").document(email).collection("Account Information")
                    .document("Coin Counter")

            coinCounterPath.get().addOnSuccessListener { count ->

                if (count.get("initialised") != SimpleDateFormat("yyyy/MM/dd").format(Date())) {
                    resetCoinCounter(coinCounterPath)
                    bankedCoinCount = count.get("count").toString().toInt()
                    addCoinToBank(coinId,coinCurrency,coinValue)
                    items.removeAt(v.adapterPosition)
                    notifyItemRemoved(v.adapterPosition)
                } else {
                    bankedCoinCount = count.get("count").toString().toInt()
                    if (bankedCoinCount < 25 || coinId.startsWith("s_")) {
                            addCoinToBank(coinId, coinCurrency, coinValue)
                            items.removeAt(v.adapterPosition)
                            notifyItemRemoved(v.adapterPosition)
                        } else {
                        val alert = AlertDialog.Builder(context)
                        alert.apply {
                            setPositiveButton("OK", null)
                            setCancelable(true)
                            alert.setMessage("You can't bank your own coins, once you have banked 25 on one day!")
                            create().show()
                        }
                        }
                    }

            }
        }
        return v
    }

    fun setUpSendingCoin(bankedCoinCount: Int, v: ViewHolder) {

            val coinId = items.get(v.adapterPosition).id
            val coinCurrency = items.get(v.adapterPosition).currency
            val coinValue = items.get(v.adapterPosition).value
            val iconId = items.get(v.adapterPosition).iconId
            val collectedBy = items.get(v.adapterPosition).collectedBy

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
                sendCoinToUser(v.adapterPosition, coinId, coinCurrency, coinValue, collectedBy, dialog)
            }

        }


    //todo just a reminder that the map is downloaded locally cause if another user logs in the map will already be donwloaded so thaths nice

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val coin_recycler_item = view.coin_recycler_item as LinearLayout
        val coin_recycler_bank_coin = view.coin_recycler_bank_coin as ImageView
        val coin_recycler_send_coin = view.coin_recycler_send_coin as ImageView
        val coin_recycler_image = view.coin_recycler_image as ImageView
        val coin_recycler_currency = view.coin_recycler_currency as TextView
        val coin_recycler_value = view.coin_recycler_value as TextView
        val coin_recycler_item_collectedBy = view.coin_recycler_item_collectedBy as TextView
    }

    @SuppressLint("LogNotTimber")
    fun addCoinToBank(coinId: String, coinCurrency: String, coinValue: String) {

        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()
        val bankReference = firestore.collection("Users").document(email).collection("Account Information").document("Gold Balance")

        //todo implement coin addition to bank and recycler view update and moving coins to different folders.

        bankReference.get().addOnSuccessListener {
            var goldBalance = it.data!!.get("goldBalance") as Double


            val sharedPreferences = context.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
            val rate = sharedPreferences.getString(coinCurrency, "default")

            goldBalance += coinValue.toDouble() * rate.toDouble()

            bankReference.set(Bank(goldBalance)).addOnCompleteListener {
                d(tag, "Coin Successfully Banked!")
                addToCoinCounter(coinId, email, firestore)
            }.addOnFailureListener {
                d(tag, "Coin NOT Banked!")
            }
            removeCoinFromWallet(firestore, email, coinId, coinCurrency, coinValue, "Banked Coins Today")
        }


    }

    @SuppressLint("LogNotTimber")
    fun removeCoinFromWallet(firestore: FirebaseFirestore, email: String, coinId: String, coinCurrency: String, coinValue: String, storeLocation: String) {

        //decide whether i want to put the coin in to the sent coins thing

        val walletReference = firestore.collection("Users").document(email).collection("Coins")

        walletReference.document("Collected Coins").get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        val coinMap: MutableMap<String, Any> = mutableMapOf<String, Any>()

                        coinMap.put(coinId, true)

                        walletReference.document(storeLocation).set(coinMap, SetOptions.merge())

                        walletReference.document("Collected Coins").update(coinId, FieldValue.delete())

                        d(tag, "Deleting $coinValue, $coinCurrency")

                    } else {
                        d(tag, "No such document")
                    }
                }
                .addOnFailureListener { exception ->
                    d(tag, "get failed with ", exception)
                }
    }

    fun sendCoinToUser(position: Int, coinId: String, coinCurrency: String, coinValue: String, collectedBy : String, dialog: Dialog) {


        val receiverEmail = dialog.coin_recycler_dialog_enter_email.text.toString().toLowerCase()
        val firestore = FirebaseFirestore.getInstance()
        val email = FirebaseAuth.getInstance().currentUser!!.email.toString()

        //Get the count of banked coins to see if you have "spare change":

        val alert = AlertDialog.Builder(context)
        alert.apply {
            setPositiveButton("OK", null)
            setCancelable(true)
            create()
        }
            if (receiverEmail.isEmpty()) {
                alert.setMessage("Please enter the email").show()
            } else if (email == receiverEmail) {
                alert.setMessage("Sorry, but you can't send coins to yourself").show()
            } else if (receiverEmail == collectedBy) {
                alert.setMessage("Sorry, but you can't send a coin to the person that has originally collected it").show()
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

                                        //todo figure out why document[coinId] gives null when the id contains an email

                                        //can perhaps have the id changed to have the first few lines as the collected users name.
                                        //technically A user can collect A unique coin only once. so having the id as that guarantees itll always
                                        //be unique. i think

                                        //yeh

                                        //can also have it to start with s_ so that we can check if it starts with that, and if it doesnt, rename

                                        //this is so that the key would not get abused and get too large. although it is just a string, still abusible.

                                        val personalDetailsReference = firestore.collection("Users").document(email)
                                                .collection("Account Information").document("Personal Details")

                                        personalDetailsReference.get().addOnSuccessListener { personalData ->

                                            //Send coin to other users database

                                            val coinMap: MutableMap<String, Any> = mutableMapOf<String, Any>()
                                            val coinInfoMap = document[coinId] as MutableMap<String, Any>
                                            coinInfoMap.put("sentBy", email)

                                            if (coinId.startsWith("s_")) {
                                                coinMap.put(coinId, coinInfoMap)
                                            } else {
                                                coinMap.put("s_" + personalData["username"] + "_" + coinId, coinInfoMap)
                                            }

                                            receiverWalletReference.collection("Coins")
                                                    .document("Collected Coins").set(coinMap, SetOptions.merge())
                                            //Remove coin from current users database and put it to Sent
                                            removeCoinFromWallet(firestore, email, coinId, coinCurrency, coinValue, "Sent Coins Today")

                                        }

                                    }
                                }
                        items.removeAt(position)
                        notifyItemRemoved(position)
                        dialog.dismiss()
                    }
                }
            }
        }


    @SuppressLint("LogNotTimber", "SimpleDateFormat")
    private fun addToCoinCounter(coinId : String, email : String, firestore: FirebaseFirestore) {

        val coinCounterPath = firestore.collection("Users").document(email).collection("Account Information")
                .document("Coin Counter")

        coinCounterPath.get().addOnSuccessListener { count ->

            //the if check is only for the situation where the user has turned off his phone when the counter restart was supposed to happen.

            if (count.get("initialised").toString() == SimpleDateFormat("yyyy/MM/dd").format(Date())) {
                if (!coinId.startsWith("s_")) {
                    val newCount = count.get("count").toString().toInt() + 1
                    coinCounterPath.set(CoinCounter(newCount)).addOnCompleteListener {
                        d(tag, "Coin Counter Updated")
                    }.addOnFailureListener {
                        d(tag, "Coin Counter NOT Updated")
                    }
                }
            } else {
                resetCoinCounter(coinCounterPath)
            }
        }
    }

    @SuppressLint("LogNotTimber", "SimpleDateFormat")
    private fun resetCoinCounter(coinCounterPath: DocumentReference) {
        coinCounterPath.run {
            update("initialised", SimpleDateFormat("yyyy/MM/dd").format(Date()))
            update("count", 0)
        }.addOnCompleteListener {
            d("Alarm", "Coin Counter Restarted")
        }.addOnFailureListener {
            d("Alarm", "Coin Counter NOT Restarted")
        }
    }

}


//todo figure out what to do with coins when sent to other user:
//todo 1. allow sending them back and forth between same users?
//todo 2. allow sending them more than once?
//todo 3. make a thingy that changes the id in some way. perhaps uses the senders username or smth..


//constrain usernames to be some amount of characters long max.

//sent coins should be looked at when deciding upon map right? but the list gerts deleteed anywat.

//list doesnt get deleted when user turns his phone off. a better way wouls be to implement the checker at the server.

