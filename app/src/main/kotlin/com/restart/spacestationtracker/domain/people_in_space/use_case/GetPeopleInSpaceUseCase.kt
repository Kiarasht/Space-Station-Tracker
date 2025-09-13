package com.restart.spacestationtracker.domain.people_in_space.use_case

import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetPeopleInSpaceUseCase @Inject constructor(
    private val repository: PeopleInSpaceRepository
) {
    suspend operator fun invoke(): Result<Pair<Expedition, List<Astronaut>>> {
        return repository.getPeopleInSpace().mapCatching { (expedition, astronauts) ->
            coroutineScope {
                val updatedExpedition = async {
                    val expeditionTitle = expedition.url.substringAfterLast("/")
                    val expeditionResult = repository.getAstronautBio(expeditionTitle)
                    expedition.copy(bio = expeditionResult.getOrDefault("Could not load bio."))
                }.await()

                val updatedAstronauts = astronauts.map { astronaut ->
                    async {
                        val pageTitle = astronaut.bioUrl.substringAfterLast("/")
                        val bioResult = repository.getAstronautBio(pageTitle)
                        astronaut.copy(bio = bioResult.getOrDefault("Could not load bio."))
                    }
                }.awaitAll()
                Pair(updatedExpedition, updatedAstronauts)
            }
        }
    }
}
