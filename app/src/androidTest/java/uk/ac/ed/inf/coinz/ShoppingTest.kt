package uk.ac.ed.inf.coinz


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
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
import com.mapbox.mapboxsdk.annotations.IconFactory
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
class ShoppingTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(MapsActivity::class.java)
    private val mAuth = FirebaseAuth.getInstance()
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

    @Before
    fun setUpUserDatabase(){
        disableBonus("Coin Value")
        disableBonus("Coin Currency")
        updateGold()
    }

    private fun disableBonus(bonus : String) {
        //Restores the bonus back to false
        firestore.collection("Users").document(email).collection("Bonuses")
                .document(bonus).update("activated", false)
    }

    private fun updateGold() {

        firestore.collection("Users").document(email).collection("Account Information")
                .document("Gold Balance").update("goldBalance", 100000.0)

    }


    /**
     * Tests here all test if the shopping is working as expected, by:
     * 1. Buying a certain combination of bonuses for marker appearance
     * 2. Getting one marker out of the whole map and storing its bitmap information
     * 3. According to that markers value and currency AND to the bonus condition, generate a bitmap that is EXPECTED
     * to be displayed in the map.
     * 4. Comparing if the bitmaps are the same.
     * 4.1. If yes, perform a isDisplayed() check on something that is known to be displayed.
     * 4.2. If not, force to check something that is definitely not displayed.
     *
     * Therefore, this test indirectly tests whether:
     * 1. The bonus database is correctly linked to the app.
     * 2. The shopping fragment works  and displays everything as expected.
     * 3. The coin marker initialisation on the map correctly accounts for bonuses.
     */


    //Bonus Condition = Coin Currency
    @Test
    fun currencyBonusMarkerIcon() {

        Thread.sleep(3000)

        val appCompatImageView = onView(
                allOf(withId(R.id.maps_go_to_interactive)
                        , withContentDescription("An icon for the button that navigates from Maps Activity to Interactive Activity")))
        appCompatImageView.perform(click())

        Thread.sleep(3000)

        //Buy the Coin Currency Bonus

        val appCompatButton2 = onView(
                allOf(withId(R.id.shop_page_buy), withText("400 GOLD"),
                        childAtPosition(
                                withParent(withId(R.id.shop_pager)),
                                3),
                        isDisplayed()))
        appCompatButton2.perform(click())

        Thread.sleep(3000)

        val appCompatImageView2 = onView(
                allOf(withId(R.id.interactive_go_to_map),
                        withContentDescription("Icon for button that navigates to Maps from Interactive activities")))
        appCompatImageView2.perform(click())

        Thread.sleep(7000)

        //The if statement forcing a view check, depending on the output of checkMarkers()

        if(checkMarkers("Coin Currency")) {
            onView(withId(R.id.maps_open_rates_dialog)).check(matches(isDisplayed()))
        } else {
            onView(withText("FORCED TEST FAIL")).check(matches(isDisplayed()))
        }

    }

    //Bonus Condition = Coin Currency AND Coin Value
    @Test
    fun valueAndCurrencyBonusMarkerIcon() {

        Thread.sleep(3000)

        val appCompatImageView = onView(
                allOf(withId(R.id.maps_go_to_interactive)
                        , withContentDescription("An icon for the button that navigates from Maps Activity to Interactive Activity")))
        appCompatImageView.perform(click())

        Thread.sleep(3000)

        //Buy Coin Currency Bonus

        val appCompatButton2 = onView(
                allOf(withId(R.id.shop_page_buy), withText("400 GOLD"),
                        childAtPosition(
                                withParent(withId(R.id.shop_pager)),
                                3),
                        isDisplayed()))
        appCompatButton2.perform(click())

        Thread.sleep(2000)

        onView(allOf(withId(R.id.shop_page_next_button))).perform(click())

        Thread.sleep(2000)

        //Buy Coin Value Bonus

        appCompatButton2.perform(click())

        Thread.sleep(2000)

        val appCompatImageView2 = onView(
                allOf(withId(R.id.interactive_go_to_map),
                        withContentDescription("Icon for button that navigates to Maps from Interactive activities")))
        appCompatImageView2.perform(click())

        Thread.sleep(7000)

        //The if statement forcing a view check, depending on the output of checkMarkers()

        if(checkMarkers("ValueAndCurrency")) {
            onView(withId(R.id.maps_open_rates_dialog)).check(matches(isDisplayed()))
        } else {
            onView(withText("FORCED TEST FAIL")).check(matches(isDisplayed()))
        }
    }

    //Bonus Condition = Coin Value
    @Test
    fun valueBonusMarkerIcon() {

        Thread.sleep(3000)

        val appCompatImageView = onView(
                allOf(withId(R.id.maps_go_to_interactive)
                        , withContentDescription("An icon for the button that navigates from Maps Activity to Interactive Activity")))
        appCompatImageView.perform(click())

        Thread.sleep(3000)

        onView(allOf(withId(R.id.shop_page_next_button))).perform(click())

        Thread.sleep(3000)

        //Buy Coin Value Bonus

        val appCompatButton2 = onView(
                allOf(withId(R.id.shop_page_buy), withText("400 GOLD"),
                        childAtPosition(
                                withParent(withId(R.id.shop_pager)),
                                3),
                        isDisplayed()))
        appCompatButton2.perform(click())

        Thread.sleep(3000)

        val appCompatImageView2 = onView(
                allOf(withId(R.id.interactive_go_to_map),
                        withContentDescription("Icon for button that navigates to Maps from Interactive activities")))
        appCompatImageView2.perform(click())

        Thread.sleep(7000)

        //The if statement forcing a view check, depending on the output of checkMarkers()

        if(checkMarkers("Coin Value")) {
            onView(withId(R.id.maps_open_rates_dialog)).check(matches(isDisplayed()))
        } else {
            onView(withText("FORCED TEST FAIL")).check(matches(isDisplayed()))
        }
    }

    //Bonus Condition = No Bonus
    @Test
    fun noBonusMarkerIcon() {

        Thread.sleep(7000)

        if(checkMarkers("Default")) {
            onView(withId(R.id.maps_open_rates_dialog)).check(matches(isDisplayed()))
        } else {
            onView(withText("FORCED TEST FAIL")).check(matches(isDisplayed()))
        }

    }

    @After
    fun disable2(){
        disableBonus("Coin Currency")
        disableBonus("Coin Value")
    }


    /**
     * The helper function that retrieves the icon of a specified marker, generates the expected icon, given the bonus
     * conditions, and sees whether they are the same with bitmap.sameAs(bitmap)
     */

    private fun checkMarkers(bonusCondition : String) : Boolean {

        val map = mActivityTestRule.activity.provideMapboxMapForTesting()
        val marker = map?.markers!![1]

        var targetBitmap = IconFactory.getInstance(mActivityTestRule.activity).fromResource(
                mActivityTestRule.activity.resources.getIdentifier(
                        "generic_coin", "drawable", mActivityTestRule.activity.packageName)).bitmap


        val featuresCoin = marker.title.toString().split(" ")
        val currency = featuresCoin[1].toLowerCase()
        val value = featuresCoin[2][0]

        val markerBitmap = marker.icon.bitmap

        when (bonusCondition) {
            "Default" -> targetBitmap = IconFactory.getInstance(mActivityTestRule.activity).fromResource(
                    mActivityTestRule.activity.resources.getIdentifier(
                            "generic_coin", "drawable", mActivityTestRule.activity.packageName)).bitmap

            "Coin Value" -> targetBitmap = IconFactory.getInstance(mActivityTestRule.activity).fromResource(
                    mActivityTestRule.activity.resources.getIdentifier(
                            "generic_coin$value", "drawable", mActivityTestRule.activity.packageName)).bitmap

            "Coin Currency" -> targetBitmap = IconFactory.getInstance(mActivityTestRule.activity).fromResource(
                    mActivityTestRule.activity.resources.getIdentifier(
                            currency, "drawable", mActivityTestRule.activity.packageName)).bitmap

            "ValueAndCurrency" -> targetBitmap = IconFactory.getInstance(mActivityTestRule.activity).fromResource(
                    mActivityTestRule.activity.resources.getIdentifier(
                            "$currency$value", "drawable", mActivityTestRule.activity.packageName)).bitmap
        }
        return targetBitmap.sameAs(markerBitmap)
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
