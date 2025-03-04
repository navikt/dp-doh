package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.humanReadableTime
import no.nav.dagpenger.doh.slack.SlackClient
import org.slf4j.LoggerFactory
import java.net.URLEncoder
import java.nio.charset.Charset
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

@Suppress("ktlint:standard:max-line-length")
internal class AppStateMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
    private val nedetidFørAlarm: Duration = Duration.ofMinutes(5),
) : River.PacketListener {
    private companion object {
        private val log = LoggerFactory.getLogger(AppStateMonitor::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
        private val natt = LocalTime.MIDNIGHT..LocalTime.of(5, 0)
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "app_status") }
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
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            }.register(this)
    }

    private var lastReportTime = LocalDateTime.MIN

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        log.error(problems.toString())
        sikkerLogg.error(problems.toExtendedReport())
    }

    private val lastAlertTimes = mutableMapOf<String, LocalDateTime>()

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val now = LocalDateTime.now()
        if (now.toLocalTime() in natt) return

        val appsDown = packet.apperSomHarVærtNede()

        if (appsDown.isNotEmpty()) {
            val appsToAlert =
                appsDown.filter { (app, _, _) ->
                    val lastAlertTime = lastAlertTimes[app]
                    lastAlertTime == null || Duration.between(lastAlertTime, now).toMinutes() >= 5
                }
            val logtext =
                if (appsToAlert.size == 1) {
                    val (app, sistAktivitet, _) = appsDown.first()
                    val tid = humanReadableTime(ChronoUnit.SECONDS.between(sistAktivitet, now))
                    val kibanaUrl = teamdagpengerKibanaUrl()
                    lastAlertTimes[app] = now
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
                            lastAlertTimes[app] = now
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
            appsToAlert.forEach { (app, _, instances) ->
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
                lastAlertTimes[app] = now
            }
        }
        lastReportTime = now
    }

    private fun teamdagpengerKibanaUrl(): String =
        Kibana.createUrl(
            URLEncoder.encode(
                "team: teamdagpenger AND (level:Error OR level:Warning) AND envclass:p",
                Charset.defaultCharset(),
            ),
            LocalDateTime.now().minusMinutes(15),
        )

    private fun JsonMessage.apperSomHarVærtNede(): List<Triple<String, LocalDateTime, List<Pair<String, LocalDateTime>>>> {
        val opprettet = this["@opprettet"].asLocalDateTime()
        val grenseForVarsel: LocalDateTime = opprettet.minus(nedetidFørAlarm)
        return this["states"]
            .filter { it["state"].asInt() == 0 }
            .filter {
                it["last_active_time"].asLocalDateTime().isBefore(grenseForVarsel)
            }.map {
                Triple(
                    it["app"].asText(),
                    it["last_active_time"].asLocalDateTime(),
                    it["instances"]
                        .filter { instance -> instance.path("state").asInt() == 0 }
                        .map { instance ->
                            Pair(
                                instance.path("instance").asText(),
                                instance.path("last_active_time").asLocalDateTime(),
                            )
                        },
                )
            }
    }
}
