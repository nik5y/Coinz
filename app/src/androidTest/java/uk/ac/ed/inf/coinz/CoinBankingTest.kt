package uk.ac.ed.inf.coinz


import android.content.Context
import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.click
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.rule.GrantPermissionRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.geometry.LatLng
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class CoinBankingTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(InteractiveActivity::class.java)
    private val mAuth = FirebaseAuth.getInstance()!!
    private val email = "tp@tp.com"
    private val password = "tptptp"
    private val firestore = FirebaseFirestore.getInstance()

    @Rule
    @JvmField
    var mGrantPermissionRule =
            GrantPermissionRule.grant(
                    "android.permission.ACCESS_FINE_LOCATION")!!


    @Before
    fun logIn(){
        mAuth.signInWithEmailAndPassword(email,password)
    }


    @After
    fun delete() {
        deleteCoins()
        updateGold()
        resetCoinCounter()
    }

    /**
     * This test creates a mock coin and inserts it in to the database.
     * Furthermore, it alters one of the rates, corresponding to the coin, to a specified one.
     * Next, it banks the coin and check the Profile Fragment, that displays user stats, to see if everything has updated correctly.
     * Therefore this test tests:
     * 1. Interaction of the database with the Coin Fragment.
     * 2. Banking mechanism.
     * 3. Correct information display in the Profile Fragment.
     */

    @Test
    fun bankingCoins(){

        Thread.sleep(2000)

        //Set up mock values

        val coinValue = "5.0000000"
        val targetCurrency = "DOLR"
        val targetRateValue = "10.0000000"
        val oldRate = getOldSharedPrefs(targetCurrency)

        //Database pre-processing and mock coin insertion

        deleteCoins()
        insertCoin(coinValue, targetCurrency)
        resetCoinCounter()
        updateGold()

        //Rates altering

        setNewSharedPrefs(targetCurrency, targetRateValue)

        Thread.sleep(5000)

        onView(allOf(withId(R.id.nav_coins))).perform(click())

        Thread.sleep(3000)

        //Coin Banking in Recycler View

        //CHECK FOR CORRECT CURRENCY DISPLAY

        onView(allOf(withId(R.id.coin_recycler_currency))).check(matches(withText(targetCurrency)))

        //CHECK FOR CORRECT VALUE DISPLAY

        onView(allOf(withId(R.id.coin_recycler_value))).check(matches(withText(coinValue.toDouble().format(3))))

        val appCompatImageView2 = onView(
                allOf(withId(R.id.coin_recycler_bank_coin), withContentDescription("Icon for button that banks the coin for each coin in the recycler view"),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.coin_recycler_item),
                                        1),
                                2),
                        isDisplayed()))
        appCompatImageView2.perform(click())

        Thread.sleep(3000)

        val bottomNavigationItemView2 = onView(
                allOf(withId(R.id.nav_profile),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.bottom_navigation),
                                        0),
                                2),
                        isDisplayed()))
        bottomNavigationItemView2.perform(click())

        Thread.sleep(5000)

        //CHECK FOR COIN COUNTER UPDATE

        onView(allOf(withId(R.id.profile_collected_value))).check(matches(withText("1")))

        //CHECK FOR CORRECT GOLD BALANCE UPDATE

        onView(allOf(withId(R.id.profile_gold_value))).check(matches(withText((targetRateValue.toDouble() * coinValue.toDouble()).format(3))))

        //Reset to old rate

        setNewSharedPrefs(targetCurrency, oldRate)

    }

    private fun createCoinMutableMap(marker: Marker): MutableMap<String, Any> {
        val featuresCoin = marker.title.toString().split(" ")

        val currValMap: MutableMap<String, String> = mutableMapOf<String, String>().apply {
            put("currency", featuresCoin[1])
            put("value", featuresCoin[2])
            put("collectedBy", email)
            put("sentBy", "")
        }

        val coinMap: MutableMap<String, Any> = mutableMapOf()
        coinMap[featuresCoin[0]] = currValMap

        return coinMap
    }

    private fun addCoinToDatabase(coin: MutableMap<String, Any>) {

        val coinReference = firestore.collection("Users").document(email).collection("Coins").document("Collected Coins")

        coinReference.set(coin, SetOptions.merge()).addOnCompleteListener {

        }.addOnFailureListener {

        }

    }

    private fun addToCoinCounter() {

        val coinCounterPath = firestore.collection("Users").document(email)
                .collection("Account Information").document("Collected Coin Counter")

        coinCounterPath.get().addOnSuccessListener { count ->

            val newCount = count.get("count").toString().toInt() + 1
            coinCounterPath.set(CoinCounter(newCount)).addOnCompleteListener {

            }.addOnFailureListener {

            }

        }
    }

    private fun disableBonus(bonus : String) {
        //Restores the bonus back to false
        firestore.collection("Users").document(email).collection("Bonuses")
                .document(bonus).update("activated", false)
    }

    private fun deleteCoins() {
        firestore.collection("Users").document(email).collection("Coins")
                .document("Collected Coins").delete()
    }

    private fun resetCoinCounter() {
        firestore.collection("Users").document(email).collection("Account Information")
                .document("Collected Coin Counter").update("count", 0)
    }

    private fun updateGold() {

        firestore.collection("Users").document(email).collection("Account Information")
                .document("Gold Balance").update("goldBalance", 0.0)

    }

    private fun insertCoin(coinValue : String, coinCurrency : String) {
        val marker = Marker(MarkerOptions().position(LatLng(0.0, 0.0)).title("dddd-dddd-dddd-dddd $coinCurrency $coinValue"))

        val coinMap = createCoinMutableMap(marker)

        addCoinToDatabase(coinMap)
        addToCoinCounter()
    }

    private fun getOldSharedPrefs(key : String) : String {
        val sharedPreferences = mActivityTestRule.activity.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, "default")
    }

    private fun setNewSharedPrefs(key : String, value : String){
        val sharedPreferences = mActivityTestRule.activity.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key,value).apply()
    }

    private fun childAtPosition(
            parentMatcher: Matcher<View>, position: Int): Matcher<View> {

        return object : TypeSafeMatcher<View>() {
            override fun describeTo(description: Description) {
                description.appendText("Child at position $position in parent ")
                parentMatcher.describeTo(description)
            }

            public override fun matchesSafely(view: View): Boolean {
                val parent = view.parent
                return parent is ViewGroup && parentMatcher.matches(parent)
                        && view == parent.getChildAt(position)
            }
        }
    }
}
