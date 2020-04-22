package no.nav.helse.spammer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class AvstemmingMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    private val tidsstempel = DateTimeFormatter.ofPattern("eeee d. MMMM Y")

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "avstemming") }
            validate { it.requireKey("@id", "antall_oppdrag") }
            validate { it.requireArray("fagområder") {
                requireKey("nøkkel_fom", "nøkkel_tom", "antall_oppdrag", "antall_avstemmingsmeldinger")
            } }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.require("dagen", JsonNode::asLocalDate) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("forstod ikke avstemming:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        slackClient?.postMessage(String.format(
            "Avstemming <%s|%s> for %s ble kjørt for %s siden. %d oppdrag ble avstemt (%d fagområde(r))",
            Kibana.createUrl(String.format("\"%s\"", packet["@id"].asText()), packet["@opprettet"].asLocalDateTime().minusHours(1)),
            packet["@id"].asText(),
            packet["dagen"].asLocalDate().format(tidsstempel),
            humanReadableTime(ChronoUnit.SECONDS.between(packet["@opprettet"].asLocalDateTime(), LocalDateTime.now())),
            packet["antall_oppdrag"].asInt(),
            packet["fagområder"].size()
        ))
    }
}
