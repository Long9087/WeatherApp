package com.example.weatherapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.weatherapp.ui.theme.WeatherAppTheme

//
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import androidx.compose.runtime.*


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WeatherAppTheme {
                // A surface container using the 'background' color from the theme
                Surface(color = MaterialTheme.colorScheme.background) {
                    WeatherApp()
                }
            }
        }
    }
}

data class WeatherData(val city: String, val temperature: String, val condition: String)

val apiKey = "ca80c1fd8bedcee2cb266f9a7cbf4dd7"
val id = "1581129"

// Function to fetch latitude and longitude from zip code using OpenWeatherMap API
suspend fun getLatLonFromZip(id: String, apiKey: String): Pair<Double, Double>? {
    val apiUrl = "https://api.openweathermap.org/data/2.5/weather?id=$id&appid=$apiKey"
    return withContext(Dispatchers.IO) {
        try {
            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                // Parse JSON response
                val jsonResponse = JSONObject(response.toString())
                val lat = jsonResponse.getJSONObject("coord").getDouble("lat")
                val lon = jsonResponse.getJSONObject("coord").getDouble("lon")

                Pair(lat, lon)
            } else {
                println("Failed to fetch latitude and longitude. Response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            println("Error fetching latitude and longitude: ${e.message}")
            null
        }
    }
}

// Mock function to fetch weather data
suspend fun getWeather(lat: String, lon: String, apiKey: String): WeatherData? {
    return withContext(Dispatchers.IO) {
        try {
            val apiUrl = "https://api.openweathermap.org/data/2.5/weather?lat=$lat&lon=$lon&appid=$apiKey"

            val url = URL(apiUrl)
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(connection.inputStream))
                val response = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    response.append(line)
                }
                reader.close()

                // Log the response for debugging
                println("Weather API response: $response")

                // Parse JSON response
                val jsonResponse = JSONObject(response.toString())
                val city = jsonResponse.getString("name")
                val temperatureKelvin = jsonResponse.getJSONObject("main").getDouble("temp")
                val temperatureCelsius = temperatureKelvin - 273.15 // Convert Kelvin to Celsius
                val temperature = String.format("%.2f", temperatureCelsius) // Format to two decimal places
                val condition = jsonResponse.getJSONArray("weather").getJSONObject(0).getString("description")

                WeatherData(city, temperature, condition)
            } else {
                println("Weather API request failed with response code: $responseCode")
                null
            }
        } catch (e: Exception) {
            println("Error fetching weather data: ${e.message}")
            null
        }
    }
}



@Composable
fun WeatherApp() {

    val latLon = remember { mutableStateOf<Pair<Double, Double>?>(null) }
    var weather by remember { mutableStateOf<WeatherData?>(null) }

    LaunchedEffect(Unit) {
        val fetchedLatLon = getLatLonFromZip(id, apiKey)
        latLon.value = fetchedLatLon
        val (lat, lon) = latLon.value ?: Pair(0.0, 0.0)
        weather = getWeather(lat.toString(), lon.toString(), apiKey)
    }

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Current Weather in Ha Noi",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            weather?.let { data ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "City: ${data.city}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Temperature: ${data.temperature}°C",
                        style = MaterialTheme.typography.labelMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Condition: ${data.condition}",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            } ?: Text(
                text = "Loading...",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}



@Preview(showBackground = true)
@Composable
fun WeatherAppPreview() {
    WeatherAppTheme {
        WeatherApp()
    }
}