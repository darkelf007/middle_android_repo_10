package ru.yandex.buggyweatherapp.viewmodel

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.yandex.buggyweatherapp.domain.GetWeatherForCurrentLocationUseCase
import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import javax.inject.Inject

@HiltViewModel
class WeatherViewModel @Inject constructor(
    private val weatherRepository: IWeatherRepository,
    private val locationRepository: ILocationRepository,
    private val getWeatherForCurrentLocationUseCase: GetWeatherForCurrentLocationUseCase
) : ViewModel() {

    val weatherData = MutableLiveData<WeatherData>()
    val currentLocation = MutableLiveData<Location>()
    val isLoading = MutableLiveData<Boolean>()
    val error = MutableLiveData<String?>()
    val cityName = MutableLiveData<String>()

    private var autoRefreshJob: Job? = null

    init {
        fetchCurrentLocationWeather()
        startAutoRefresh()
    }

    fun fetchCurrentLocationWeather() {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            getWeatherForCurrentLocationUseCase()
                .onSuccess { result ->
                    weatherData.value = result.weatherData
                    currentLocation.value = result.location
                    cityName.value = result.cityName ?: "Unknown City"
                }
                .onFailure { exception ->
                    error.value = exception.message ?: "Unable to get current location"
                }

            isLoading.value = false
        }
    }

    private fun getCityName(location: Location) {
        viewModelScope.launch {
            locationRepository.getCityNameFromLocation(location)
                .onSuccess { name ->
                    cityName.value = name ?: "Unknown City"
                }
                .onFailure {
                    cityName.value = "Unknown City"
                }
        }
    }

    fun getWeatherForLocation(location: Location) {
        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            weatherRepository.getWeatherData(location)
                .onSuccess { data ->
                    weatherData.value = data
                }
                .onFailure { exception ->
                    error.value = exception.message ?: "Unknown error"
                }
            isLoading.value = false
        }
    }

    fun searchWeatherByCity(city: String) {
        if (city.isBlank()) {
            error.value = "City name cannot be empty"
            return
        }

        viewModelScope.launch {
            isLoading.value = true
            error.value = null

            weatherRepository.getWeatherByCity(city)
                .onSuccess { data ->
                    weatherData.value = data
                    cityName.value = data.cityName
                    val newLocation =
                        Location(latitude = 0.0, longitude = 0.0, name = data.cityName)
                    currentLocation.value = newLocation
                }
                .onFailure { exception ->
                    error.value = exception.message ?: "Unknown error"
                }
            isLoading.value = false
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            while (true) {
                delay(60000L)
                currentLocation.value?.let { location ->
                    weatherRepository.getWeatherData(location)
                        .onSuccess { data ->
                            weatherData.postValue(data)
                        }
                }
            }
        }
    }

    fun toggleFavorite() {
        weatherData.value?.let { currentData ->
            val updatedData = currentData.copy(isFavorite = !currentData.isFavorite)
            weatherData.value = updatedData
        }
    }

    fun formatTemperature(temp: Double): String = "${temp.toInt()}°C"
}