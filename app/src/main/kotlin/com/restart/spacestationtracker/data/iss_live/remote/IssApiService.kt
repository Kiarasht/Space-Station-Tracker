package com.restart.spacestationtracker.data.iss_live.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IssApiService {

    @GET("v1/satellites/25544")
    suspend fun getIssLocation(): IssLocationDto

    @GET("v1/satellites/{id}/positions")
    suspend fun getIssFutureLocations(
        @Path("id") id: Int = 25544,
        @Query("timestamps") timestamps: String,
        @Query("units") units: String = "kilometers",
    ): List<IssLocationDto>

}
