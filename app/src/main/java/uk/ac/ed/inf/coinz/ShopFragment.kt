package uk.ac.ed.inf.coinz

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.view.ViewPager
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.fragment_shop.*

@Suppress("DEPRECATION")
class ShopFragment : Fragment() {

    private var dots : ArrayList<TextView> = arrayListOf()
    private lateinit var pageDots : LinearLayout
    private lateinit var slider : ViewPager
    private lateinit var prevButton : Button
    private lateinit var nextButton : Button
    private val firebase = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser!!.email
    private var currentPage = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        val view = inflater.inflate(R.layout.fragment_shop, container, false)
        slider = view.findViewById(R.id.shop_pager)
        pageDots = view.findViewById(R.id.shop_page_dots)
        prevButton = view.findViewById(R.id.shop_page_back_button)
        nextButton = view.findViewById(R.id.shop_page_next_button)

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


                currentPage = position

                if(position == 0){
                    nextButton.visibility = View.VISIBLE
                    prevButton.visibility = View.INVISIBLE
                } else if(currentPage < 3) {
                    nextButton.visibility = View.VISIBLE
                    prevButton.visibility = View.VISIBLE
                } else {
                    nextButton.visibility = View.INVISIBLE
                    prevButton.visibility = View.VISIBLE
                }

            }

        })

        firebase.collection("Users").document(currentUser).collection("Account Information")
                .document("Gold Balance").get().addOnSuccessListener {
                    val goldBalance = it["goldBalance"].toString().toDouble().format(3)
                    if (goldBalance == "0.000") {
                        shop_gold_balance.text = "0" //for prettiness
                    } else {
                        shop_gold_balance.text = goldBalance
                    }
                    //make the display visible
                    shop_gold_balance_icon.visibility = View.VISIBLE
                }

        nextButton.setOnClickListener {
            slider.setCurrentItem(currentPage+1)
        }

        prevButton.setOnClickListener {
            slider.setCurrentItem(currentPage-1)
        }

        return view
    }


    private fun addDots(position: Int) {
        for (i in 0..3) {
            dots.add(i,TextView(context!!))
            dots[i].text = Html.fromHtml("&#8226", Html.FROM_HTML_MODE_LEGACY)
            dots[i].textSize = 30.0F
            pageDots.addView(dots[i])

        }

        dots[position].setTextColor(resources.getColor(R.color.mapboxGrayLight))

    }

}
