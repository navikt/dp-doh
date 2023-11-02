package no.nav.dagpenger.doh.slack

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.time.Duration.Companion.seconds

internal class SlackClient(private val accessToken: String, private val channel: String) {
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

        return "https://slack.com/api/chat.postMessage".post(
            objectMapper.writeValueAsString(
                parameters,
            ),
        )?.let {
            objectMapper.readTree(it)["ts"]?.asText()
        }
    }

    private fun String.post(jsonPayload: String): String? {
        var connection: HttpURLConnection? = null
        try {
            connection =
                (URL(this).openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 3.seconds.inWholeMilliseconds.toInt()
                    readTimeout = 5.seconds.inWholeMilliseconds.toInt()
                    doOutput = true
                    setRequestProperty("Authorization", "Bearer $accessToken")
                    setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    setRequestProperty("User-Agent", "navikt/dp-doh")

                    outputStream.use {
                        it.bufferedWriter(Charsets.UTF_8).apply {
                            write(jsonPayload)
                            flush()
                        }
                    }
                }
            val responseCode = connection.responseCode

            if (connection.responseCode !in 200..299) {
                log.error("response from slack: code=$responseCode")
                tjenestekall.error("response from slack: code=$responseCode body=${connection.errorStream.readText()}")
                return null
            }
            val responseBody = connection.inputStream.readText()
            log.debug("response from slack: code=$responseCode")
            tjenestekall.debug("response from slack: code=$responseCode body=$responseBody")

            return responseBody
        } catch (err: IOException) {
            log.error("feil ved posting til slack: {}", err.message, err)
        } finally {
            connection?.disconnect()
        }

        return null
    }

    private fun InputStream.readText() = use { it.bufferedReader().readText() }
}
