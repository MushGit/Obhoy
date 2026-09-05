package com.obhoy.app.data.repository

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class WeatherRepository {

    suspend fun fetchSurfacePressureHpa(latitude: Double, longitude: Double): Float? = withContext(Dispatchers.IO) {
        try {
            val urlString = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=surface_pressure"
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 4000

            if (connection.responseCode == 200) {
                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val json = JSONObject(responseText)
                val current = json.getJSONObject("current")
                return@withContext current.getDouble("surface_pressure").toFloat()
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}
