package no.nav.helse.spammer

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.slf4j.LoggerFactory
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime

internal fun SlackClient?.postMessage(slackThreadDao: SlackThreadDao, vedtaksperiodeId: String, message: String) {
    if (this == null) return

    var threadTs: String? = null
    var broadcast = false
    slackThreadDao.hentThreadTs(vedtaksperiodeId)?.also {
        threadTs = it.first
        broadcast = it.second.isBefore(LocalDateTime.now().minusDays(2))
    }

    this.postMessage(message, threadTs, broadcast)?.also {
        if (threadTs == null) {
            slackThreadDao.lagreThreadTs(vedtaksperiodeId, it)
        }
    } ?: threadTs?.let {
        // if threadTs is !null, and postMessage didn't return a threadTs; assume there's an error
        // with the provided threadTs and retry without.
        this.postMessage(message)?.also {
            slackThreadDao.lagreThreadTs(vedtaksperiodeId, it)
        }
    }
}

internal class SlackClient(private val accessToken: String, private val channel: String) {

    private companion object {
        private val tjenestekall = LoggerFactory.getLogger("tjenestekall")
        private val log = LoggerFactory.getLogger(SlackClient::class.java)
        private val objectMapper = jacksonObjectMapper()
            .registerModule(JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
    }

    fun postMessage(text: String, threadTs: String? = null, broadcast: Boolean = false): String? {
        return "https://slack.com/api/chat.postMessage".post(objectMapper.writeValueAsString(mutableMapOf<String, Any>(
            "channel" to channel,
            "text" to text
        ).apply {
            threadTs?.also {
                put("thread_ts", it)
                put("reply_broadcast", broadcast)
            }
        }))?.let {
            objectMapper.readTree(it)["ts"]?.asText()
        }
    }

    private fun String.post(jsonPayload: String): String? {
        var connection: HttpURLConnection? = null
        try {
            connection = (URL(this).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 1000
                readTimeout = 1000
                doOutput = true
                setRequestProperty("Authorization", "Bearer $accessToken")
                setRequestProperty("Content-Type", "application/json; charset=utf-8")
                setRequestProperty("User-Agent", "navikt/spammer")

                outputStream.use { it.bufferedWriter(Charsets.UTF_8).apply { write(jsonPayload); flush() } }
            }

            val responseCode = connection.responseCode

            if (connection.responseCode !in 200..299) {
                log.error("response from slack: code=$responseCode")
                tjenestekall.error("response from slack: code=$responseCode body=${connection.errorStream.readText()}")
                return null
            }

            val responseBody = connection.inputStream.readText()
            log.info("response from slack: code=$responseCode")
            tjenestekall.info("response from slack: code=$responseCode body=$responseBody")

            return responseBody
        } catch (err: IOException) {
            log.error("feil ved posting til slack: {}", err.messge, err)
        } finally {
            connection?.disconnect()
        }

        return null
    }

    private fun InputStream.readText() = use { it.bufferedReader().readText() }
}
