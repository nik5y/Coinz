package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentTransaction
import kotlinx.android.synthetic.main.activity_interactive.*

class InteractiveActivity : AppCompatActivity() {

    private var createdFrag: android.support.v4.app.Fragment? = null

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {

        val currentFrag = supportFragmentManager.findFragmentById(R.id.fragment_container)

        when (it.itemId) {
            R.id.nav_shop -> {
                if (!(currentFrag is ShopFragment)) {
                            createdFrag = ShopFragment()
                }
            }
            R.id.nav_coins -> {
                if (!(currentFrag is CoinsFragment)) {
                    createdFrag = CoinsFragment()
                }
            }
            R.id.nav_gamble -> {
                if (!(currentFrag is GambleFragment)) {
                    createdFrag = GambleFragment()
                }
            }
            R.id.nav_profile -> {
                if (!(currentFrag is ProfileFragment)) {
                    createdFrag = ProfileFragment()
                }
            }
        }

        if(createdFrag!=null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, createdFrag).setTransition(FragmentTransaction.TRANSIT_ENTER_MASK).commit()
        }

         true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interactive)

        /*if (savedInstanceState == null) {
            bottom_navigation.selectedItemId = R.id.nav_coins
        }*/

        bottom_navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, ShopFragment()).commit()

        interactive_go_to_map.setOnClickListener {

            goToMap()
            overridePendingTransition(R.anim.left_slide_in, R.anim.right_slide_out)
        }

        interactive_go_to_login.setOnClickListener {

            goToLogin()

        }

}

    //for the slide animation if the user was to click back instead of icon.

    override fun finish() {
        super.finish()
        overridePendingTransition(R.anim.left_slide_in,R.anim.right_slide_out)
    }

    private fun goToMap() {
            val intent = Intent(this, MapsActivity::class.java)
            startActivity(intent)
        }

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

}


