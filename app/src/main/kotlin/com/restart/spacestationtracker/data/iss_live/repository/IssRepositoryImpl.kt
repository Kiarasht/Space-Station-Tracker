package com.restart.spacestationtracker.data.iss_live.repository

import com.restart.spacestationtracker.data.iss_live.remote.IssApiService
import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.iss_live.repository.IssRepository
import javax.inject.Inject

class IssRepositoryImpl @Inject constructor(
    private val api: IssApiService
) : IssRepository {

    override suspend fun getIssLocation(): Result<IssLocation> {
        return try {
            val response = api.getIssLocation()
            Result.success(response.toIssLocation())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getIssFutureLocations(timestamps: List<Long>): Result<List<IssLocation>> {
        return try {
            val timestampsString = timestamps.joinToString(",")
            val response = api.getIssFutureLocations(timestamps = timestampsString)
            Result.success(response.map { it.toIssLocation() })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
