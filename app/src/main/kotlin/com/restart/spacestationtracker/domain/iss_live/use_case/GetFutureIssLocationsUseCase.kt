package com.restart.spacestationtracker.domain.iss_live.use_case

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation
import com.restart.spacestationtracker.domain.iss_live.repository.IssRepository
import javax.inject.Inject

class GetFutureIssLocationsUseCase @Inject constructor(
    private val issRepository: IssRepository
) {
    suspend operator fun invoke(timestamps: List<Long>): Result<List<IssLocation>> {
        return issRepository.getIssFutureLocations(timestamps)
    }
}
