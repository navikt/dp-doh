package no.nav.dagpenger.doh.slack

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpRequest.BodyPublishers.ofString
import java.net.http.HttpResponse
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

internal class SlackClient(
    private val accessToken: String,
    private val channel: String,
    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(2.seconds.toJavaDuration())
            .build(),
) {
    private companion object {
        private val tjenestekall = LoggerFactory.getLogger("tjenestekall")
        private val log = LoggerFactory.getLogger(SlackClient::class.java)
        private val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun postMessage(
        text: String,
        emoji: String = ":scream:",
        threadTs: String? = null,
        broadcast: Boolean = false,
    ): String? {
        val slackTrådParameter =
            if (threadTs != null) {
                mapOf("thread_ts" to threadTs, "reply_broadcast" to broadcast)
            } else {
                emptyMap()
            }
        val parameters =
            mapOf<String, Any>(
                "channel" to channel,
                "text" to text,
                "icon_emoji" to emoji,
            ) + slackTrådParameter

        return "https://slack.com/api/chat.postMessage"
            .post(objectMapper.writeValueAsString(parameters))
            ?.let { objectMapper.readTree(it)["ts"]?.asText() }
    }

    private fun String.post(jsonPayload: String): String? {
        try {
            val request =
                HttpRequest
                    .newBuilder(URI(this))
                    .header("Authorization", "Bearer $accessToken")
                    .header("Content-Type", "application/json; charset=utf-8")
                    .header("Accept", "application/json")
                    .header("User-Agent", "navikt/dp-doh")
                    .POST(ofString(jsonPayload))
                    .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
            val responseCode = response?.statusCode()

            if (responseCode !in 200..299) {
                log.error("response from slack: code=$responseCode")
                tjenestekall.error("response from slack: code=$responseCode body=${response.body()}")
                return null
            }

            val responseBody = response.body()
            log.debug("response from slack: code=$responseCode")
            tjenestekall.debug("response from slack: code=$responseCode body=$responseBody")

            return responseBody
        } catch (err: IOException) {
            log.error("feil ved posting til slack: {}", err.message, err)
        }

        return null
    }
}
