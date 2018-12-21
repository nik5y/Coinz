@file:Suppress("DEPRECATION")
@file:SuppressLint("LogNotTimber")

package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Dialog
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.location.Location
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.annotation.VisibleForTesting
import android.support.v7.app.AlertDialog
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.*
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.IconFactory
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.maps_dialog_rates.*
import org.json.JSONObject
import java.util.*


class MapsActivity : AppCompatActivity(),
        PermissionsListener, LocationEngineListener, OnMapReadyCallback {

    private val tag = "MapsActivity"
    private lateinit var dateCreated : String

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location

    private var locationEngine: LocationEngine? = null

    private var locationLayerPlugin: LocationLayerPlugin? = null

    private var mAuth = FirebaseAuth.getInstance()

    private var userEmail: String? = null

    private val firestore = FirebaseFirestore.getInstance()

    private var currencyMarkerBonus: Boolean = true
    private var valueMarkerBonus: Boolean = true
    private var coinCollectRangeBonus: Boolean = false
    private var rateDialogBonus : Boolean = false

    private var coinCollectRange: Double = 0.0

    private var iconId: Int = 0

    private val sharedPREFS = "sharedPreferences"
    private val jsonMAP = "Map"
    private val downloadDATE = "Download Date"

    private lateinit var alert : AlertDialog.Builder

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        //Check if the user is signed in, if not, go to LoginAcitivity

        if (mAuth.currentUser == null) {
            goToLogin()
        }

        //Set the dateCreated variable to today, to take note when the activity was created

        dateCreated =  todayYMD()

        //initialise alert dialog used throughout

        alert = AlertDialog.Builder(this)
        alert.apply {
            setPositiveButton("OK", null)
            setCancelable(true)
        }

        //set up an alarm manager for sent and banked coin deletion

        setDailyCoinDelete()

        //take note of the currently signed in user's email, used throughout the activity

        userEmail = mAuth.currentUser?.email

        //initialise the map and set up the coins and their collection

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        //initialise the onClickListener allowing to go to InteractiveActivity

        maps_go_to_interactive.setOnClickListener {
            goToInteractive()
        }

        //initialise the onClickListener allowing to go see the rates dialog, if the bonus was bought

        maps_open_rates_dialog.setOnClickListener {

            firestore.collection("Users").document(userEmail!!).collection("Bonuses")
                    .document("Rates").get().addOnSuccessListener { ratesBonus ->

                        rateDialogBonus = ratesBonus["activated"] as Boolean
                        if (rateDialogBonus) {
                            setupDialog(this).show()
                        } else {
                            alert.setMessage("Rate List unavailable! Buy the Bonus!").create().show()
                        }
                    }
        }
    }

    override fun onMapReady(mapboxMap: MapboxMap?) {

        //DOWNLOADING MAP

        downloadMap()
        val coins = getMap()
        val coinFeatures: FeatureCollection = FeatureCollection.fromJson(coins)

        map = mapboxMap

        //Adding coins to map

        addCoinsToMap(map, coinFeatures)

        //Enabling location

        enableLocation()

        //Setting up the coin collection

        map?.setOnMarkerClickListener {
            coinCollect(it)
            true
        }
    }

    private fun enableLocation() {
        if (PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag, "Permissions are not granted")
            permissionManager = PermissionsManager(this)
            permissionManager.requestLocationPermissions(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationEngine() {

        locationEngine = LocationEngineProvider(this).obtainBestLocationEngineAvailable()
        locationEngine?.apply {
            interval = 5000
            fastestInterval = 1000
            priority = LocationEnginePriority.HIGH_ACCURACY
            activate()
        }
        val lastLocation: Location? = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        if (mapView == null) {
            Log.d(tag, "mapView is null")
        } else {
            if (map == null) {
                Log.d(tag, "map is null")
            } else {
                locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
                locationLayerPlugin?.apply {
                    isLocationLayerEnabled = true
                    cameraMode = CameraMode.TRACKING
                    renderMode = RenderMode.NORMAL
                }
            }
        }
    }

    private fun setCameraPosition(location: Location) {
        map?.animateCamera(CameraUpdateFactory.newLatLng(LatLng(location.latitude, location.longitude)))
    }

    /// Permissions listener:
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        //Present a toast or a dialog explaining why they should provide access
        Log.d(tag, "Permissions: $permissionsToExplain")

    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open dialogue for the user go do sudoku or something (c) Stephen Gilmore
            alert.apply {
                setMessage("Location Permission Denied. In order to play the game," +
                        " location services have to be turned on!")
                create().show()
            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Locations listener:
    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag, "[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(location)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag, "[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }

    //Intent that apart from going to LoginActivity, ensures that if the back button was clicked in the next activity,
    //the application would not go back to this activity, but instead would close.

    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    //<LIFECYCLES>

    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()

        //making sure the location engine is on

        if (locationEngine != null) {
            locationEngine!!.requestLocationUpdates()
            locationLayerPlugin!!.onStart()
        }

        mapView?.onStart()

    }

    override fun onResume() {
        super.onResume()

        //if the user was to pause the activity when midnight hit, the map would still get recreated when the user comes back

        if(dateCreated !=  todayYMD()){
            reCreateMap()
            Log.d(tag, "[onResume] Recreating Map for ${todayYMD()}")
        }

        mapView?.onResume()

    }

    override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onStop() {
        super.onStop()
        locationEngine?.removeLocationUpdates()
        locationLayerPlugin?.onStop()
        mapView?.onStop()
    }


    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
        locationEngine?.deactivate()
    }

    override fun onSaveInstanceState(outState: Bundle?) {
        super.onSaveInstanceState(outState)
        if (outState != null) {
            mapView?.onSaveInstanceState(outState)
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    //</LIFECYCLES>

    @SuppressLint("MissingPermission")
    @VisibleForTesting
    fun coinCollect(marker: Marker) {

        /**
         * checking if the map was created today or not.
         * this check is here to make sure the user is unable to collect coins from previous days,
         * in the situation where the map activity was open when midnight hit
         */


        if (dateCreated != todayYMD()) {
            reCreateMap()

            /**
             *  additionally, checks if the location engine has been initialised, as it is an asynchronous task
             *  and sometimes the coins are displayed before the location is established
             */

        } else if (locationEngine == null || locationEngine!!.lastLocation == null) {
            alert.apply {
                setMessage("Please wait until your location is found")
                create().show()
            }

        } else {

            //Checking Levels and the corresponding maximum amount of coins able to collect per day

            val userReference = firestore.collection("Users").document(userEmail!!)

            userReference.collection("Account Information").document("Level")
                    .get().addOnSuccessListener { level ->
                userReference.collection("Account Information").document("Collected Coin Counter")
                        .get().addOnSuccessListener { count ->

                            val allowedCoinsToCollect = LevelingSystem().allowedCoinCollect(level["level"].toString().toInt())

                            val coinsCollectedToday = count["count"].toString().toInt()

                            //Checks to see if the user is still allowed to collect coins today

                            if(coinsCollectedToday < allowedCoinsToCollect) {

                                //Take note of current position and markers position

                                val lastLocation = locationEngine!!.lastLocation
                                val markerPos = marker.position
                                val currentPos = LatLng(lastLocation.latitude, lastLocation.longitude)

                                //Check to see if the augmented range bonus is activated

                                userReference.collection("Bonuses")
                                        .document("Range+").get().addOnSuccessListener {

                                            if(it["activated"] as Boolean) {
                                                val expiration = it["expires"] as Date
                                                coinCollectRangeBonus = if(expiration <= Date()) {
                                                    firestore.collection("Users").document(userEmail!!).collection("Bonuses")
                                                            .document("Range+").update("activated", false)
                                                    false
                                                } else {
                                                    true
                                                }
                                            }

                                        }.addOnCompleteListener {

                                            //If activated, augment range, if not, set to default

                                            coinCollectRange = if (coinCollectRangeBonus) {
                                                50.0
                                            } else {
                                                25.0
                                            }

                                            //If in range, remove from map, store in database, and add to collected counter
                                            //Additionally, display the attributes of the collected coin in a Toast

                                            if (markerPos.distanceTo(currentPos) <= coinCollectRange) {

                                                marker.remove()
                                                val coinMap = createCoinMutableMap(marker)

                                                addCoinToDatabase(coinMap)
                                                addToCoinCounter()

                                                val currVal = getCoinCurrVal(marker)
                                                Toast.makeText(this@MapsActivity, "Collected ${currVal[0]} of value ${currVal[1].toDouble().format(3)}", Toast.LENGTH_LONG).show()

                                            } else {

                                                //if not in range, display how much out of range is the user from the coin

                                                Toast.makeText(this@MapsActivity, "Coin ${(markerPos.distanceTo(currentPos) - coinCollectRange).format(0)}m out of Range!", Toast.LENGTH_LONG).show()
                                            }
                                        }
                            }

                            else {

                                //if collected the max amount corresponding to the users level, display an alert notifying that

                                alert.setMessage("You have collected the maximum amount of coins for today ($allowedCoinsToCollect)! Level up to collect more!").create().show()
                            }
                        }
            }
        }
    }

    //Adds coin to the database that is represented as a nested mutable map


    private fun addCoinToDatabase(coin: MutableMap<String, Any>) {

        val coinReference = firestore.collection("Users").document(userEmail!!).collection("Coins").document("Collected Coins")

        coinReference.set(coin, SetOptions.merge()).addOnCompleteListener {
            Log.d(tag, "Coin added to the Database")
        }.addOnFailureListener {
            Log.d(tag, "Coin NOT added to the Database!")
        }

    }

    //Creates a nested mutable map for coins using the information stored in the marker

    private fun createCoinMutableMap(marker: Marker): MutableMap<String, Any> {
        val featuresCoin = marker.title.toString().split(" ")

        val currValMap: MutableMap<String, String> = mutableMapOf<String, String>().apply {
            put("currency", featuresCoin[1])
            put("value", featuresCoin[2])
            put("collectedBy", userEmail!!)
            put("sentBy", "")
        }

        val coinMap: MutableMap<String, Any> = mutableMapOf()
        coinMap[featuresCoin[0]] = currValMap

        return coinMap
    }

    //Retrieves information about the coin that is manually stored in the marker's title

    private fun getCoinCurrVal(marker: Marker) : Array<String> {
        val featuresCoin = marker.title.toString().split(" ")
        return arrayOf(featuresCoin[1],featuresCoin[2])
    }

    /**
     * Adds markers (coins) to the map. Checks over Collected, Sent and Banked Coin Databases in order for the coin to
     * not reappear on the map, once it has been collected/sent/banked.
     * Additionally, checks over the Bonuses that affect marker appearance, to see if they are activated.
     * Depending on the combination of bonuses (or their absence) the icons for each coin will change accordingly
     */

    private fun addCoinsToMap(map: MapboxMap?, coinFeatures: FeatureCollection) {

        val userReference = firestore.collection("Users").document(userEmail!!)
        val coinReference = userReference.collection("Coins")

        //Initialises an Iterable to store the IDs of the coins that are not supposed to be shown on the map anymore

        var coinsToRemove : Iterable<String> = mutableListOf()

        //Checks over Collected, Sent, Banked Databases and adds the coins to the Iterable

        coinReference.document("Collected Coins").get().addOnSuccessListener {collected ->

            var coins = collected?.data?.keys
            if(coins!=null){
                coinsToRemove = coinsToRemove.union(coins as Iterable<String>)
            }

            coinReference.document("Sent Coins Today").get().addOnSuccessListener {sent ->

                coins = sent?.data?.keys
                if(coins !=null){
                    coinsToRemove=coinsToRemove.union(coins as Iterable<String>)
                }

                coinReference.document("Banked Coins Today").get().addOnSuccessListener {banked->

                    coins = banked?.data?.keys
                    if (coins!= null){
                        coinsToRemove= coinsToRemove.union(coins as Iterable<String>)
                    }

                    //Check over Bonuses affecting marker appearance.

                    userReference.collection("Bonuses").document("Coin Currency").get().addOnSuccessListener {currencyBonus->
                        currencyMarkerBonus = currencyBonus["activated"] as Boolean
                    }.addOnCompleteListener {_->
                        userReference.collection("Bonuses").document("Coin Value").get().addOnSuccessListener {valueBonus ->
                            valueMarkerBonus = valueBonus["activated"] as Boolean
                        }.addOnCompleteListener {_->

                            //Loop over all features (coins) in the provided feature collection of the json map and retrieve their IDs

                            for (i in coinFeatures.features()!!) {

                                val coinId = i.getStringProperty("id")

                                //if the retrieved id is not in coinsToRemove, continue

                                if (!(coinsToRemove.contains(coinId))) {

                                    val geometry = i.geometry() as Point
                                    val point = geometry.coordinates()
                                    val coinCurrency = i.getStringProperty("currency")
                                    val coinValue = i.getStringProperty("value")

                                    //Depending on different combination of marker appearance bonuses, assign different icons to them

                                    iconId = if (currencyMarkerBonus) {
                                        if (valueMarkerBonus) {
                                            resources.getIdentifier(coinCurrency.toLowerCase() + coinValue[0], "drawable", packageName)
                                        } else {
                                            resources.getIdentifier(coinCurrency.toLowerCase(), "drawable", packageName)
                                        }
                                    } else {
                                        if (valueMarkerBonus) {
                                            resources.getIdentifier("generic_coin" + coinValue[0], "drawable", packageName)
                                        } else {
                                            resources.getIdentifier("generic_coin", "drawable", packageName)
                                        }
                                    }

                                    /**
                                     * add the marker to the map with the extracted position, given icon, and a custom title containing
                                     * coin attributes that are further used for coin collection
                                     */

                                    map?.addMarker(MarkerOptions().position(LatLng(point[1], point[0])).title("$coinId $coinCurrency $coinValue")
                                            .icon(IconFactory.getInstance(this).fromResource(iconId)))
                                }
                            }

                            Log.d(tag, "Added Coins to Map")

                        }
                    }

                    //altering minor functionality of the mapview

                    map?.uiSettings?.isAttributionEnabled = false
                    map?.uiSettings?.isZoomControlsEnabled = true
                }

             }
        }
    }

    //Setting an Alarm to delete coins at midnight

    @VisibleForTesting
    fun setDailyCoinDelete() {

        val millisUntilTomorrowStart = Timing().millisUntilTomorrowStart()
        val alarmMan = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, DailyCoinDelete::class.java)
        val pendIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        alarmMan.cancel(pendIntent)

        alarmMan.setRepeating(AlarmManager.RTC, Date().time + millisUntilTomorrowStart, 24 * 60 * 60 * 1000, pendIntent)
        Log.d(tag, "Alarm for daily coin deletion set")

    }

    private fun reCreateMap() {

        //Restart Coin Counters:

        val path = firestore.collection("Users").document(userEmail!!)

        path.collection("Account Information").document("Banked Coin Counter")
                .get().addOnSuccessListener { _ ->
                    path.collection("Account Information").document("Banked Coin Counter")
                            .set(CoinCounter())
                }.addOnCompleteListener {
                    Log.d(tag, "[recreateMap] Banked Coin Counter Restarted")
                }.addOnFailureListener {
                    Log.d(tag, "[recreateMap] Banked Coin Counter NOT Restarted")
                }


        path.collection("Account Information").document("Collected Coin Counter")
                .get().addOnSuccessListener { _ ->
                    path.collection("Account Information").document("Collected Coin Counter")
                            .set(CoinCounter())
                }.addOnCompleteListener {
                    Log.d(tag, "[recreateMap] Collected Coin Counter Restarted")
                }.addOnFailureListener {
                    Log.d(tag, "[recreateMap] Collected Coin Counter NOT Restarted")
                }


        //Reset Bonuses that wear off at midnight:

        path.collection("Bonuses").document("Coin Currency").apply {
            update("activated", false)
            update("updated", todayYMD())
        }
        path.collection("Bonuses").document("Coin Value").apply {
            update("activated", false)
            update("updated", todayYMD())
        }
        path.collection("Bonuses").document("Rates").apply {
            update("activated", false)
            update("updated", todayYMD())
        }

        //coin delete in case the user clicks on a coin after midnight but before the alarm has time to delete the coins.
        //as this method restarts the activity, the alarm would also get reset, thus not deleting the coins as needed.

        path.collection("Coins").document("Sent Coins Today").delete().addOnCompleteListener {
            Log.d(tag, "[recreateMap] Sent Coins Today Deleted")
        }.addOnFailureListener {
            Log.d(tag, "[recreateMap] Sent Coins Today NOT Deleted")
        }

        path.collection("Coins").document("Banked Coins Today").delete().addOnCompleteListener {
            Log.d(tag, "[recreateMap] Banked Coins Today Deleted")
        }.addOnFailureListener {
            Log.d(tag, "[recreateMap] Banked Coins Today NOT Deleted")
        }

        //RESTART ACTIVITY

        goToMaps()

        Toast.makeText(this, "Restarted Map for ${todayYMD()}", Toast.LENGTH_LONG).show()

    }

    //intent to open the interactive activity. additionally, overriden the transition animation with custom anim xmls.

    private fun goToInteractive() {
        val intent = Intent(this, InteractiveActivity::class.java)

        startActivity(intent)

        overridePendingTransition(R.anim.right_slide_in, R.anim.left_slide_out)
    }

    //intent to go to MapsActivity, i.e. restart the activity

    private fun goToMaps() {

        val intent = Intent(this, MapsActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)

    }

    /**
     * Checks whether the map has been downloaded for today.
     * if NOT, downloads the map with the provided downloader interface
     * and stores it, along side the extracted rates in Shared Preferences
     *
     * If the map has already been downloaded today, the function does nothing of significance.
     */

    private fun downloadMap() {

        val date: String = todayYMD()
        val sharedPreferences = getSharedPreferences(sharedPREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()

        /**
         * compares todays value to the downloadDATE in shared preferences
         */

        if (sharedPreferences.getString(downloadDATE, "default") != date) {
            Log.d(tag, "Downloading and storing map for $date")
            val mapURL = "http://homepages.inf.ed.ac.uk/stg/coinz/$date/coinzmap.geojson"
            val coins  = DownloadFileTask(DownloadCompleteRunner).execute(mapURL).get()
            editor.putString(downloadDATE, date)
            editor.putString(jsonMAP, coins)

            //get rates and put them in shared prefs as well

            val rates = JSONObject(coins).get("rates") as JSONObject
            Log.d(tag, "Storing rates in $sharedPREFS")
            for (i in rates.keys()) {
                val value = rates.get(i).toString()
                editor.putString(i, value)
            }
            editor.apply()

        } else {
            Log.d(tag, "Map for $date already downloaded at $sharedPREFS")
        }


    }

    //Returns map from shared preferences

    private fun getMap(): String {

        val sharedPreferences = getSharedPreferences(sharedPREFS, Context.MODE_PRIVATE)

        return sharedPreferences.getString(jsonMAP, "No Map")
    }


    //Sets up the dialog containing the information about rates for today

    @SuppressLint("SetTextI18n")
    fun setupDialog(context: Context) : Dialog {
        val dialog  = Dialog(context)
        val sharedPreferences = getSharedPreferences(sharedPREFS, Context.MODE_PRIVATE)
        dialog.setContentView(R.layout.maps_dialog_rates)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.maps_dialog_rates_dolr_title.text = "DOLR"
        dialog.maps_dialog_rates_dolr_value.text = sharedPreferences.getString("DOLR", "no rate").toDouble().format(3)
        dialog.maps_dialog_rates_quid_title.text = "QUID"
        dialog.maps_dialog_rates_quid_value.text = sharedPreferences.getString("QUID", "no rate").toDouble().format(3)
        dialog.maps_dialog_rates_peny_title.text = "PENY"
        dialog.maps_dialog_rates_peny_value.text = sharedPreferences.getString("PENY", "no rate").toDouble().format(3)
        dialog.maps_dialog_rates_shil_title.text = "SHIL"
        dialog.maps_dialog_rates_shil_value.text = sharedPreferences.getString("SHIL", "no rate").toDouble().format(3)

        return dialog
    }

    //adds 1 to the counter of collected coins, once a coin was successfully collected

    private fun addToCoinCounter() {

        val coinCounterPath = firestore.collection("Users").document(userEmail!!)
                .collection("Account Information").document("Collected Coin Counter")

        coinCounterPath.get().addOnSuccessListener { count ->

                val newCount = count.get("count").toString().toInt() + 1
                coinCounterPath.set(CoinCounter(newCount)).addOnCompleteListener {
                    Log.d(tag, "Collected Coin Counter Updated")
                }.addOnFailureListener {
                    Log.d(tag, "Collected Coin Counter NOT Updated")
                }

        }
    }

    @VisibleForTesting
    fun provideMapboxMapForTesting() : MapboxMap? {
        return map
    }

}

// http://m.yandex.kz/collections/card/5b6eb2f9a947cc00c1981068/ coin source//
