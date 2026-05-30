package com.restart.spacestationtracker.data.iss_passes.remote

import com.google.gson.Gson
import org.junit.Assert.assertTrue
import org.junit.Test

class IssPassesResponseDtoTest {

    @Test
    fun toIssPasses_returnsEmptyListWhenApiOmitsPasses() {
        val response = Gson().fromJson(
            """
            {
              "info": {
                "satid": 25544,
                "satname": "SPACE STATION",
                "transactionscount": 16,
                "passescount": 0
              }
            }
            """.trimIndent(),
            IssPassesResponseDto::class.java
        )

        assertTrue(response.toIssPasses().isEmpty())
    }
}
