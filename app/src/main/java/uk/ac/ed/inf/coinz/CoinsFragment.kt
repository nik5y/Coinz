

package uk.ac.ed.inf.coinz


import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_coins.*

class CoinsFragment : Fragment() {

    private val firestore = FirebaseFirestore.getInstance()
    private var mAuth = FirebaseAuth.getInstance()
    private val userEmail = mAuth.currentUser?.email
    private val coinsArray = ArrayList<CoinRecyclerViewClass>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_coins, container, false)

        val recycler = view.findViewById(R.id.recyclerView_Coins) as RecyclerView

        recycler.layoutManager = LinearLayoutManager(activity, LinearLayout.VERTICAL, false)

        //A custom callback function created so that it would be possible to return an array of coins from the asynchronous
        //task of retrieving coins from the database.

        readData(object : MyCallback {
            override fun onCallback(value: ArrayList<CoinRecyclerViewClass>) {

                //The returned value, i.e. coin array is fed in to the adapter
                recycler.adapter = CoinRecyclerAdapter(context!!, value)
            }
        })

        return view

    }

    interface MyCallback {
        fun onCallback(value: ArrayList<CoinRecyclerViewClass>)
    }

    private fun readData(callback: MyCallback) {

        val coinsReference = firestore.collection("Users").document(userEmail!!).collection("Coins")
                .document("Collected Coins")

        coinsReference.get().addOnSuccessListener {

            val coinMaps = it?.data

            //necessary check, or else it crashes when empty

            if (coinMaps == null) {
                coins_fragment_nocoins.visibility = View.VISIBLE
                return@addOnSuccessListener
            } else {
                for (key in coinMaps.keys) {
                    @Suppress("UNCHECKED_CAST")//due to the nature of coin storing in database, it is appropriate to suppress warning
                    val coinInfoMap = coinMaps[key] as MutableMap<String, String>

                    coinsArray.add(CoinRecyclerViewClass(key, coinInfoMap["currency"]!!, coinInfoMap["value"]!!,
                            resources.getIdentifier(coinInfoMap["currency"]!!.toLowerCase(), "drawable", "uk.ac.ed.inf.coinz")
                            ,coinInfoMap["sentBy"]!!, coinInfoMap["collectedBy"]!!))
                }

                //Display No Coins text if there are no coins

                if(coinsArray.isEmpty()){
                    coins_fragment_nocoins.visibility = View.VISIBLE
                } else {
                    coins_fragment_nocoins.visibility = View.GONE
                }

                callback.onCallback(coinsArray)

            }
        }

    }

}


