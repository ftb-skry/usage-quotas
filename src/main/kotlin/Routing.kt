package com.example

import io.ktor.server.application.*
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.plugins.BadRequestException
import io.ktor.server.plugins.CannotTransformContentToTypeException
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.receive
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.Serializable

@Serializable
data class Request(
    val apiKey : String,
    val units : Int
)

fun Application.configureRouting() {
    install(ContentNegotiation) {
        json()
    }

    install(StatusPages) {
        exception<CannotTransformContentToTypeException> { call, _ ->
            call.respondText("Unsupported Media Type", status = HttpStatusCode.UnsupportedMediaType)
        }
        exception<BadRequestException> { call, _ ->
            call.respondText("Bad Request", status = HttpStatusCode.BadRequest)
        }
        exception<Throwable> { call, _ ->
            call.respondText("Internal Server Error", status = HttpStatusCode.InternalServerError)
        }
    }

    routing {
        post("/") {
            val request = call.receive<Request>()
            if (request.units < 0)
                return@post call.respondText("units can't be negative", status = HttpStatusCode.BadRequest)

            val result = chargeUnits(request.apiKey, request.units)
            val status =
                if (result.allowed) HttpStatusCode.OK
                else HttpStatusCode.TooManyRequests

            call.respond(status, result)
        }
    }
}
