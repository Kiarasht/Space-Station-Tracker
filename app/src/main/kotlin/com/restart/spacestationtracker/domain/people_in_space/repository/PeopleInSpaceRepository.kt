package com.restart.spacestationtracker.domain.people_in_space.repository

import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut

interface PeopleInSpaceRepository {
    suspend fun getPeopleInSpace(): Result<List<Astronaut>>
    suspend fun getAstronautBio(pageTitle: String): Result<String>
}
