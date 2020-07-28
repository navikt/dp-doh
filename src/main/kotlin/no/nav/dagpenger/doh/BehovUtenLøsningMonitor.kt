package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import java.time.temporal.ChronoUnit
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory

internal class BehovUtenLøsningMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val uløsteBehovCounter = Counter.build("dp_uløste_behov", "Antall behov uten løsning")
            .labelNames(
                "mangler"
            )
            .register()
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
                it.require("behov_opprettet", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("forstod ikke behov_uten_fullstendig_løsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        uløsteBehovCounter.labels(
            packet["mangler"].toLabel()
        ).inc()

        slackClient?.postMessage(
            String.format(
                "Behov <%s|%s> mottok aldri løsning for %s innen %s",
                Kibana.createUrl(
                    String.format("\"%s\"", packet["behov_id"].asText()),
                    packet["behov_opprettet"].asLocalDateTime().minusHours(1)
                ),
                packet["behov_id"].asText(),
                packet["mangler"].joinToString(),
                humanReadableTime(
                    ChronoUnit.SECONDS.between(
                        packet["behov_opprettet"].asLocalDateTime(),
                        packet["@opprettet"].asLocalDateTime()
                    )
                )
            )
        )
    }

    private fun JsonNode.toLabel() = this.map(JsonNode::asText).sorted().joinToString()
}
