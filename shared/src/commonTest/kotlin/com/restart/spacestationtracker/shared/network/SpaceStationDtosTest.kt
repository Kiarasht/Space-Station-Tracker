package com.restart.spacestationtracker.shared.network

import kotlinx.serialization.decodeFromString
import kotlin.test.Test
import kotlin.test.assertTrue

class SpaceStationDtosTest {

    @Test
    fun omittedPassesDecodeAsEmptyList() {
        val response = spaceStationJson.decodeFromString<IssPassesResponseDto>(
            """
            {
              "info": {
                "satid": 25544,
                "satname": "SPACE STATION",
                "transactionscount": 16,
                "passescount": 0
              }
            }
            """.trimIndent()
        )

        assertTrue(response.passes.orEmpty().isEmpty())
    }
}
