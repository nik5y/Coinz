@file:Suppress("DEPRECATION")

package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.widget.DrawerLayout
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AlertDialog
import android.util.Log
import android.view.Menu
import android.view.MenuItem
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
import org.joda.time.DateTime
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.time.Duration
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.concurrent.schedule


class MapsActivity : AppCompatActivity(),
        PermissionsListener, LocationEngineListener, OnMapReadyCallback {

    private val tag = "MapsActivity"

    private var mapView: MapView? = null
    private var map: MapboxMap? = null
    private lateinit var permissionManager: PermissionsManager
    private lateinit var originLocation: Location

    private var locationEngine : LocationEngine? = null

    private var locationLayerPlugin: LocationLayerPlugin? = null

    private var mAuth = FirebaseAuth.getInstance()

    private var userEmail : String? = null

    private val firestore = FirebaseFirestore.getInstance()

    private var coinsToRemove : MutableSet<String>? = null

    private var currencyMarkerBonus : Boolean = true
    private var valueMarkerBonus : Boolean = true
    private var coinCollectRangeBonus : Boolean = false

    private var coinCollectRange : Double = 0.0

    private var iconId : Int = 0
/*
    private lateinit var drawer : DrawerLayout
    private lateinit var mToggle : ActionBarDrawerToggle*/

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        setSupportActionBar(toolbar)

        setDailyCoinDelete()

        if (mAuth.currentUser == null) {
            goToLogin()
        }

        userEmail = mAuth.currentUser?.email

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

            mapView = findViewById(R.id.mapView)
            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)

       /* drawer = findViewById(R.id.drawer_layout)

        mToggle = ActionBarDrawerToggle(this, drawer, toolbar,
                R.string.nav_open, R.string.nav_close)

        drawer.addDrawerListener(mToggle)
        mToggle.syncState()*/

        val config = SlidrConfig.Builder()
                .position(SlidrPosition.RIGHT)
                .build()

        Slidr.attach(this, config)

    }

    /*override fun onBackPressed() {

        if(drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }*/


    override fun onMapReady(mapboxMap: MapboxMap?) {

        //DOWNLOADING

        val date = SimpleDateFormat("yyyy/MM/dd").format(Date())
        val mapURL : String = "http://homepages.inf.ed.ac.uk/stg/coinz/" + date + "/coinzmap.geojson"
        val coins : String = DownloadFileTask(DownloadCompleteRunner).execute(mapURL).get()
        val coinFeatures : FeatureCollection = FeatureCollection.fromJson(coins)

        val rates : JSONObject = JSONObject(coins).get("rates") as JSONObject


        map = mapboxMap

        addCoinsToMap(map, coinFeatures )

        //locating

        enableLocation()

        map?.setOnMarkerClickListener {
            coinCollect(it)
            true
        }
    }

    //DOWNLOADER

    interface DownloadCompleteListener {
        fun downloadComplete(result:String)
    }

    object DownloadCompleteRunner : DownloadCompleteListener {
        private var result: String? = null
        override fun downloadComplete(result: String) {
            this.result = result
        }
    }

    class DownloadFileTask(private val caller : DownloadCompleteListener) :
            AsyncTask<String, Void, String>() {
        override fun doInBackground(vararg urls: String): String = try {
            loadFileFromNetwork(urls[0])
        } catch (e: IOException) {
            "Unable to load content. Check your network connection."
        }

        private fun loadFileFromNetwork(urlString:String): String {

            val stream : InputStream = downloadUrl(urlString)
            //read input from stream, build result as a string
            return stream.bufferedReader().use { it.readText() }
        }

        //given a string representation of a url, sets up a connection
        //and gets an input stream
        @Throws(IOException::class)
        private fun downloadUrl(urlString: String): InputStream {
            val url = URL(urlString)
            val conn = url.openConnection() as HttpURLConnection
            conn.apply{
                readTimeout = 10000
                connectTimeout = 15000
                requestMethod = "GET"
                doInput = true
                connect() // starts the query
            }
            return conn.inputStream
        }

        override fun onPostExecute(result: String) {
            super.onPostExecute(result)
            caller.downloadComplete(result)
        }
    }

    private fun enableLocation() {
        if(PermissionsManager.areLocationPermissionsGranted(this)) {
            Log.d(tag, "Permissions are granted")
            initialiseLocationEngine()
            initialiseLocationLayer()
        } else {
            Log.d(tag,"Permissions are not granted")
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
        val lastLocation : Location? = locationEngine?.lastLocation
        if (lastLocation != null) {
            originLocation = lastLocation
            setCameraPosition(lastLocation)
        } else {
            locationEngine?.addLocationEngineListener(this)
        }
    }

    @SuppressWarnings("MissingPermission")
    private fun initialiseLocationLayer() {
        if (mapView == null) { Log.d(tag,"mapView is null") }
        else {
            if (map == null) { Log.d(tag,"map is null") }
            else {
            locationLayerPlugin = LocationLayerPlugin(mapView!!, map!!, locationEngine)
            locationLayerPlugin?.apply{
                setLocationLayerEnabled(true)
                cameraMode = CameraMode.TRACKING
                renderMode = RenderMode.NORMAL
            }}
        }
    }

    private fun setCameraPosition(location: Location) {
        map?.animateCamera(CameraUpdateFactory.newLatLng(
                LatLng(location.latitude, location.longitude)))
    }
    /// Permissions listener:
    override fun onExplanationNeeded(permissionsToExplain: MutableList<String>?) {
        //Present a toast or a dialog explainging why they should provide access
        Log.d(tag,"Permissions: $permissionsToExplain")

    }

    override fun onPermissionResult(granted: Boolean) {
        Log.d(tag,"[onPermissionResult] granted == $granted")

        if (granted) {
            enableLocation()
        } else {
            // Open dialogue for the user go do sudoku or something
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        permissionManager.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    // Locations listener:
    override fun onLocationChanged(location: Location?) {
        if (location == null) {
            Log.d(tag,"[onLocationChanged] location is null")
        } else {
            originLocation = location
            setCameraPosition(location)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag,"[onConnected] requesting location updates")
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
        mapView?.onResume()

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) : Boolean  {
        when (item?.itemId) {
            /*R.id.sign_out_menu -> {
                mAuth.signOut()
                goToLogin()
                return true}*/
            R.id.shop_menu -> {
                goToInteractive()
                return true
            }
        }
        return super.onOptionsItemSelected(item)


        // if(mToggle!!.onOptionsItemSelected(item)){
        // return true
        // }
    }

    @SuppressLint("MissingPermission")
    private fun coinCollect(marker : Marker) {

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

            } else {
                Toast.makeText(this@MapsActivity, "Coin ${(markerPos.distanceTo(currentPos) - coinCollectRange).format(0)}m out of Range!", Toast.LENGTH_LONG).show()
            }
        }
    }
    private fun addCoinToDatabase( coin : MutableMap<String,Any>) {

        val coinReference = firestore.collection("Users").document(userEmail!!).collection("Coins").document("Collected Coins")

            coinReference.set(coin, SetOptions.merge()).addOnCompleteListener {
                Log.d(tag, "Coin added to the Database")
            }.addOnFailureListener {
                Log.d(tag, "Coin NOT added to the Database!")
            }

    }

    private fun createCoinMutableMap(marker: Marker) : MutableMap<String,Any> {
        val featuresCoin = marker.title.toString().split(" ")

        val currValMap : MutableMap<String,String> = mutableMapOf<String,String>()
        currValMap.put("currency",featuresCoin[1])
        currValMap.put("value",featuresCoin[2])

        val coinMap : MutableMap<String,Any> = mutableMapOf()
        coinMap.put(featuresCoin[0], currValMap)

        return coinMap
    }

    private fun addCoinsToMap(map: MapboxMap?, coinFeatures: FeatureCollection) {

        firestore.collection("Users").document(userEmail!!).collection("Coins").document("Collected Coins").get().addOnSuccessListener {

            val hash = it?.data
            coinsToRemove = hash?.keys

            for (i in coinFeatures.features()!!) {

                val geometry = i.geometry() as Point
                val point = geometry.coordinates()
                // val tit = i.getStringProperty("id")
                val coinCurrency = i.getStringProperty("currency")
                val coinId = i.getStringProperty("id")
                val coinValue = i.getStringProperty("value")

                if (currencyMarkerBonus) {
                    if(valueMarkerBonus) {
                        iconId  =  resources.getIdentifier(coinCurrency.toLowerCase()+coinValue[0], "drawable", packageName)
                    } else {
                        iconId  =  resources.getIdentifier(coinCurrency.toLowerCase(), "drawable", packageName)
                    }
                } else {
                    if (valueMarkerBonus) {
                        iconId  =  resources.getIdentifier("generic_coin" + coinValue[0], "drawable", packageName)
                    } else {
                        iconId  =  resources.getIdentifier("generic_coin", "drawable", packageName)
                    }
                }


                if (coinsToRemove == null || !(coinsToRemove!!.contains(coinId))) {

                    map?.addMarker(MarkerOptions()
                            .position(LatLng(point[1], point[0]))
                            .title(coinId + " " + coinCurrency + " " + coinValue)
                            .icon(IconFactory.getInstance(this).fromResource(iconId)))
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


    //Formatting digits
    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)


    //Setting an Alarm to delete coins at midnight
    private fun setDailyCoinDelete(){

        /*val calendar: Calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            //add(Calendar.DAY_OF_MONTH, 1)
        }*/

       val millisUntilTomorrowStart = millisUntilTomorrowStart()

       /* val today =  DateTime().withTimeAtStartOfDay();
        val tomorrow = today.plusDays(1).withTimeAtStartOfDay()*/

        val alarmMan = getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(this,DailyCoinDelete::class.java)

        val pendIntent = PendingIntent.getBroadcast(this,0,intent,PendingIntent.FLAG_CANCEL_CURRENT)

        alarmMan.cancel(pendIntent)

        alarmMan.setRepeating(AlarmManager.RTC, Date().time + millisUntilTomorrowStart , 24*60*60*1000, pendIntent)
        Log.d(tag, "Alarm for daily coin deletion set")

    }

    fun reCreateMap() {

        //Timer used here as the map should be recreated at midnight only if the user is actually playing.
        //If the user is not playing, the map will get changed automatically at next launch

        val millisUntilTomorrowStart = millisUntilTomorrowStart()


        Timer("Deleting Coins", false).schedule(millisUntilTomorrowStart+1000*60) {
            Log.d(tag, "ReCreating Maps for new Coins")
            goToMaps()
        }


    }

    fun goToInteractive(){
        val intent = Intent(this, InteractiveActivity::class.java)

        startActivity(intent)

        overridePendingTransition(R.anim.left_slide_in,R.anim.right_slide_out)
    }

    fun goToMaps() {

        val intent = Intent(this, MapsActivity::class.java)
        startActivity(intent)

    }

    fun millisUntilTomorrowStart() : Long {

        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val today = now.toLocalDate()
        val tomorrow = today.plusDays(1)
        val tomorrowStart = OffsetDateTime.of(
                tomorrow,
                LocalTime.MIN,
                ZoneOffset.UTC
        )
        val d = Duration.between(now, tomorrowStart)
        val millisUntilTomorrowStart = d.toMillis()

        return millisUntilTomorrowStart
    }
}




// http://m.yandex.kz/collections/card/5b6eb2f9a947cc00c1981068/ coin source//
