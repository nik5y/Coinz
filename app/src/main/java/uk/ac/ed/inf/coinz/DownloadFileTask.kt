package uk.ac.ed.inf.coinz

import android.os.AsyncTask
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

class DownloadFileTask(private val caller: DownloadCompleteListener) : AsyncTask<String, Void, String>() {
    override fun doInBackground(vararg urls: String): String = try {
        loadFileFromNetwork(urls[0])
    } catch (e: IOException) {
        "Unable to load content. Check your network connection."
    }

    private fun loadFileFromNetwork(urlString: String): String {

        val stream: InputStream = downloadUrl(urlString)
        //read input from stream, build result as a string
        return stream.bufferedReader().use { it.readText() }
    }

    //given a string representation of a url, sets up a connection
    //and gets an input stream
    @Throws(IOException::class)
    private fun downloadUrl(urlString: String): InputStream {
        val url = URL(urlString)
        val conn = url.openConnection() as HttpURLConnection
        conn.apply {
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

interface DownloadCompleteListener {
    fun downloadComplete(result: String)
}

object DownloadCompleteRunner : DownloadCompleteListener {
    private var result: String? = null
    override fun downloadComplete(result: String) {
        this.result = result
    }
}