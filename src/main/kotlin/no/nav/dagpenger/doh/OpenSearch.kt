package no.nav.dagpenger.doh

import com.bazaarvoice.jackson.rison.RisonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object OpenSearch {
    private const val OPENSEARCH_BASE_URL = "https://logs.az.nav.no/app/data-explorer/discover"
    private const val URL_FORMAT = "$OPENSEARCH_BASE_URL#?_a=%s&_g=%s&_q=%s"
    private const val DEFAULT_INDEX = "c4992d50-be41-11f0-aab5-1ff58dd1d822"

    private val risonMapper = ObjectMapper(RisonFactory())

    fun createUrl(
        query: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime? = null,
        index: String = DEFAULT_INDEX,
    ) = createUrl(query, startTime.isoLocalDateTime(), endTime?.isoLocalDateTime() ?: "now", index)

    fun createUrl(
        query: String,
        startTime: String,
        endTime: String = "now",
        index: String = DEFAULT_INDEX,
    ) = String.format(URL_FORMAT, appState(index), globalState(startTime, endTime), queryState(query))

    private fun appState(index: String) =
        mapOf(
            "discover" to
                mapOf(
                    "columns" to listOf("level", "message", "envclass", "application", "pod", "cluster"),
                    "isDirty" to false,
                    "sort" to emptyList<Any>(),
                ),
            "metadata" to
                mapOf(
                    "indexPattern" to index,
                    "view" to "discover",
                ),
        ).toRison()

    private fun globalState(
        startTime: String,
        endTime: String,
    ) = mapOf(
        "filters" to emptyList<Any>(),
        "refreshInterval" to
            mapOf(
                "pause" to true,
                "value" to 0,
            ),
        "time" to
            mapOf(
                "from" to startTime,
                "to" to endTime,
            ),
    ).toRison()

    private fun queryState(query: String) =
        mapOf(
            "filters" to emptyList<Any>(),
            "query" to
                mapOf(
                    "language" to "kuery",
                    "query" to query,
                ),
        ).toRison()

    private fun LocalDateTime.isoLocalDateTime() = this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

    private fun Any.toRison() = risonMapper.writeValueAsString(this)
}
