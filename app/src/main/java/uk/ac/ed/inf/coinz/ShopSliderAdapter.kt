package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.support.constraint.ConstraintLayout
import android.support.v4.app.FragmentActivity
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AlertDialog
import android.util.Log
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
import java.util.*
import java.util.zip.Inflater

@SuppressLint("LogNotTimber")
class ShopSliderAdapter(val activity: FragmentActivity, val containerView: View, var context: Context,
                        val firebase: FirebaseFirestore, val email: String) : PagerAdapter(){

    val tag = "ShopSliderAdapter"

    override fun getCount(): Int {
        return bonusTitles.size
     }

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as ConstraintLayout
    }

    private val bonusIcons : Array<Int> = arrayOf(
            R.drawable.shil0,
            R.drawable.shil,
            R.drawable.generic_coin,
            R.drawable.peny)


    private val bonusTitles : Array<String> = arrayOf(
             "Coin Currency",
            "Coin Value",
            "Range+",
            "Rates"
    )

    private val bonusPrices : Array<Double> = arrayOf(
            400.0,400.0,500.0,200.0
    )

    private val bonusDescriptions : Array<String> = arrayOf(
            "Colour coins according to their currency! Lasts until the end of the day.",
            "Display the coin value! Lasts until the end of the day",
            "Double coin collection range! Lasts 24 hours",
            "Find out the rates for today! Available at the map screen. Lasts until the end of the day"
    )

    private val bonusTimed : Array<Boolean> = arrayOf(
            false,
            false,
            true,
            false
    )

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val view = inflater.inflate(R.layout.shop_page_layout, container, false)

        val shop_page_icon = view.shop_page_icon as ImageView
        val shop_page_title = view.shop_page_title as TextView
        val shop_page_description = view.shop_page_description as TextView
        val shop_page_buy = view.shop_page_buy as Button

        shop_page_icon.setImageResource(bonusIcons[position])
        shop_page_title.text = bonusTitles[position]
        shop_page_description.text = bonusDescriptions[position]
        shop_page_buy.text = "${bonusPrices[position].toInt()} GOLD"
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

                            containerView.shop_gold_balance.text = gold.format(2)

                            //update button appearance

                            shop_page_buy.apply {
                                isEnabled = false
                                setBackgroundColor(resources.getColor(R.color.vikaLightOrange))
                            }

                            //set timer to delete --TIMED-- bonuses in 24 hours

                            if (bonusTimed[position]) {
                                //expiration variable:
                                val expiration = Date()
                                expiration.seconds = expiration.seconds + 24*60*60
                                bonusPath.run {
                                    update("activated", true)
                                    update("expires", expiration)
                                }
                                setBonusResetTimer(24, position)
                            } else {
                                bonusPath.run {
                                    update("activated", true)
                                    update("updated", todayYMD())
                                }
                            }

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


    fun setBonusResetTimer(hours : Int, position: Int) {

        val alarmMan = activity.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, TimedBonusReset::class.java).apply {
            putExtra("bonus", bonusTitles[position])
            putExtra("timed", bonusTimed[position])
        }
        val pendIntent = PendingIntent.getBroadcast(context, position+5, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        alarmMan.set(AlarmManager.RTC, Date().time + hours*60*60*1000, pendIntent)
        Log.d(tag, "Alarm for ${bonusTitles[position]} bonus reset set!")
    }

}