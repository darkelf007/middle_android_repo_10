package ru.yandex.buggyweatherapp.repository

import ru.yandex.buggyweatherapp.model.Location

interface ILocationRepository {

    suspend fun getCurrentLocation(): Result<Location>

    suspend fun getCityNameFromLocation(location: Location): Result<String?>
}