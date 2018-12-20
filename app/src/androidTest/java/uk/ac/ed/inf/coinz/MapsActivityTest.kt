package uk.ac.ed.inf.coinz


import android.content.Context
import android.support.annotation.VisibleForTesting
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
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class MapsActivityTest {

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
    fun logIn() {
        mAuth.signInWithEmailAndPassword(email,password)
    }

    @Test
    //Tests to see whether the activity opens once the user has signed in
    fun mapActivityOpen() {
        onView(withId(R.id.maps_open_rates_dialog)).check(matches(isDisplayed()))
    }

    @Test
    //Tests to see whether the dialog displaying rates for today opens when the bonus is DISABLED
    fun ratesDialogNotOpenWhenNoBonus() {
        onView(withId(R.id.maps_open_rates_dialog)).perform(click())
        Thread.sleep(5000)
        onView(withId(android.R.id.message)).check(matches(withText(R.string.maps_rates_dialog_no_bonus)))
    }

    @Test
    /* Tests to see whether the button going to the other activity works.
     * The activity has fragments, the default of which is the ShopFragment, hence
     * checking whether shop's layout is displayed
     */
    fun interactiveActivityOpenOnButtonPress() {
        val appCompatImageView = onView(allOf(withId(R.id.maps_go_to_interactive),
                withContentDescription("An icon for the button that navigates from Maps Activity to Interactive Activity"),
                childAtPosition(allOf(withId(R.id.drawer_layout), childAtPosition(withId(android.R.id.content),
                                                0)), 2), isDisplayed()))
        appCompatImageView.perform(click())

        Thread.sleep(5000)

        val imageView = onView(
                allOf(withId(R.id.fragment_shop)))
        imageView.check(matches(isDisplayed()))
    }


    @Test
    /**
     * Tests if the rates dialog opens when the bonus is turned on
     *
     * Tests if the values displayed for the rates in the rate dialog change if the rates change.
     *
     * The test enables the bonus, saves the old, true rate, inserts a test rate, tests,
     * and resets everything back to normal at the end.
     */
    fun checkCorrectRateDisplay() {

        //Prepare before testing
        Thread.sleep(7000)

        enableRatesBonus()

        val targetRate = "DOLR"
        val targetRateValue = "100.0000000"
        val oldRate = getOldSharedPrefs(targetRate)

        setNewSharedPrefs(targetRate, targetRateValue)

        Thread.sleep(3000)

        onView(withId(R.id.maps_open_rates_dialog)).perform(click())

        Thread.sleep(3000)

        onView(withId(R.id.maps_dialog_rates_dolr_value)).check(matches(withText("100.000")))

        //Reset after testing
        disableBonus("Rates")
        setNewSharedPrefs(targetRate, oldRate)

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

    private fun enableRatesBonus() {
        firestore.collection("Users").document(email).collection("Bonuses")
                .document("Rates").update("activated", true)

    }

    @VisibleForTesting
    fun disableBonus(bonus : String) {
        //Restores the bonus back to false
        firestore.collection("Users").document(email).collection("Bonuses")
                .document(bonus).update("activated", false)
    }

    private fun getOldSharedPrefs(key : String) : String {
        val sharedPreferences = mActivityTestRule.activity.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
        return sharedPreferences.getString(key, "default")
    }

    private fun setNewSharedPrefs(key : String, value : String){
        val sharedPreferences = mActivityTestRule.activity.getSharedPreferences("sharedPreferences", Context.MODE_PRIVATE)
        sharedPreferences.edit().putString(key,value).apply()
    }


}
