package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.humanReadableTime
import no.nav.dagpenger.doh.slack.SlackClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

internal class BehovUtenLøsningMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {

    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val uløsteBehovCounter = Counter.build("dp_uloste_behov", "Antall behov uten løsning")
            .labelNames("behovType")
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

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerLog.error("forstod ikke behov_uten_fullstendig_løsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["mangler"].map(JsonNode::asText).forEach {
            uløsteBehovCounter.labels(it).inc()
        }

        slackClient?.postMessage(
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
        )
    }

    private fun JsonNode.toLabel() = this.map(JsonNode::asText).sorted().joinToString()
}
