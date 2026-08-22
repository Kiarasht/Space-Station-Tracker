package com.restart.spacestationtracker.domain.iss_passes.use_case

import com.restart.spacestationtracker.domain.iss_passes.model.IssPass
import com.restart.spacestationtracker.domain.iss_passes.repository.IssPassesRepository

data class UserLocation(
    val latitude: Double,
    val longitude: Double,
    val altitude: Double,
    val name: String
)

class GetIssPassesUseCase(
    private val repository: IssPassesRepository
) {
    suspend operator fun invoke(location: UserLocation): Result<List<IssPass>> {
        return repository.getIssPasses(location.latitude, location.longitude, location.altitude)
    }
}
