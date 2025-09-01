package com.restart.spacestationtracker.domain.iss_passes.repository

import com.restart.spacestationtracker.domain.iss_passes.model.IssPass

interface IssPassesRepository {
    suspend fun getIssPasses(latitude: Double, longitude: Double, altitude: Double): Result<List<IssPass>>
}
