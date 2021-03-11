package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class AppStateMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(AppStateMonitor::class.java)
        private val sikkerLogg = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "app_status") }
            validate {
                it.requireArray("states") {
                    requireKey("app", "state")
                    require("last_active_time", JsonNode::asLocalDateTime)
                }
            }
            validate { it.require("threshold", JsonNode::asLocalDateTime) }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    private var lastReportTime = LocalDateTime.MIN
    override fun onError(problems: MessageProblems, context: MessageContext) {
        log.error(problems.toString())
        sikkerLogg.error(problems.toExtendedReport())
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        if (lastReportTime > LocalDateTime.now().minusMinutes(2)) return // don't create alerts too eagerly
        val appsDown = packet["states"]
            .filter { it["state"].asInt() == 0 }
            .filter { it["last_active_time"].asLocalDateTime() < LocalDateTime.now().minusMinutes(2) }
            .map { it["app"].asText() to it["last_active_time"].asLocalDateTime() }

        if (appsDown.isEmpty()) return

        val appString = appsDown.last().let { siste ->
            if (appsDown.size == 1) siste.printApp()
            else appsDown.subList(0, appsDown.size - 1).joinToString { it.printApp() } + " og ${siste.printApp()}"
        }
        val logtext = String.format(
            "%s er antatt nede fordi de ikke har svart på ping innen %s siden.",
            appString,
            humanReadableTime(ChronoUnit.SECONDS.between(packet["threshold"].asLocalDateTime(), LocalDateTime.now()))
        )
        log.warn(logtext)
        slackClient?.postMessage(logtext)
        lastReportTime = LocalDateTime.now()
    }

    private fun Pair<String, LocalDateTime>.printApp(): String {
        val tid = humanReadableTime(ChronoUnit.SECONDS.between(second, LocalDateTime.now()))
        return "$first (sist aktiv: $tid siden)"
    }
}
