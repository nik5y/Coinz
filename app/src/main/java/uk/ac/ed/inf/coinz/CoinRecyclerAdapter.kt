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
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.android.synthetic.main.coin_recycler_dialog.*
import kotlinx.android.synthetic.main.coin_recycler_item.view.*
import java.util.*

private const val tag = "CoinRecyclerAdapter"

class CoinRecyclerAdapter(var context: Context, private val items : ArrayList<CoinRecyclerViewClass>)
    : RecyclerView.Adapter<CoinRecyclerAdapter.ViewHolder>() {

    val firestore = FirebaseFirestore.getInstance()
    val email = FirebaseAuth.getInstance().currentUser!!.email.toString()

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val coin: CoinRecyclerViewClass = items[position]
        holder.coinRecyclerCurrency.text = coin.currency

        holder.coinRecyclerValue.text = coin.value.toDouble().format(3)
        holder.coinRecyclerImage.setImageResource(coin.iconId)


        if (coin.sentBy.isNotEmpty()) {
            holder.coinRecyclerItemCollectedBy.text = "From: ${coin.sentBy}"
            holder.coinRecyclerItemCollectedBy.visibility = View.VISIBLE
        }

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : CoinRecyclerAdapter.ViewHolder {

        val v = CoinRecyclerAdapter.ViewHolder(LayoutInflater.from(context)
                .inflate(R.layout.coin_recycler_item, parent, false))


        v.coinRecyclerSendCoin.setOnClickListener { _ ->

            //get the coin count

            var bankedCoinCount : Int

            val coinCounterPath = firestore.collection("Users").document(email).collection("Account Information")
                    .document("Banked Coin Counter")

            coinCounterPath.get().addOnSuccessListener { count ->

                if (count.get("initialised") != todayYMD()) {
                    resetCoinCounter(coinCounterPath)
                    bankedCoinCount = count.get("count").toString().toInt()
                    setUpSendingCoin( v)
                } else {
                    bankedCoinCount = count.get("count").toString().toInt()
                    if (bankedCoinCount >= 25) {
                        setUpSendingCoin( v)
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

        v.coinRecyclerBankCoin.setOnClickListener {

            var bankedCoinCount : Int
            val coinId = items[v.adapterPosition].id
            val coinCurrency = items[v.adapterPosition].currency
            val coinValue = items[v.adapterPosition].value

            val coinCounterPath = firestore.collection("Users").document(email).collection("Account Information")
                    .document("Banked Coin Counter")

            coinCounterPath.get().addOnSuccessListener { count ->

                if (count.get("initialised") != todayYMD()) {
                    resetCoinCounter(coinCounterPath)
                    bankedCoinCount = count.get("count").toString().toInt()
                    addCoinToBank(coinId,coinCurrency,coinValue)
                    items.removeAt(v.adapterPosition)
                    notifyItemRemoved(v.adapterPosition)
                } else {
                    bankedCoinCount = count.get("count").toString().toInt()
                    //either below given limit or if it has been sent by somebody (identified by the s_ prefix)
                    if (bankedCoinCount < 25 || coinId.startsWith("s_"))  {
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

    private fun setUpSendingCoin(v: ViewHolder) {

            val coinId = items[v.adapterPosition].id
            val coinCurrency = items[v.adapterPosition].currency
            val coinValue = items[v.adapterPosition].value
            val iconId = items[v.adapterPosition].iconId
            val collectedBy = items[v.adapterPosition].collectedBy

            //SENDING COINS

            //create Dialog for sending coins

            val dialog = Dialog(context)
            dialog.setContentView(R.layout.coin_recycler_dialog)
            dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.coin_recycler_dialog_coinValue.text = coinValue
        dialog.coin_recycler_dialog_coin_currency.text = coinCurrency
            dialog.coin_recycler_dialog_image.setImageResource(iconId)
            dialog.show()

            dialog.coin_recycler_dialog_send_coin.setOnClickListener {
                sendCoinToUser(v.adapterPosition, coinId, coinCurrency, coinValue, collectedBy, dialog)
            }

        }


    //todo just a reminder that the map is downloaded locally cause if another user logs in the map will already be downloaded so that's nice

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val coinRecyclerBankCoin = view.coin_recycler_bank_coin as ImageView
        val coinRecyclerSendCoin = view.coin_recycler_send_coin as ImageView
        val coinRecyclerImage = view.coin_recycler_image as ImageView
        val coinRecyclerCurrency = view.coin_recycler_currency as TextView
        val coinRecyclerValue = view.coin_recycler_value as TextView
        val coinRecyclerItemCollectedBy = view.coin_recycler_item_collectedBy as TextView
    }

    @SuppressLint("LogNotTimber")
    fun addCoinToBank(coinId: String, coinCurrency: String, coinValue: String) {

        val bankReference = firestore.collection("Users").document(email)
                .collection("Account Information").document("Gold Balance")

        bankReference.get().addOnSuccessListener {
            var goldBalance = it.data!!["goldBalance"] as Double


            val sharedPreferences = context.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
            val rate = sharedPreferences.getString(coinCurrency, "default")

            goldBalance += coinValue.toDouble() * rate.toDouble()

            bankReference.set(Bank(goldBalance)).addOnCompleteListener { _ ->
                d(tag, "Coin Successfully Banked!")
                addToCoinCounter(coinId, email, firestore)
            }.addOnFailureListener { _ ->
                d(tag, "Coin NOT Banked!")
            }
            removeCoinFromWallet(firestore, email, coinId, coinCurrency, coinValue, "Banked Coins Today")
        }


    }

    @SuppressLint("LogNotTimber")
    fun removeCoinFromWallet(firestore: FirebaseFirestore, email: String, coinId: String, coinCurrency: String, coinValue: String, storeLocation: String) {

        val walletReference = firestore.collection("Users").document(email).collection("Coins")

        walletReference.document("Collected Coins").get()
                .addOnSuccessListener { document ->
                    if (document != null) {

                        val coinMap: MutableMap<String, Any> = mutableMapOf()

                        coinMap[coinId] = true

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

    private fun sendCoinToUser(position: Int, coinId: String, coinCurrency: String, coinValue: String, collectedBy : String, dialog: Dialog) {

        val receiverEmail = dialog.coin_recycler_dialog_enter_email.text.toString().toLowerCase()

        val alert = AlertDialog.Builder(context)
        alert.apply {
            setPositiveButton("OK", null)
            setCancelable(true)
            create()
        }
        when {

            receiverEmail.isEmpty() -> alert.setMessage("Please enter the email").show()

            email == receiverEmail -> alert.setMessage("Sorry, but you can't send coins to yourself").show()

            receiverEmail == collectedBy -> alert.setMessage("Sorry, but you can't send a coin to the person that has originally collected it").show()

            else -> {
                val userWalletReference = firestore.collection("Users").document(email).collection("Coins")
                val receiverWalletReference = firestore.collection("Users").document(receiverEmail)
                receiverWalletReference.get().addOnSuccessListener {
                    if (!it.exists()) {
                        alert.setMessage("User under this email does not exist").show()
                    } else {
                        userWalletReference.document("Collected Coins").get()
                                .addOnSuccessListener { document ->
                                    if (document != null) {

                                        //can perhaps have the id changed to have the first few lines as the collected users name.
                                        //technically A user can collect A unique coin only once. so having the id as that guarantees it'll always
                                        //be unique. i think

                                        //yeh

                                        //can also have it to start with s_ so that we can check if it starts with that, and if it doesn't, rename

                                        //this is so that the key would not get abused and get too large. although it is just a string, still possible to abuse.

                                        val personalDetailsReference = firestore.collection("Users").document(email)
                                                .collection("Account Information").document("Personal Details")

                                        personalDetailsReference.get().addOnSuccessListener { personalData ->

                                            //Send coin to other users database

                                            val coinMap: MutableMap<String, Any> = mutableMapOf()
                                            @Suppress("UNCHECKED_CAST") //due to the nature of coin storing in database, it is appropriate to suppress the warning
                                            val coinInfoMap = document[coinId] as MutableMap<String, Any>
                                            coinInfoMap["sentBy"] = email

                                            if (coinId.startsWith("s_")) {
                                                coinMap[coinId] = coinInfoMap
                                            } else {
                                                coinMap["s_" + personalData["username"] + "_" + coinId] = coinInfoMap
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
        }


    @SuppressLint("LogNotTimber", "SimpleDateFormat")
    private fun addToCoinCounter(coinId : String, email : String, firestore: FirebaseFirestore) {

        val coinCounterPath = firestore.collection("Users").document(email).collection("Account Information")
                .document("Banked Coin Counter")

        coinCounterPath.get().addOnSuccessListener { count ->

            //the if check is only for the situation where the user has turned off his phone when the counter restart was supposed to happen.

            if (count.get("initialised").toString() == todayYMD()) {
                if (!coinId.startsWith("s_")) {
                    //only own coins contribute towards the counter
                    val newCount = count.get("count").toString().toInt() + 1
                    coinCounterPath.set(CoinCounter(newCount)).addOnCompleteListener {
                        d(tag, "Banked Coin Counter Updated")
                    }.addOnFailureListener {
                        d(tag, "Banked Coin Counter NOT Updated")
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
            update("initialised", todayYMD())
            update("count", 0)
        }.addOnCompleteListener {
            d("CoinRecyclerAdapter", "Banked Coin Counter Restarted")
        }.addOnFailureListener {
            d("CoinRecyclerAdapter", "Banked Coin Counter NOT Restarted")
        }
    }
}
