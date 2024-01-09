package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.humanReadableTime
import no.nav.dagpenger.doh.slack.SlackClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Suppress("ktlint:standard:max-line-length")
internal class AppStateMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    private companion object {
        private val log = LoggerFactory.getLogger(AppStateMonitor::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val natt = LocalTime.MIDNIGHT..LocalTime.of(5, 0)
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "app_status") }
            validate {
                it.requireArray("states") {
                    requireKey("app", "state")
                    require("last_active_time", JsonNode::asLocalDateTime)
                    requireArray("instances") {
                        requireKey("instance", "state")
                        require("last_active_time", JsonNode::asLocalDateTime)
                    }
                }
            }
            validate { it.require("threshold", JsonNode::asLocalDateTime) }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    private var lastReportTime = LocalDateTime.MIN

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        log.error(problems.toString())
        sikkerLogg.error(problems.toExtendedReport())
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val now = LocalDateTime.now()
        if (now.toLocalTime() in natt || lastReportTime > now.minusMinutes(15)) return // don't create alerts too eagerly
        val appsDown = packet.appsDown(now)

        if (appsDown.isNotEmpty()) {
            val logtext =
                if (appsDown.size == 1) {
                    val (app, sistAktivitet, _) = appsDown.first()
                    val tid = humanReadableTime(ChronoUnit.SECONDS.between(sistAktivitet, now))
                    val kibanaUrl = teamdagpengerKibanaUrl()
                    """
                    | $app er antatt nede (siste aktivitet: $tid) fordi den ikke svarer tilfredsstillende på ping. Trøblete instanser i :thread:
                    |   :question: Hva betyr dette for meg? Det kan bety at appen ikke leser fra Kafka, og kan ha alvorlig feil. Det kan også bety at appene har stoppet opp. 
                    |   - Sjekk logger i <$kibanaUrl|Kibana>
                    |   - Sjekk lag i <https://grafana.nais.io/d/j-ZhhGJnz/kafka-viser-offset-og-messages-second-per-consumer?orgId=1&var-datasource=prod-gcp&var-consumer_group=All&var-topic=All&viewPanel=18|Grafana>
                    """.trimMargin()
                } else {
                    val instanser =
                        appsDown.joinToString(separator = "\n") { (app, sistAktivitet, _) ->
                            val tid = humanReadableTime(ChronoUnit.SECONDS.between(sistAktivitet, now))
                            "- $app (siste aktivitet: $tid - $sistAktivitet)"
                        }

                    val kibanaUrl = teamdagpengerKibanaUrl()

                    """
                    | ${appsDown.size} apper er antatt nede da de ikke svarer tilfredsstillende på ping. Trøblete instanser i :thread:
                    |   $instanser
                    |   :question: Hva betyr dette for meg? Det kan bety at appene ikke leser fra Kafka, og kan ha alvorlig feil. Det kan også bety at appene har stoppet opp. 
                    |   - Loggfeil i dagpenger teamet i <$kibanaUrl|Kibana>
                    |   - Sjekk kafka lag i <https://grafana.nais.io/d/j-ZhhGJnz/kafka-viser-offset-og-messages-second-per-consumer?orgId=1&var-datasource=prod-gcp&var-consumer_group=All&var-topic=All&viewPanel=18|Grafana>
                    """.trimMargin()
                }
            log.warn(logtext)
            val threadTs = slackClient?.postMessage(logtext)
            appsDown.forEach { (_, _, instances) ->
                val text =
                    instances.joinToString(separator = "\n") { (instans, sistAktivitet) ->
                        val tid = humanReadableTime(ChronoUnit.SECONDS.between(sistAktivitet, now))
                        "- $instans (siste aktivitet: $tid - $sistAktivitet)"
                    }
                log.info(
                    """
                    POSTER TIL SLACK
                    $threadTs
                    $text
                    """.trimIndent(),
                )
                slackClient?.postMessage(text = text, threadTs = threadTs)
            }
        }
        lastReportTime = now
    }

    private fun teamdagpengerKibanaUrl(): String =
        Kibana.createUrl(
            URLEncoder.encode(
                "team: teamdagpenger AND level:Error OR level:Warning AND envclass:p",
                Charset.defaultCharset(),
            ),
            LocalDateTime.now().minusMinutes(15),
        )

    private fun JsonMessage.appsDown(now: LocalDateTime) =
        this["states"]
            .filter { it["state"].asInt() == 0 }
            .filter { it["last_active_time"].asLocalDateTime() < now.minusMinutes(2) }
            .map {
                Triple(
                    it["app"].asText(),
                    it["last_active_time"].asLocalDateTime(),
                    it["instances"]
                        .filter { instance -> instance.path("state").asInt() == 0 }
                        .map { instance ->
                            Pair(instance.path("instance").asText(), instance.path("last_active_time").asLocalDateTime())
                        },
                )
            }
}
