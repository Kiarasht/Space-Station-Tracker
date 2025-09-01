package com.restart.spacestationtracker.data.iss_passes.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface IssPassesApiService {
    @GET("https://api.n2yo.com/rest/v1/satellite/visualpasses/{id}/{observer_lat}/{observer_lng}/{observer_alt}/{days}/{min_visibility}")
    suspend fun getIssPasses(
        @Path("id") satelliteId: Int = 25544,
        @Path("observer_lat") latitude: Double,
        @Path("observer_lng") longitude: Double,
        @Path("observer_alt") altitude: Double,
        @Path("days") days: Int = 10,
        @Path("min_visibility") minVisibility: Int = 300,
        @Query("apiKey") apiKey: String = "HPD8AL-KBDGWE-JS2M48-4XH2"
    ): IssPassesResponseDto
}
