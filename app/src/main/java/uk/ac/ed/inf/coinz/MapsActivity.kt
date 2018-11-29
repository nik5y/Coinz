@file:Suppress("DEPRECATION")

package uk.ac.ed.inf.coinz

import android.annotation.SuppressLint
import android.content.Intent
import android.location.Location
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.google.android.gms.common.api.Api
import com.google.android.gms.tasks.Task
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
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

            mapView = findViewById(R.id.mapView)
            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)

    }


    override fun onMapReady(mapboxMap: MapboxMap?) {

        //DOWNLOADING

        val date = SimpleDateFormat("yyyy/MM/dd").format(Date())
        val mapURL : String = "http://homepages.inf.ed.ac.uk/stg/coinz/" + date + "/coinzmap.geojson"
        val coins : String = DownloadFileTask(DownloadCompleteRunner).execute(mapURL).get()
        val coinFeatures : FeatureCollection = FeatureCollection.fromJson(coins)

        val rates : JSONObject = JSONObject(coins).get("rates") as JSONObject


        map = mapboxMap

        addCoinsToMap(userEmail!!, map, coinFeatures )

        //locating

        enableLocation()

        map?.setOnMarkerClickListener {
            coinCollect(it, coinCollectRangeBonus)
            true
        }
    }

    //DOWNLOADER

    interface DownloadCompleteListener {
        fun downloadComplete(result:String)
    }

    object DownloadCompleteRunner : DownloadCompleteListener {
        var result: String? = null
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
            val result : String = stream.bufferedReader().use { it.readText() }

            return result
        }

        //given a string representation of a url, sets up a connection
        //and gets an input stream
        @Throws(IOException::class)
        private fun downloadUrl(urlString: String): InputStream {
            var url = URL(urlString)
            var conn = url.openConnection() as HttpURLConnection
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

    fun enableLocation() {
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


    fun goToLogin() {
        val intent: Intent = Intent(this, LoginActivity::class.java)
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

        if (mAuth.currentUser == null) {
            goToLogin()
        }

        userEmail = mAuth.currentUser?.email

        scheduleInit()

    }

    override fun onResume() {
        super.onResume()
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

    //MENU

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?) : Boolean  {
        when (item?.itemId) {
            R.id.sign_out_menu -> {
                mAuth.signOut()
                goToLogin()
                return true}
        }
        return super.onOptionsItemSelected(item)
    }

    @SuppressLint("MissingPermission")
    private fun coinCollect(marker : Marker, coinCollectRangeBonus : Boolean) {

        val lastLocation = locationEngine!!.lastLocation

        val markerPos = marker.position
        val currentPos = LatLng(lastLocation.latitude, lastLocation.longitude)

        if(coinCollectRangeBonus) {
            coinCollectRange = 50.0
        } else {
            coinCollectRange = 250.0
        }

        if(markerPos.distanceTo(currentPos) <= coinCollectRange) {

            marker.remove()

            val coinMap = createCoinMap(marker)

            addCoinToDatabase(userEmail!!, coinMap)

        } else {
            Toast.makeText(this@MapsActivity, "Coin ${(markerPos.distanceTo(currentPos) - coinCollectRange).
                    format(0)}m out of Range!", Toast.LENGTH_LONG).show()
        }
    }

    private fun addCoinToDatabase(email : String, coin : MutableMap<String,Any>) {

        val coinReference = firestore.collection("Users").document(userEmail!!).collection("Coins").document("Collected Coins")

            coinReference.set(coin, SetOptions.merge()).addOnCompleteListener {
                Log.d(tag, "Coin added to the Database")
            }.addOnFailureListener {
                Log.d(tag, "Coin NOT added to the Database!")
            }

    }

    private fun createCoinMap(marker: Marker) : MutableMap<String,Any> {
        val featuresCoin = marker.title.toString().split(" ")

        var currValMap : MutableMap<String,String> = mutableMapOf<String,String>()
        currValMap.put("currency",featuresCoin[1])
        currValMap.put("value",featuresCoin[2])

        val coinMap : MutableMap<String,Any> = mutableMapOf()
        coinMap.put(featuresCoin[0], currValMap)

        return coinMap
    }

    private fun addCoinsToMap(email : String, map: MapboxMap?, coinFeatures: FeatureCollection) {

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

            //map?.getUiSettings()?.setRotateGesturesEnabled(false)
            //map?.getUiSettings()?.setLogoGravity(Gravity.BOTTOM | Gravity.END);
            //map?.getUiSettings()?.setLogoEnabled(true);
            map?.getUiSettings()?.setAttributionEnabled(false)
            map?.getUiSettings()?.setZoomControlsEnabled(true)
        }

    }

    fun Double.format(digits: Int) = java.lang.String.format("%.${digits}f", this)

    fun scheduleInit() {
        //SCHEDULE

        val scheduledExecutorService : ScheduledExecutorService = Executors.newScheduledThreadPool(1)
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

        //scheduledExecutorService.schedule(dailyUpdate(), 0, TimeUnit.SECONDS)

        //scheduledExecutorService.shutdown()

        scheduledExecutorService.scheduleAtFixedRate(dailyUpdate(), millisUntilTomorrowStart ,  TimeUnit.DAYS.toMillis( 1 ) ,  TimeUnit.MILLISECONDS  )
    }

    /*fun deleteCollection(collection : CollectionReference, batchSize : Long) {
        try {
    // retrieve a small batch of documents to avoid out-of-memory errors
        val future : Task<QuerySnapshot> = collection.limit(batchSize).get();
        var deleted = 0;
    // future.get() blocks on document retrieval
        val documents : List<QueryDocumentSnapshot>  = ;
    for (document in documents) {
      document.getReference().delete();
      ++deleted;
    }
    if (deleted >= batchSize) {
      // retrieve and delete another batch
      deleteCollection(collection, batchSize);
    }
  } catch (e : Exception) {
    System.err.println("Error deleting collection : " + e.message);
  }*/

    fun delCheck() {



    }


}



// http://m.yandex.kz/collections/card/5b6eb2f9a947cc00c1981068/ coin source//
