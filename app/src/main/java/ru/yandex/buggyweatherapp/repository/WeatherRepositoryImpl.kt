package ru.yandex.buggyweatherapp.repository

import android.util.Log
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.buggyweatherapp.api.WeatherApiService
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WeatherRepositoryImpl @Inject constructor(
    private val weatherApi: WeatherApiService
) : IWeatherRepository {

    private var cachedWeatherData: WeatherData? = null

    override suspend fun getWeatherData(location: Location): Result<WeatherData> =
        withContext(Dispatchers.IO) {
            try {
                val responseBody =
                    weatherApi.getCurrentWeather(location.latitude, location.longitude)
                val weatherData = parseWeatherData(responseBody, location)
                cachedWeatherData = weatherData
                Result.success(weatherData)
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather", e)
                Result.failure(e)
            }
        }

    override suspend fun getWeatherByCity(cityName: String): Result<WeatherData> =
        withContext(Dispatchers.IO) {
            try {
                val responseBody = weatherApi.getWeatherByCity(cityName)
                val location = extractLocationFromResponse(responseBody)
                val weatherData = parseWeatherData(responseBody, location)
                Result.success(weatherData)
            } catch (e: Exception) {
                Log.e("WeatherRepository", "Error fetching weather by city", e)
                Result.failure(e)
            }
        }

    private fun parseWeatherData(json: JsonObject, location: Location): WeatherData {

        val main = json.getAsJsonObject("main")
        val wind = json.getAsJsonObject("wind")
        val sys = json.getAsJsonObject("sys")
        val weather = json.getAsJsonArray("weather").get(0).asJsonObject
        val clouds = json.getAsJsonObject("clouds")

        return WeatherData(
            cityName = json.get("name").asString,
            country = sys.get("country").asString,
            temperature = main.get("temp").asDouble,
            feelsLike = main.get("feels_like").asDouble,
            minTemp = main.get("temp_min").asDouble,
            maxTemp = main.get("temp_max").asDouble,
            humidity = main.get("humidity").asInt,
            pressure = main.get("pressure").asInt,
            windSpeed = wind.get("speed").asDouble,
            windDirection = if (wind.has("deg")) wind.get("deg").asInt else 0,
            description = weather.get("description").asString,
            icon = weather.get("icon").asString,
            cloudiness = clouds.get("all").asInt,
            sunriseTime = sys.get("sunrise").asLong,
            sunsetTime = sys.get("sunset").asLong,
            timezone = json.get("timezone").asInt,
            timestamp = json.get("dt").asLong,
            rawApiData = json.toString(),
            rain = if (json.has("rain") && json.getAsJsonObject("rain").has("1h"))
                json.getAsJsonObject("rain").get("1h").asDouble else null,
            snow = if (json.has("snow") && json.getAsJsonObject("snow").has("1h"))
                json.getAsJsonObject("snow").get("1h").asDouble else null
        )
    }

    private fun extractLocationFromResponse(json: JsonObject): Location {
        val coord = json.getAsJsonObject("coord")
        val lat = coord.get("lat").asDouble
        val lon = coord.get("lon").asDouble
        val name = json.get("name").asString

        return Location(lat, lon, name)
    }
}