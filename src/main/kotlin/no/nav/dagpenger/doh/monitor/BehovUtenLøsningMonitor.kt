package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import io.prometheus.metrics.core.metrics.Counter
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.humanReadableTime
import no.nav.dagpenger.doh.slack.SlackClient
import org.slf4j.LoggerFactory
import java.time.temporal.ChronoUnit

internal class BehovUtenLøsningMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    private companion object {
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
        private val uløsteBehovCounter =
            Counter
                .builder()
                .name("dp_uloste_behov")
                .help("Antall behov uten løsning")
                .labelNames("behovType")
                .register()
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov_uten_fullstendig_løsning") }
                validate {
                    it.requireKey("@id", "behov_id", "ufullstendig_behov")
                    it.requireArray("forventet")
                    it.requireArray("løsninger")
                    it.requireArray("mangler")
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                    it.require("behov_opprettet", JsonNode::asLocalDateTime)
                }
            }.register(this)
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        sikkerLog.error("forstod ikke behov_uten_fullstendig_løsning:\n${problems.toExtendedReport()}")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        packet["mangler"].map(JsonNode::asText).forEach {
            uløsteBehovCounter.labelValues(it).inc()
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
}
