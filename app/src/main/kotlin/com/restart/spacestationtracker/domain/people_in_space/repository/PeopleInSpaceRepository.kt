package com.restart.spacestationtracker.domain.people_in_space.repository

import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition

interface PeopleInSpaceRepository {
    suspend fun getPeopleInSpace(): Result<Pair<Expedition, List<Astronaut>>>
    suspend fun getAstronautBio(pageTitle: String): Result<String>
}
