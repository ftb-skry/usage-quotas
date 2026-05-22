package com.example

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import kotlin.test.*

class ServerTest {

    @Test
    fun `accepts json request at root endpoint`() = testApplication {
        configure()

        val response = createJsonClient().postUsage(Request(apiKey = "test-api-key", units = 10))

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ChargeResult(allowed = true, availableUnits = 90), response.body())
    }

    @Test
    fun `returns too many requests when quota is exceeded`() = testApplication {
        configure()

        val response = createJsonClient().postUsage(Request(apiKey = "quota-exceeded-api-key", units = 101))

        assertEquals(HttpStatusCode.TooManyRequests, response.status)
        assertEquals(ChargeResult(allowed = false, availableUnits = 100), response.body())
    }

    @Test
    fun `returns bad request when units is negative`() = testApplication {
        configure()

        val response = createJsonClient().postUsage(Request(apiKey = "negative-units-api-key", units = -1))

        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals("units can't be negative", response.bodyAsText())
    }

    @Test
    fun `returns bad request when json cannot be parsed`() = testApplication {
        configure()

        val response = createJsonClient().post("/") {
            contentType(ContentType.Application.Json)
            setBody("""{"apiKey":"bad-json","units":"not-a-number"}""")
        }

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun `returns unsupported media type when request is not json`() = testApplication {
        configure()

        val response = createJsonClient().post("/") {
            contentType(ContentType.Text.Plain)
            setBody("""{"apiKey":"wrong-content-type","units":10}""")
        }

        assertEquals(HttpStatusCode.UnsupportedMediaType, response.status)
    }

    private fun ApplicationTestBuilder.createJsonClient() = createClient {
        install(ContentNegotiation) {
            json()
        }
    }

    private suspend fun HttpClient.postUsage(request: Request) = post("/") {
        contentType(ContentType.Application.Json)
        setBody(request)
    }
}
