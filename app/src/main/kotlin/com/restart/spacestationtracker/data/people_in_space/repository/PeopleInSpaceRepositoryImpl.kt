package com.restart.spacestationtracker.data.people_in_space.repository

import com.restart.spacestationtracker.data.people_in_space.remote.PeopleInSpaceApiService
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import javax.inject.Inject

class PeopleInSpaceRepositoryImpl @Inject constructor(
    private val api: PeopleInSpaceApiService
) : PeopleInSpaceRepository {

    override suspend fun getPeopleInSpace(): Result<Pair<Expedition, List<Astronaut>>> {
        return try {
            val response = api.getPeopleInSpace()
            val expedition = response.toExpedition()
            val astronauts = response.people.map { it.toAstronaut() }
            Result.success(Pair(expedition, astronauts))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAstronautBio(pageTitle: String): Result<String> {
        return try {
            val response = api.getWikiBio(pageTitle)
            val page = response.query?.pages?.values?.firstOrNull()
            val extract = page?.extract ?: "<p>Biography not available\n</p>"
            Result.success(android.text.Html.fromHtml(extract, android.text.Html.FROM_HTML_MODE_LEGACY).toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
