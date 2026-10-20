package com.restart.spacestationtracker.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.headersOf
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AstronautBioTest {
    private val englishPage =
        """{"query":{"pages":[{"title":"Pyotr Dubrov","extract":"<p>Pyotr Dubrov is a Russian cosmonaut.</p>",
           "langlinks":[{"lang":"de","title":"Pjotr Walerjewitsch Dubrow"}]}]}}"""
    private val germanPage =
        """{"query":{"pages":[{"title":"Pjotr Walerjewitsch Dubrow","extract":"<p>Dubrow ist ein russischer Kosmonaut.</p>"}]}}"""
    private val untranslatedPage =
        """{"query":{"pages":[{"title":"Pyotr Dubrov","extract":"<p>Pyotr Dubrov is a Russian cosmonaut.</p>"}]}}"""

    @Test
    fun usesTheArticleInTheAppLanguage() = runTest {
        val hosts = mutableListOf<String>()
        val repository = repository("de") { host ->
            hosts += host
            if (host.startsWith("de.")) germanPage else englishPage
        }
        assertEquals("Dubrow ist ein russischer Kosmonaut.", repository.getAstronautBio("Pyotr_Dubrov").getOrThrow())
        assertEquals(listOf("en.wikipedia.org", "de.wikipedia.org"), hosts)
    }

    @Test
    fun fallsBackToEnglishWithoutATranslation() = runTest {
        val repository = repository("de") { untranslatedPage }
        assertEquals("Pyotr Dubrov is a Russian cosmonaut.", repository.getAstronautBio("Pyotr_Dubrov").getOrThrow())
    }

    @Test
    fun englishMakesASingleRequest() = runTest {
        val hosts = mutableListOf<String>()
        val repository = repository("en") { host ->
            hosts += host
            englishPage
        }
        assertEquals("Pyotr Dubrov is a Russian cosmonaut.", repository.getAstronautBio("Pyotr_Dubrov").getOrThrow())
        assertEquals(listOf("en.wikipedia.org"), hosts)
    }

    @Test
    fun mapsAndroidsOldIndonesianCode() {
        assertEquals("id", wikipediaLanguage("in"))
        assertEquals("zh", wikipediaLanguage("zh"))
    }

    private fun repository(language: String, respondFor: (host: String) -> String): KtorSpaceStationRepository {
        val engine = MockEngine { request ->
            respond(
                respondFor(request.url.host),
                headers = headersOf(HttpHeaders.ContentType, "application/json")
            )
        }
        val client = HttpClient(engine) { install(ContentNegotiation) { json(spaceStationJson) } }
        return KtorSpaceStationRepository(client = client, bioLanguage = { language })
    }
}
