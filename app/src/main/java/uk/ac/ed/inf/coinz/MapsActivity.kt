@file:Suppress("DEPRECATION")
@file:SuppressLint("LogNotTimber", "SimpleDateFormat")

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
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import com.r0adkll.slidr.model.SlidrPosition
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.coin_recycler_dialog.*
import kotlinx.android.synthetic.main.maps_dialog_rates.*
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.schedule


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

    //private var coinsToRemove: MutableSet<String>? = null

    private var currencyMarkerBonus: Boolean = true
    private var valueMarkerBonus: Boolean = true
    private var coinCollectRangeBonus: Boolean = false

    private var coinCollectRange: Double = 0.0

    private var iconId: Int = 0

    private val SHARED_PREFS = "sharedPreferences"
    private val JSON_MAP = "Map"
    private val DOWNLOAD_DATE = "Download Date"

/*
    private lateinit var drawer : DrawerLayout
    private lateinit var mToggle : ActionBarDrawerToggle*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        //setSupportActionBar(toolbar)

        if (mAuth.currentUser == null) {
            goToLogin()
        }

        dateCreated =  SimpleDateFormat("yyyy/MM/dd").format(Date())

        setDailyCoinDelete()

        userEmail = mAuth.currentUser?.email

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)

        //locating

        //todo so, come up with ways of restarting bonuses, both offline and if the user is playing currently
        //todo look at bonuses in maps
        //perhaps add more bonuses
        //try messenger

        /* drawer = findViewById(R.id.drawer_layout)

        mToggle = ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.nav_open, R.string.nav_close)

        drawer.addDrawerListener(mToggle)
        mToggle.syncState()*/

        val config = SlidrConfig.Builder().position(SlidrPosition.RIGHT).build()

        Slidr.attach(this, config)

        maps_go_to_interactive.setOnClickListener {
            goToInteractive()
        }

        maps_open_rates_dialog.setOnClickListener {
            setupDialog(this).show()
        }


        //testing

        /*val string :String = "s_asdasd_nnnn_cccc_fffff_sssa"
        val res = string.substringAfter("_").substringBefore('_')

       val k = string.replace(res, "hey")

        val i = 1*/

    }


    override fun onMapReady(mapboxMap: MapboxMap?) {

        //DOWNLOADING

        downloadMap()

        val coins = getMap()

        //val date = SimpleDateFormat("yyyy/MM/dd").format(Date())
        // val mapURL : String = "http://homepages.inf.ed.ac.uk/stg/coinz/" + date + "/coinzmap.geojson"
        //val coins : String = DownloadFileTask(DownloadCompleteRunner).execute(mapURL).get()
        val coinFeatures: FeatureCollection = FeatureCollection.fromJson(coins)

        map = mapboxMap

        addCoinsToMap(map, coinFeatures)

        map?.setOnMarkerClickListener {
            coinCollect(it)
            true
        }

        enableLocation()
    }


    //DOWNLOADER


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
                    setLocationLayerEnabled(true)
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
        //Present a toast or a dialog explainging why they should provide access
        Log.d(tag, "Permissions: $permissionsToExplain")

    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag, "[onPermissionResult] granted == $granted")
        if (granted) {
            enableLocation()
        } else {
            // Open dialogue for the user go do sudoku or something

            val alert = AlertDialog.Builder(this)
            alert.apply {

                setPositiveButton("OK", null)
                setCancelable(true)
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


    private fun goToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK.or(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
    }

    //Lifecycle ting:----------------------------------------


    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()

        if (locationEngine != null) {
            locationEngine!!.requestLocationUpdates()
            locationLayerPlugin!!.onStart()
        }

        mapView?.onStart()

    }

    override fun onResume() {
        super.onResume()
        if(dateCreated !=  SimpleDateFormat("yyyy/MM/dd").format(Date())){
            goToMaps()
        }

        mapView?.onResume()

        //put here so that it triggers if a user decided to yada yada
        //doesnt work if somebody left their map opened for over 2 midnight, which is unlikely, yet still a fault
        reCreateMap()

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

    //MENU

 /*   override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        when (item?.itemId) {
        *//*R.id.sign_out_menu -> {
                mAuth.signOut()
                goToLogin()
                return true}*//*
            R.id.shop_menu -> {
                goToInteractive()
                return true
            }
        }
        return super.onOptionsItemSelected(item)


        // if(mToggle!!.onOptionsItemSelected(item)){
        // return true
        // }
    }*/

    @SuppressLint("MissingPermission")
    private fun coinCollect(marker: Marker) {

        if (locationEngine == null || locationEngine!!.lastLocation == null) {

            val alert = AlertDialog.Builder(this)
            alert.apply {

                setPositiveButton("OK", null)
                setCancelable(true)
                setMessage("Please wait until your location is found")
                create().show()
            }

        } else {

            val lastLocation = locationEngine!!.lastLocation

            val markerPos = marker.position
            val currentPos = LatLng(lastLocation.latitude, lastLocation.longitude)

            coinCollectRange = if (coinCollectRangeBonus) {
                50.0
            } else {
                250.0
            }

            if (markerPos.distanceTo(currentPos) <= coinCollectRange) {

                marker.remove()
                val coinMap = createCoinMutableMap(marker)
                addCoinToDatabase(coinMap)
                val currVal = getCoinCurrVal(marker)
                Toast.makeText(this@MapsActivity, "Collected ${currVal[0]} of value ${currVal[1].toDouble().format(3)}", Toast.LENGTH_LONG).show()

            } else {

                Toast.makeText(this@MapsActivity, "Coin ${(markerPos.distanceTo(currentPos) - coinCollectRange).format(0)}m out of Range!", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun addCoinToDatabase(coin: MutableMap<String, Any>) {

        val coinReference = firestore.collection("Users").document(userEmail!!).collection("Coins").document("Collected Coins")

        coinReference.set(coin, SetOptions.merge()).addOnCompleteListener {
            Log.d(tag, "Coin added to the Database")
        }.addOnFailureListener {
            Log.d(tag, "Coin NOT added to the Database!")
        }

    }

    private fun createCoinMutableMap(marker: Marker): MutableMap<String, Any> {
        val featuresCoin = marker.title.toString().split(" ")

        val currValMap: MutableMap<String, String> = mutableMapOf<String, String>().apply {
            put("currency", featuresCoin[1])
            put("value", featuresCoin[2])
            put("collectedBy", userEmail!!)
            put("sentBy", "")
        }

        val coinMap: MutableMap<String, Any> = mutableMapOf()
        coinMap.put(featuresCoin[0], currValMap)

        return coinMap
    }

    private fun getCoinCurrVal(marker: Marker) : Array<String> {
        val featuresCoin = marker.title.toString().split(" ")
        val array : Array<String> = arrayOf(featuresCoin[1],featuresCoin[2])
        return array
    }

    //todo ADD RECEIVED FROM THINGY TO THE COINS

    //todo ADD REALTIME LISTENER TO COIN ADDITION


    private fun addCoinsToMap(map: MapboxMap?, coinFeatures: FeatureCollection) {

        val coinReference = firestore.collection("Users").document(userEmail!!).collection("Coins")

        var coinsToRemove : Iterable<String> = mutableListOf()

        coinReference.document("Collected Coins").get().addOnSuccessListener {collected ->

            //val coinsCollected : Iterable<String> = mutableListOf()

            var coins = collected?.data?.keys

            if(coins!=null){
                coinsToRemove = coinsToRemove.union(coins as Iterable<String>)
               // val i = 1
            }

            coinReference.document("Sent Coins Today").get().addOnSuccessListener {sent ->

                //val coinsSent : Iterable<String> = mutableListOf()

                coins = sent?.data?.keys
                if(coins !=null){
                    coinsToRemove=coinsToRemove.union(coins as Iterable<String>)
                }


                coinReference.document("Banked Coins Today").get().addOnSuccessListener {banked->

                    //val coinsBanked : Iterable<String> = mutableListOf()
                    coins = banked?.data?.keys
                    if (coins!= null){
                        coinsToRemove= coinsToRemove.union(coins as Iterable<String>)
                    }

                    //coinsToRemove.union(coinsBanked).union(coinsCollected).union(coinsSent)

                    for (i in coinFeatures.features()!!) {

                        val geometry = i.geometry() as Point
                        val point = geometry.coordinates()
                        // val tit = i.getStringProperty("id")
                        val coinCurrency = i.getStringProperty("currency")
                        val coinId = i.getStringProperty("id")
                        val coinValue = i.getStringProperty("value")

                        if (currencyMarkerBonus) {
                            if (valueMarkerBonus) {
                                iconId = resources.getIdentifier(coinCurrency.toLowerCase() + coinValue[0], "drawable", packageName)
                            } else {
                                iconId = resources.getIdentifier(coinCurrency.toLowerCase(), "drawable", packageName)
                            }
                        } else {
                            if (valueMarkerBonus) {
                                iconId = resources.getIdentifier("generic_coin" + coinValue[0], "drawable", packageName)
                            } else {
                                iconId = resources.getIdentifier("generic_coin", "drawable", packageName)
                            }
                        }

                        if (!(coinsToRemove.contains(coinId))) {
                            map?.addMarker(MarkerOptions().position(LatLng(point[1], point[0])).title(coinId + " " + coinCurrency + " " + coinValue).icon(IconFactory.getInstance(this).fromResource(iconId)))
                        }
                    }

                    Log.d(tag, "Added Coins to Map")

                    //map?.getUiSettings()?.setRotateGesturesEnabled(false)
                    //map?.getUiSettings()?.setLogoGravity(Gravity.BOTTOM | Gravity.END);
                    //map?.getUiSettings()?.setLogoEnabled(true);
                    map?.getUiSettings()?.setAttributionEnabled(false)
                    map?.getUiSettings()?.setZoomControlsEnabled(true)
                }

             }
        }
    }

    //Setting an Alarm to delete coins at midnight
    private fun setDailyCoinDelete() {

        /*val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            //add(Calendar.DAY_OF_MONTH, 1)
        }*/

        val millisUntilTomorrowStart = Timing().millisUntilTomorrowStart()

        /* val today =  DateTime().withTimeAtStartOfDay();
        val tomorrow = today.plusDays(1).withTimeAtStartOfDay()*/

        val alarmMan = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this, DailyCoinDelete::class.java)

        val pendIntent = PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)

        alarmMan.cancel(pendIntent)

        alarmMan.setRepeating(AlarmManager.RTC, Date().time + millisUntilTomorrowStart, 24 * 60 * 60 * 1000, pendIntent)
        Log.d(tag, "Alarm for daily coin deletion set")

    }

    fun reCreateMap() {

        val millisUntilTomorrowStart = Timing().millisUntilTomorrowStart()

        Timer("Deleting Coins", false).schedule(millisUntilTomorrowStart + 1000 * 60) {
            Log.d(tag, "ReCreating Maps for new Coins")
            goToMaps()
        }


    }

    fun goToInteractive() {
        val intent = Intent(this, InteractiveActivity::class.java)

        startActivity(intent)

        overridePendingTransition(R.anim.right_slide_in, R.anim.left_slide_out)
    }

    fun goToMaps() {

        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)

    }

    private fun downloadMap() {

        val date: String = SimpleDateFormat("yyyy/MM/dd").format(Date())
        val mapURL: String = "http://homepages.inf.ed.ac.uk/stg/coinz/" + date + "/coinzmap.geojson"
        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()


        if (sharedPreferences.getString(DOWNLOAD_DATE, "dik") != date) {
            Log.d(tag, "Downloading and storing map for $date")
            val coins  = DownloadFileTask(DownloadCompleteRunner).execute(mapURL).get()
            editor.putString(DOWNLOAD_DATE, date)
            editor.putString(JSON_MAP, coins)

            //get rates and put them in shared prefs as well

            val rates = JSONObject(coins).get("rates") as JSONObject
            Log.d(tag, "Storing rates in $SHARED_PREFS")
            for (i in rates.keys()) {
                val value = rates.get(i).toString()
                editor.putString(i, value)
            }
            editor.apply()

        } else {
            Log.d(tag, "Map for $date already downloaded at $SHARED_PREFS")
        }


    }

    private fun getMap(): String {

        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)

        return sharedPreferences.getString(JSON_MAP, "No Map")
    }

    fun setupDialog(context: Context) : Dialog {
        val dialog  = Dialog(context)
        val sharedPreferences = getSharedPreferences(SHARED_PREFS, Context.MODE_PRIVATE)
        dialog.setContentView(R.layout.maps_dialog_rates)
        dialog.window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        dialog.maps_dialog_rates_dolr_title.setText("DOLR")
        dialog.maps_dialog_rates_dolr_value.setText(sharedPreferences.getString("DOLR", "no rate").toDouble().format(3))
        dialog.maps_dialog_rates_quid_title.setText("QUID")
        dialog.maps_dialog_rates_quid_value.setText(sharedPreferences.getString("QUID", "no rate").toDouble().format(3))
        dialog.maps_dialog_rates_peny_title.setText("PENY")
        dialog.maps_dialog_rates_peny_value.setText(sharedPreferences.getString("PENY", "no rate").toDouble().format(3))
        dialog.maps_dialog_rates_shil_title.setText("SHIL")
        dialog.maps_dialog_rates_shil_value.setText(sharedPreferences.getString("SHIL", "no rate").toDouble().format(3))
        return dialog
    }

}










// http://m.yandex.kz/collections/card/5b6eb2f9a947cc00c1981068/ coin source//
