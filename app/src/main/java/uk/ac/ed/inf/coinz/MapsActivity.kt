package uk.ac.ed.inf.coinz

import android.content.Intent
import android.graphics.BitmapFactory
import android.location.Location
import android.os.AsyncTask
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
//import com.google.firebase.FirebaseApp

import com.google.firebase.auth.FirebaseAuth
import com.mapbox.android.core.location.LocationEngine
import com.mapbox.android.core.location.LocationEngineListener
import com.mapbox.android.core.location.LocationEnginePriority
import com.mapbox.android.core.location.LocationEngineProvider
import com.mapbox.android.core.permissions.PermissionsListener
import com.mapbox.android.core.permissions.PermissionsManager
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.annotations.MarkerOptions
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.plugins.locationlayer.LocationLayerPlugin
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.CameraMode
import com.mapbox.mapboxsdk.plugins.locationlayer.modes.RenderMode
import kotlinx.android.synthetic.main.activity_main.*
import uk.ac.ed.inf.coinz.MapsActivity.DownloadCompleteRunner.result
import uk.ac.ed.inf.coinz.R.id.toolbar
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*


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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // setSupportActionBar(toolbar)

        Mapbox.getInstance(applicationContext, getString(R.string.access_token))

            mapView = findViewById(R.id.mapView)
            mapView?.onCreate(savedInstanceState)
            mapView?.getMapAsync(this)
        if (mAuth.currentUser == null) {
            goToLogin()
        }
    }


    override fun onMapReady(mapboxMap: MapboxMap?) {

        //DOWNLOADING

        val date = SimpleDateFormat("yyyy/MM/dd").format(Date())
        val mapURL : String = "http://homepages.inf.ed.ac.uk/stg/coinz/" + date + "/coinzmap.geojson"

        var coins : String = DownloadFileTask(DownloadCompleteRunner).execute(mapURL).get()

        var coinFeatures : FeatureCollection = FeatureCollection.fromJson(coins)

        //mapping

        map = mapboxMap

        for (i in coinFeatures.features()!!) {

            val g = i.geometry() as Point
            val p = g.coordinates()
            val tit = i.getStringProperty("id")
            val k = i.properties()?.get("currency")
            map?.addMarker( MarkerOptions()
                    .position( LatLng(p[1], p[0]))
                    .title(tit))
                    //.icon(R.drawable.generic_coin)

        }

        //map?.getUiSettings()?.setRotateGesturesEnabled(false)
        //map?.getUiSettings()?.setLogoGravity(Gravity.BOTTOM | Gravity.END);
        //map?.getUiSettings()?.setLogoEnabled(true);
        map?.getUiSettings()?.setAttributionEnabled(false)
        map?.getUiSettings()?.setZoomControlsEnabled(true)

        //locating

        enableLocation()
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
            setCameraPosition(originLocation)
        }
    }

    @SuppressWarnings("MissingPermission")
    override fun onConnected() {
        Log.d(tag,"[onConnected] requesting location updates")
        locationEngine?.requestLocationUpdates()
    }


    fun goToLogin() {
        val intent : Intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
    }

    //Lifecycle ting:----------------------------------------


    @SuppressWarnings("MissingPermission")
    override fun onStart() {
        super.onStart()
       // if (mAuth.currentUser == null) {
         //   goToLogin()
        //} else {
            /*if (PermissionsManager.areLocationPermissionsGranted(this)) {
            locationEngine!!.requestLocationUpdates()
            locationLayerPlugin!!.onStart()
        }*/
            mapView?.onStart()

        //}
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

}


// http://m.yandex.kz/collections/card/5b6eb2f9a947cc00c1981068/ coin source//
