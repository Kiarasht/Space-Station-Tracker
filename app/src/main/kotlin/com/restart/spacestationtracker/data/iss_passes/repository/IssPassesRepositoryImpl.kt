package com.restart.spacestationtracker.data.iss_passes.repository

import com.restart.spacestationtracker.data.iss_passes.remote.IssPassesApiService
import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.iss_passes.repository.IssPassesRepository
import javax.inject.Inject

class IssPassesRepositoryImpl @Inject constructor(
    private val api: IssPassesApiService
) : IssPassesRepository {
    override suspend fun getIssPasses(
        latitude: Double,
        longitude: Double,
        altitude: Double
    ): Result<List<IssPass>> {
        return try {
            val response = api.getIssPasses(
                latitude = latitude,
                longitude = longitude,
                altitude = altitude
            )
            Result.success(response.passes.map { it.toIssPass() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
