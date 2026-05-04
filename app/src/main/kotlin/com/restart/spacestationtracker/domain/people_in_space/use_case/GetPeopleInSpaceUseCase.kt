package com.restart.spacestationtracker.domain.people_in_space.use_case

import android.content.Context
import com.restart.spacestationtracker.R
import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.domain.people_in_space.repository.PeopleInSpaceRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import javax.inject.Inject

class GetPeopleInSpaceUseCase @Inject constructor(
    private val repository: PeopleInSpaceRepository,
    @param:ApplicationContext private val context: Context
) {
    suspend operator fun invoke(): Result<Pair<Expedition, List<Astronaut>>> {
        return repository.getPeopleInSpace().mapCatching { (expedition, astronauts) ->
            coroutineScope {
                val updatedExpedition = async {
                    val expeditionTitle = expedition.url.substringAfterLast("/")
                    val expeditionResult = repository.getAstronautBio(expeditionTitle)
                    expedition.copy(
                        bio = expeditionResult.getOrDefault(
                            context.getString(R.string.could_not_load_bio)
                        )
                    )
                }.await()

                val updatedAstronauts = astronauts.map { astronaut ->
                    async {
                        val pageTitle = astronaut.bioUrl.substringAfterLast("/")
                        val bioResult = repository.getAstronautBio(pageTitle)
                        astronaut.copy(
                            bio = bioResult.getOrDefault(
                                context.getString(R.string.could_not_load_bio)
                            )
                        )
                    }
                }.awaitAll()
                Pair(updatedExpedition, updatedAstronauts)
            }
        }
    }
}
