package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import org.slf4j.LoggerFactory

internal class AktivitetsloggMonitor(rapidsConnection: RapidsConnection) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(AktivitetsloggMonitor::class.java)

        private val aktivitetCounter = Counter.build("dp_aktivitet_totals", "Antall aktiviteter")
            .labelNames("alvorlighetsgrad", "melding")
            .register()
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "vedtak_endret")
                it.requireArray("aktivitetslogg.aktiviteter") {
                    requireKey("alvorlighetsgrad", "melding")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        packet["aktivitetslogg.aktiviteter"]
            .takeIf(JsonNode::isArray)
            ?.filter { it["alvorlighetsgrad"].asText() in listOf("WARN", "ERROR") }
            ?.onEach {
                aktivitetCounter.labels(it["alvorlighetsgrad"].asText(), it["melding"].asText()).inc()
            }
    }
}
