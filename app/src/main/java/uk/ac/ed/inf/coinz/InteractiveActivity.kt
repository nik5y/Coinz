package uk.ac.ed.inf.coinz

import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.design.widget.BottomNavigationView
import android.support.design.widget.NavigationView
import android.support.v4.app.FragmentTransaction
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_interactive.*
import kotlinx.android.synthetic.main.fragment_shop.*

class InteractiveActivity : AppCompatActivity() {

    private lateinit var createdFrag : android.support.v4.app.Fragment

    private val mOnNavigationItemSelectedListener = BottomNavigationView.OnNavigationItemSelectedListener {
        when (it.itemId) {
            R.id.nav_shop -> {
                Toast.makeText(this,"hey shop",Toast.LENGTH_LONG).show()
                createdFrag = ShopFragment()

            }
            R.id.nav_coins -> {
            Toast.makeText(this,"hey coins",Toast.LENGTH_LONG).show()
            createdFrag = CoinsFragment()

        }
            R.id.nav_gamble -> {
                Toast.makeText(this,"hey gamble",Toast.LENGTH_LONG).show()
                createdFrag = GambleFragment()
            }

        }
        supportFragmentManager.beginTransaction().replace(R.id.fragment_container,createdFrag).
                setTransition(FragmentTransaction.TRANSIT_ENTER_MASK).commit()
        true
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_interactive)

        bottom_navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener)

        supportFragmentManager.beginTransaction().add(R.id.fragment_container, ShopFragment()).commit()
}

   /* private fun createShopFragment() {
        val transaction = supFragManager.beginTransaction()
        val fragment = ShopFragment()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun createGambleFragment() {
        val transaction = supFragManager.beginTransaction()
        val fragment = GambleFragment()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    private fun createCoinsFragment() {
        val transaction = supFragManager.beginTransaction()
        val fragment = CoinsFragment()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }
*/


}
