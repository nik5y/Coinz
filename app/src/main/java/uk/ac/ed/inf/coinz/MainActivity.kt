package uk.ac.ed.inf.coinz

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val tag = "MainActivity"

    var tally = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            tally++
            Snackbar.make(view, "Tally is $tally", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        fab1.setOnClickListener { view ->
            tally--
            Snackbar.make(view, "Tally is $tally", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        buttonMaps.setOnClickListener { view ->
            switchToMaps()
        }

    }

    //SWITCH TO MAPS: -----------------------------------------------------------------------------

    fun switchToMaps() {
        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.sign_out_menu -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    //idk: -----------------------------------------------------

    private var downloadDate = "" // Format: YYYY/MM/DD
    private val preferencesFile = "MyPrefsFile" // for storing preferences

    //override fun onCreate(savedInstanceState: Bundle?) {
        // Set up user interface as usual
    //}

    override fun onStart() {
        super.onStart()

       /* //Restore preferences
        val settings = getSharedPreferences(preferencesFile, Context.MODE_PRIVATE)

        //use "" as the default value (this might be the first time the app is run
        downloadDate = settings.getString("lastDownloadDate","")

        //Write a message to "logcat" (for debugging purposes)
        Log.d(tag, "[onStart] Recalled lastDownloadDate is '$downloadDate'")*/
    }

    // Wed Oct 3 Download stuff:

    /*interface DownloadCompleteListener {
        fun downloadComplete(result: String)
    }

    object DownloadCompleteRunner : DownloadCompleteListener {
        var result : String? = null
        override fun downloadComplete(result: String) {
            this.result = result
        }
    }
*/

}
