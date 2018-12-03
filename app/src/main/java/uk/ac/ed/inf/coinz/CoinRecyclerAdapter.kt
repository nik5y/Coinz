package uk.ac.ed.inf.coinz

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.mapbox.mapboxsdk.annotations.IconFactory
import kotlinx.android.synthetic.main.coin_recycler_item.view.*
import kotlin.coroutines.experimental.coroutineContext

class CoinRecyclerAdapter(val items : ArrayList<CoinRecyclerViewClass>)
    : RecyclerView.Adapter<CoinRecyclerAdapter.ViewHolder>() {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {

        val coin : CoinRecyclerViewClass = items[position]

        holder.coin_recycler_currency.text = coin.currency
        holder.coin_recycler_value.text = coin.value
        holder.coin_recycler_image.setImageResource(coin.iconId)

    }

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int)
            : CoinRecyclerAdapter.ViewHolder {

        val v = CoinRecyclerAdapter.ViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.coin_recycler_item,parent,false))

        v.coin_recycler_item.setOnClickListener {
            println(v.coin_recycler_currency.text)
        }


        return v



    }


    class ViewHolder (view : View) : RecyclerView.ViewHolder(view) {

        val coin_recycler_item = view.coin_recycler_item as LinearLayout
        val coin_recycler_image = view.coin_recycler_image as ImageView
        val coin_recycler_currency = view.coin_recycler_currency as TextView
        val coin_recycler_value = view.coin_recycler_value as TextView
    }

}

