package no.nav.dagpenger.doh

import com.bazaarvoice.jackson.rison.RisonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Kibana {
    private const val baseUrl = "https://logs.adeo.no/app/kibana"
    private const val urlFormat = "$baseUrl#/discover?_a=%s&_g=%s"
    private const val defaultIndex = "96e648c0-980a-11e9-830a-e17bbd64b4db"

    private val risonMapper = ObjectMapper(RisonFactory())

    fun createUrl(query: String, startTime: LocalDateTime, endTime: LocalDateTime? = null, index: String = defaultIndex) =
            createUrl(query, startTime.isoLocalDateTime(), endTime?.isoLocalDateTime()
                    ?: "now", index)

    fun createUrl(
        query: String,
        startTime: String,
        endTime: String = "now",
        index: String = defaultIndex
    ) =
        String.format(urlFormat, appState(index, query), globalState(startTime, endTime))

    private fun appState(index: String, query: String) = mapOf(
        "index" to index,
        "query" to mapOf(
            "language" to "lucene",
            "query" to query
        )
    ).toRison()

    private fun globalState(startTime: String, endTime: String) = mapOf(
        "time" to mapOf(
            "from" to startTime,
            "mode" to "absolute",
            "to" to endTime
        )
    ).toRison()

    private fun LocalDateTime.isoLocalDateTime() = this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    private fun Any.toRison() = risonMapper.writeValueAsString(this)
}
