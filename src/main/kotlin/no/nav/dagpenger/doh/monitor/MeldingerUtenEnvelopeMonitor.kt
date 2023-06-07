package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.dagpenger.doh.slack.SlackClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class MeldingerUtenEnvelopeMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    private companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.${this::class.java.simpleName}")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.forbid(
                    "@id",
                    "@opprettet",
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        sikkerlogg.info { "Mottok pakke som ikke bruker envelope. Packet: ${packet.toJson()}" }
        /*slackClient?.postMessage(
            String.format(
                "Behov <%s|%s> mottok aldri løsning for %s innen %s",
                Kibana.createUrl(
                    String.format("\"%s\"", packet["behov_id"].asText()),
                    packet["behov_opprettet"].asLocalDateTime().minusHours(1),
                ),
                packet["behov_id"].asText(),
                packet["mangler"].joinToString(),
                humanReadableTime(
                    ChronoUnit.SECONDS.between(
                        packet["behov_opprettet"].asLocalDateTime(),
                        packet["@opprettet"].asLocalDateTime(),
                    ),
                ),
            ),
        )*/
    }
}
