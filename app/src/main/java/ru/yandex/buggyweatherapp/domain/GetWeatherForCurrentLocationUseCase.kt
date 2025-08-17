package ru.yandex.buggyweatherapp.domain

import ru.yandex.buggyweatherapp.model.Location
import ru.yandex.buggyweatherapp.model.WeatherData
import ru.yandex.buggyweatherapp.repository.ILocationRepository
import ru.yandex.buggyweatherapp.repository.IWeatherRepository
import javax.inject.Inject

data class CurrentWeatherResult(
    val weatherData: WeatherData,
    val cityName: String?,
    val location: Location
)

class GetWeatherForCurrentLocationUseCase @Inject constructor(
    private val locationRepository: ILocationRepository,
    private val weatherRepository: IWeatherRepository
) {
    suspend operator fun invoke(): Result<CurrentWeatherResult> {
        val locationResult = locationRepository.getCurrentLocation()
        if (locationResult.isFailure) {
            return Result.failure(locationResult.exceptionOrNull()!!)
        }
        val location = locationResult.getOrThrow()
        val weatherDataResult = weatherRepository.getWeatherData(location)
        if (weatherDataResult.isFailure) {
            return Result.failure(weatherDataResult.exceptionOrNull()!!)
        }
        val weatherData = weatherDataResult.getOrThrow()

        val cityName = locationRepository.getCityNameFromLocation(location).getOrNull()

        return Result.success(
            CurrentWeatherResult(
                weatherData = weatherData,
                cityName = cityName,
                location = location
            )
        )
    }
}