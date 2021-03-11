package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class VedtakFattetMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtak_endret")
                it.demandValue("gjeldendeTilstand", "VedtakFattet")
                it.requireKey("vedtakId")
                it.require("@opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        slackClient?.postMessage(
            text = String.format(
                "Vedtak <%s|%s> har blitt fattet",
                Kibana.createUrl(
                    String.format("\"%s\"", packet["vedtakId"].asText()),
                    packet["@opprettet"].asLocalDateTime().minusHours(1)
                ),
                packet["vedtakId"].asText()
            ),
            emoji = ":tada:"
        )
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
