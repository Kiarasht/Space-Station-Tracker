package com.restart.spacestationtracker.shared.ui

import com.restart.spacestationtracker.domain.people_in_space.model.Astronaut
import com.restart.spacestationtracker.domain.people_in_space.model.Expedition
import com.restart.spacestationtracker.shared.network.PeopleInSpaceResponseDto
import com.restart.spacestationtracker.shared.network.spaceStationJson
import kotlin.test.Test
import kotlin.test.assertEquals

class CrewStationHeadersTest {
    private val expedition = SharedCrewItem.ExpeditionItem(
        Expedition(number = 75, patchUrl = "", url = "", imageUrl = "", startDate = 0, endDate = 0, bio = "")
    )

    private fun astronaut(name: String, onIss: Boolean, craft: String = "") = SharedCrewItem.AstronautItem(
        Astronaut(
            name = name, craft = craft, bio = "", bioUrl = "", profileImageUrl = "", launchDate = 0, role = "",
            flagCode = "", twitterUrl = null, instagramUrl = null, facebookUrl = null, isOnIss = onIss
        )
    )

    private fun describe(items: List<SharedCrewItem>) = items.map { item ->
        when (item) {
            is SharedCrewItem.ExpeditionItem -> "expedition"
            is SharedCrewItem.AstronautItem -> item.astronaut.name
            is SharedCrewItem.PlatformSlot -> "ad"
            is SharedCrewItem.SectionHeader -> when (val section = item.section) {
                CrewSection.Iss -> "[ISS]"
                is CrewSection.Craft -> "[${section.name}]"
            }
            is SharedCrewItem.Summary -> "summary"
        }
    }

    @Test
    fun leavesAnIssOnlyListUnchanged() {
        val items = listOf(expedition, astronaut("Anna", true), astronaut("Pyotr", true))
        assertEquals(items, withStationHeaders(items))
    }

    @Test
    fun headsPeopleElsewhereWithTheirSpacecraft() {
        val items = listOf(
            expedition,
            astronaut("Anna", true, "Soyuz MS-29"),
            astronaut("Pyotr", true, "Crew-13 Dragon"),
            SharedCrewItem.PlatformSlot("crew-1"),
            astronaut("Zhu", false, "Shenzhou 23"),
            astronaut("Lai", false, "Shenzhou 23"),
            astronaut("Kim", false, " Axiom 5 ")
        )
        assertEquals(
            listOf("[ISS]", "expedition", "Anna", "Pyotr", "ad", "[Shenzhou 23]", "Zhu", "Lai", "[Axiom 5]", "Kim"),
            describe(withStationHeaders(items))
        )
        assertEquals(
            SharedCrewItem.Summary(total = 5, onIss = 2, elsewhere = listOf("Shenzhou 23" to 2, "Axiom 5" to 1)),
            crewSummary(items)
        )
    }

    @Test
    fun neverRepeatsACraftHeader() {
        val items = listOf(astronaut("Zhu", false, "Shenzhou 23"), astronaut("Kim", false, "Axiom 5"), astronaut("Lai", false, "Shenzhou 23"))
        assertEquals(listOf("[Shenzhou 23]", "Zhu", "[Axiom 5]", "Kim", "Lai"), describe(withStationHeaders(items)))
    }

    @Test
    fun mapsTheFeedsIssFlag() {
        val response = spaceStationJson.decodeFromString(
            PeopleInSpaceResponseDto.serializer(),
            """{"iss_expedition":75,"people":[
              {"name":"Zhu Yangzhu","spacecraft":"Shenzhou 23","iss":false},
              {"name":"Anna Kikina","spacecraft":"Soyuz MS-29","iss":true},
              {"name":"No Flag","spacecraft":"Crew-13 Dragon"}]}"""
        )
        assertEquals(listOf(false, true, true), response.people.map { it.toAstronaut().isOnIss })
    }
}
