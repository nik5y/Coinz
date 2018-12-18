package uk.ac.ed.inf.coinz

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_shop.*

class ShopFragment : Fragment() {

    var dots : ArrayList<TextView> = arrayListOf()
    lateinit var pageDots : LinearLayout
    lateinit var slider : ViewPager
    val firebase = FirebaseFirestore.getInstance()
    val currentUser = FirebaseAuth.getInstance().currentUser!!.email

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_shop, container, false)

        slider = view.findViewById<ViewPager>(R.id.shop_pager)
        pageDots = view.findViewById<LinearLayout>(R.id.shop_page_dots)

        slider.adapter = ShopSliderAdapter(activity!!, view, context!!, firebase, currentUser!!)

        addDots(0)

        slider.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {

            override fun onPageScrollStateChanged(state: Int) {

            }

            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {

            }
            override fun onPageSelected(position: Int) {
                pageDots.removeAllViewsInLayout()
                addDots(position)
            }

        })

        firebase.collection("Users").document(currentUser).collection("Account Information")
                .document("Gold Balance").get().addOnSuccessListener {
                    val gold = it["goldBalance"].toString().toDouble()
                    shop_gold_balance.setText(gold.format(2))
                }

        return view
    }


    private fun addDots(position: Int) {
        for (i in 0..3) {
            dots.add(i,TextView(context!!))
            dots[i].setText(Html.fromHtml("&#8226", Html.FROM_HTML_MODE_LEGACY))
            dots[i].setTextSize(30.0F)
            pageDots.addView(dots[i])

        }

        dots[position].setTextColor(resources.getColor(R.color.mapboxWhite))

    }

    private fun highlightDot(position: Int){

    }

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

//todo disable rotation

}
