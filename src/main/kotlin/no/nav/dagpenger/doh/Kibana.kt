package no.nav.dagpenger.doh

import com.bazaarvoice.jackson.rison.RisonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object Kibana {
    private const val baseUrl = "https://logs.adeo.no/app/kibana"
    private const val urlFormat = "$baseUrl#/discover?_a=%s&_g=%s"
    private const val defaultIndex = "96e648c0-980a-11e9-830a-e17bbd64b4db"
    private val objectMapper = ObjectMapper(RisonFactory())

    fun createUrl(
        query: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime? = null,
        index: String = defaultIndex,
    ) =
        createUrl(
            query,
            startTime.isoLocalDateTime(),
            endTime?.isoLocalDateTime()
                ?: "now",
            index,
        )

    // https://logs.adeo.no/app/kibana#/discover?_a={"index":"96e648c0-980a-11e9-830a-e17bbd64b4db","query":{"language":"lucene","query":"\\\"9af983d5-7740-42da-9c68-c45be9795bba\\\""}}&_g={"time":{"from":"2022-03-15T22:46:08.56525","mode":"absolute","to":"now"}}
    // https://logs.adeo.no/app/kibana#/discover?_a=(index:'96e648c0-980a-11e9-830a-e17bbd64b4db',query:(language:lucene,query:'\"546d8fb8-8131-40da-b5d0-0717091d0099\"'))&_g=(time:(from:'2022-03-15T22:46:45.608892',mode:absolute,to:now))

    fun createUrl(
        query: String,
        startTime: String,
        endTime: String = "now",
        index: String = defaultIndex,
    ) =
        String.format(urlFormat, appState(index, query), globalState(startTime, endTime))

    private fun appState(index: String, query: String) = mapOf(
        "index" to index,
        "query" to mapOf(
            "language" to "lucene",
            "query" to query,
        ),
    ).toRison()

    private fun globalState(startTime: String, endTime: String) = mapOf(
        "time" to mapOf(
            "from" to startTime,
            "mode" to "absolute",
            "to" to endTime,
        ),
    ).toRison()

    private fun LocalDateTime.isoLocalDateTime() = this.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    private fun Any.toRison() = objectMapper.writeValueAsString(this)
}
