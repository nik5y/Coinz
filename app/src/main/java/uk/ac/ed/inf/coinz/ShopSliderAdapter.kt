package uk.ac.ed.inf.coinz

import android.content.Context
import android.support.constraint.ConstraintLayout
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_shop.view.*
import kotlinx.android.synthetic.main.shop_page_layout.view.*
import java.util.zip.Inflater

class ShopSliderAdapter(containerView : View, var context: Context, firebase : FirebaseFirestore, email : String) : PagerAdapter(){

    val firebase = firebase
    val email = email
    val containerView = containerView

    override fun getCount(): Int {
        return bonusTitles.size
     }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as ConstraintLayout;
    }

    val bonusIcons : Array<Int> = arrayOf(
            R.drawable.shil0,
            R.drawable.shil,
            R.drawable.generic_coin,
            R.drawable.peny)


    val bonusTitles : Array<String> = arrayOf(
            "Coin Currency",
            "Coin Value",
            "Range+",
            "Rates"
    )

    val bonusPrices : Array<Double> = arrayOf(
            400.0,400.0,500.0,200.0
    )

    val bonusDescriptions : Array<String> = arrayOf(
            "Colour coins according to their currency! Lasts until the end of the day.",
            "Display the coin value! Lasts until the end of the day",
            "Double coin collection range! Lasts 24 hours",
            "Find out the rates for today! Available at the map screen. Lasts until the end of the day"
    )

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.shop_page_layout, container, false)

        val shop_page_icon = view.shop_page_icon as ImageView
        val shop_page_title = view.shop_page_title as TextView
        val shop_page_description = view.shop_page_description as TextView
        val shop_page_buy = view.shop_page_buy as Button

        shop_page_icon.setImageResource(bonusIcons[position])
        shop_page_title.setText(bonusTitles[position])
        shop_page_description.setText(bonusDescriptions[position])
        shop_page_buy.setText("${bonusPrices[position].toInt()} GOLD")
        container.addView(view)

        val bonusPath = firebase.collection("Users").document(email).collection("Bonuses")
                .document(bonusTitles[position])

        bonusPath.get().addOnSuccessListener {activated->

            if ( activated!!["activated"] as Boolean) {
                shop_page_buy.apply {
                    isEnabled = false
                    setBackgroundColor(resources.getColor(R.color.vikaLightOrange))
                }
            } else {
                shop_page_buy.setOnClickListener {


                    val goldBalancePath = firebase.collection("Users").document(email).collection("Account Information")
                            .document("Gold Balance")

                    goldBalancePath.get().addOnSuccessListener {

                        var gold = it["goldBalance"].toString().toDouble()

                        if (gold >= bonusPrices[position]) {

                            //Reduce Balance

                            gold = gold - bonusPrices[position]
                            goldBalancePath.update("goldBalance", gold)

                            //update balance on screen

                            containerView.shop_gold_balance.setText(gold.format(2))

                            //update button appearance

                            shop_page_buy.apply {
                                isEnabled = false
                                setBackgroundColor(resources.getColor(R.color.vikaLightOrange))
                            }


                            //Enable Bonus in Database

                            bonusPath.update("activated", true)

                            //set timer to delete bonus in 24 hours



                        } else {
                            val alert = AlertDialog.Builder(context)
                            alert.apply {
                                setMessage("Not enough gold!")
                                setPositiveButton("OK", null)
                                setCancelable(true)
                                create().show()
                            }
                        }

                    }

                }
            }

        }



        return view
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        return container.removeView(`object` as ConstraintLayout)
    }

}