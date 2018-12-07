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
    private var coinsFromDatabse: MutableSet<String>? = null
    val coinsArray = ArrayList<CoinRecyclerViewClass>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_coins, container, false)

        val recyc = view.findViewById<RecyclerView>(R.id.recyclerView_Coins) as RecyclerView

        recyc.layoutManager = LinearLayoutManager(activity, LinearLayout.VERTICAL, false)

    readData(object : MyCallback {
        override fun onCallback(value: ArrayList<CoinRecyclerViewClass>) {
            recyc.adapter = CoinRecyclerAdapter(context!!, value)
        }
    })
//todo add go back to map button

        return view

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        //todo add a text when there are no coin collected. i.e adjust visibility depending on array size


    }

    interface MyCallback {
        fun onCallback(value: ArrayList<CoinRecyclerViewClass>)
    }

    public fun readData(callback: MyCallback) {
        firestore.collection("Users").document(userEmail!!).collection("Coins").document("Collected Coins").get().addOnSuccessListener {

            val coinMaps = it?.data
            if (coinMaps == null) {
                return@addOnSuccessListener
            } else {
                for (key in coinMaps.keys) {
                    val coinInfoMap = coinMaps[key] as MutableMap<String, String>
                    //coinInfoMap["currency"]
                    coinsArray.add(CoinRecyclerViewClass(key, coinInfoMap["currency"]!!, coinInfoMap["value"]!!, resources.getIdentifier(coinInfoMap["currency"]!!.toLowerCase(), "drawable", "uk.ac.ed.inf.coinz")))
                }

                callback.onCallback(coinsArray)

            }
        }

    }

}


