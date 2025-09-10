package com.restart.spacestationtracker.data.people_in_space.remote

import retrofit2.http.GET
import retrofit2.http.Query

interface PeopleInSpaceApiService {

    @GET("https://corquaid.github.io/international-space-station-APIs/JSON/people-in-space.json")
    suspend fun getPeopleInSpace(): PeopleInSpaceResponseDto

    @GET("https://en.wikipedia.org/w/api.php?action=query&prop=extracts&format=json&exintro=true")
    suspend fun getWikiBio(@Query("titles") pageTitle: String): WikiBioResponseDto

}
