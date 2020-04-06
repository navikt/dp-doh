package no.nav.helse.spammer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

internal class AvstemmingMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    private val tidsstempel = DateTimeFormatter.ofPattern("eeee d. MMMM Y")

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "avstemming") }
            validate { it.requireKey("@id", "antall_oppdrag") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.require("dagen", JsonNode::asLocalDate) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        slackClient?.postMessage(String.format(
            "Avstemming <%s|%s> for %s ble kjørt %s siden. %d oppdrag ble avstemt",
            Kibana.createUrl(String.format("\"%s\"", packet["@id"].asText()), packet["@opprettet"].asLocalDateTime().minusHours(1)),
            packet["@id"].asText(),
            packet["dagen"].asLocalDate().format(tidsstempel),
            humanReadableTime(ChronoUnit.SECONDS.between(packet["@opprettet"].asLocalDateTime(), LocalDateTime.now())),
            packet["antall_oppdrag"].asInt()
        ))
    }
}
