package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class VedtakFattetMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtak_endret")
                it.requireValue("gjeldendeTilstand", "VedtakFattet")
                it.requireKey("vedtakId")
                it.require("behov_opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        slackClient?.postMessage(
            String.format(
                "Vedtak <%s|%s> har blitt fattet",
                Kibana.createUrl(
                    String.format("\"%s\"", packet["vedtakId"].asText()),
                    packet["behov_opprettet"].asLocalDateTime().minusHours(1)
                ),
                packet["vedtakId"].asText()
            )
        )
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        println(problems.toExtendedReport())
    }
}
