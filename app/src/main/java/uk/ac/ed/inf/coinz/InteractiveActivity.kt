package uk.ac.ed.inf.coinz

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.v4.app.FragmentTransaction
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_interactive.*

class InteractiveActivity : AppCompatActivity() {

    private var nextFragment: android.support.v4.app.Fragment? = null
    private val mAuth = FirebaseAuth.getInstance()

    //Navigation selector used to enable the bottomnavigation bar

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {

        //defining the current fragment in order to check whether the fragment requested is already opened
        val currentFrag = supportFragmentManager.findFragmentById(R.id.fragment_container)

        when (it.itemId) {
            R.id.nav_shop -> {
                if (!(currentFrag is ShopFragment)) {
                            nextFragment = ShopFragment()
                }
            }
            R.id.nav_coins -> {
                if (!(currentFrag is CoinsFragment)) {
                    nextFragment = CoinsFragment()
                }
            }

            R.id.nav_profile -> {
                if (!(currentFrag is ProfileFragment)) {
                    nextFragment = ProfileFragment()
                }
            }
        }

        //replace fragment views only if a different fragment was selected
        if(nextFragment!=null) {
            supportFragmentManager.beginTransaction().replace(R.id.fragment_container, nextFragment).setTransition(FragmentTransaction.TRANSIT_ENTER_MASK).commit()
        }

         true
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interactive)

        //enabling the bottomnavigation bar

        bottom_navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        //open the ShopFragment by default

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, ShopFragment()).commit()

        //onClickListener to be able to go to Map with a custom animation

        interactive_go_to_map.setOnClickListener {

            goToMap()
            overridePendingTransition(R.anim.left_slide_in, R.anim.right_slide_out)
        }

        //onClickListener to be able to go to Login

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
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

    //if the user was to log out, the flags guarantee that the user would not go back to the activity by tapping back button

    private fun goToLogin() {

        mAuth.signOut()

        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

}


