package no.nav.helse.spammer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

internal class BehovUtenLøsningMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov_uten_fullstendig_løsning")
                it.requireKey("@id", "behov_id", "ufullstendig_behov")
                it.requireArray("forventet")
                it.requireArray("løsninger")
                it.requireArray("mangler")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.require("behov_opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("forstod ikke behov_uten_fullstendig_løsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        slackClient?.postMessage(String.format(
            "Behov <%s|%s> mottok aldri løsning for %s innen %s",
            Kibana.createUrl(String.format("\"%s\"", packet["behov_id"].asText()), packet["behov_opprettet"].asLocalDateTime().minusHours(1)),
            packet["behov_id"].asText(),
            packet["mangler"].joinToString(),
            humanReadableTime(ChronoUnit.SECONDS.between(packet["behov_opprettet"].asLocalDateTime(), packet["@opprettet"].asLocalDateTime()))
        ))
    }
}
