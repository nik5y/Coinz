package uk.ac.ed.inf.coinz


import android.support.test.espresso.Espresso.onView
import android.support.test.espresso.action.ViewActions.*
import android.support.test.espresso.assertion.ViewAssertions.matches
import android.support.test.espresso.matcher.ViewMatchers.*
import android.support.test.filters.LargeTest
import android.support.test.rule.ActivityTestRule
import android.support.test.runner.AndroidJUnit4
import android.view.View
import android.view.ViewGroup
import org.hamcrest.Description
import org.hamcrest.Matcher
import org.hamcrest.Matchers.allOf
import org.hamcrest.TypeSafeMatcher
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@LargeTest
@RunWith(AndroidJUnit4::class)
class LoginActivityTest {

    @Rule
    @JvmField
    var mActivityTestRule = ActivityTestRule(LoginActivity::class.java)

    /**
     * The tests below test different combinations of user input, to see if the LoginActivity works as intended
     * and provides informative and accurate feedback
     *
     * Also check to see if it is robust, as FirebaseAuth causes a crash if the email and passwords input are empty.
     */

    @Test
    fun successfulLoginTest() {
        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText("tp@tp.com"), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText("tptptp"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        onView(withId(R.id.maps_open_rates_dialog)).check(matches(isDisplayed()))
    }


    @Test
    fun nonExistingUserTest() {

        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText("qq@qq.com"), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText("q"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        val textView = onView(
                allOf(withId(android.R.id.message), withText("There is no user record corresponding to this identifier. The user may have been deleted."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.scrollView),
                                        0),
                                1),
                        isDisplayed()))
        textView.check(matches(withText("There is no user record corresponding to this identifier. The user may have been deleted.")))

    }

    @Test
    fun noEmailNoPasswordTest() {

        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText(""), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText(""), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        val textView = onView(
                allOf(withId(android.R.id.message), withText("Please fill out the forms."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.scrollView),
                                        0),
                                1),
                        isDisplayed()))
        textView.check(matches(withText("Please fill out the forms.")))

    }

    @Test
    fun invalidEmailTest() {

        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText("qqqqq@qqq@com"), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText("qqqqqq"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        val textView = onView(
                allOf(withId(android.R.id.message), withText("The email address is badly formatted."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.scrollView),
                                        0),
                                1),
                        isDisplayed()))
        textView.check(matches(withText("The email address is badly formatted.")))

    }

    @Test
    fun invalidPasswordTest() {

        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText("tp@tp.com"), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText("qqqqqq"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        val textView = onView(
                allOf(withId(android.R.id.message)))
        textView.check(matches(withText("The password is invalid or the user does not have a password.")))

    }

    @Test
    fun noEmailTest() {

        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText(""), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText("qqqqqq"), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        val textView = onView(
                allOf(withId(android.R.id.message), withText("Please fill out the forms."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.scrollView),
                                        0),
                                1),
                        isDisplayed()))
        textView.check(matches(withText("Please fill out the forms.")))

    }

    @Test
    fun noPasswordTest() {

        val appCompatEditText = onView(
                allOf(withId(R.id.loginEmail), childAtPosition(childAtPosition(withId(android.R.id.content), 0), 0), isDisplayed()))
        appCompatEditText.perform(replaceText("qq@qq.com"), closeSoftKeyboard())

        val appCompatEditText8 = onView(
                allOf(withId(R.id.loginPassword),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                1),
                        isDisplayed()))
        appCompatEditText8.perform(replaceText(""), closeSoftKeyboard())

        val appCompatButton = onView(
                allOf(withId(R.id.loginButton), withText("Log in"),
                        childAtPosition(
                                childAtPosition(
                                        withId(android.R.id.content),
                                        0),
                                2),
                        isDisplayed()))
        appCompatButton.perform(click())

        Thread.sleep(2000)

        val textView = onView(
                allOf(withId(android.R.id.message), withText("Please fill out the forms."),
                        childAtPosition(
                                childAtPosition(
                                        withId(R.id.scrollView),
                                        0),
                                1),
                        isDisplayed()))
        textView.check(matches(withText("Please fill out the forms.")))

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
