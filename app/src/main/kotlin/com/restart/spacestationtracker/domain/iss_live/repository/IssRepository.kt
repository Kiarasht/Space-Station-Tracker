package com.restart.spacestationtracker.domain.iss_live.repository

import com.restart.spacestationtracker.domain.iss_live.model.IssLocation

interface IssRepository {
    suspend fun getIssLocation(): Result<IssLocation>
    suspend fun getIssFutureLocations(timestamps: List<Long>): Result<List<IssLocation>>
}