package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
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

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        slackClient?.postMessage(
            text = String.format(
                "Vedtak <%s|%s> har blitt fattet",
                Kibana.createUrl(
                    String.format("\"%s\"", packet["vedtakId"].asText()),
                    packet["@opprettet"].asLocalDateTime().minusHours(1)
                ),
                packet["vedtakId"].asText()
            ).also { log.info("Melding for Slack om VedtakFattet: $it") },
            emoji = ":tada:"
        ).also {
            log.info { "Sendte melding til Slack om VedtakFattet, fikk $it tilbake" }
        }
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
