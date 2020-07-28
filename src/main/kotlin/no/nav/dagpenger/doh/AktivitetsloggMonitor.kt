package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class AktivitetsloggMonitor(rapidsConnection: RapidsConnection) : River.PacketListener {
    private companion object {
        private val aktivitetCounter = Counter.build("dp_aktivitet_totals", "Antall aktiviteter")
            .labelNames(
                "alvorlighetsgrad",
                "melding",
                "tilstand",
                "harFlereFeil"
            )
            .register()
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "vedtak_endret")
                it.requireKey("forrigeTilstand")
                it.requireArray("aktivitetslogg.aktiviteter") {
                    requireKey("alvorlighetsgrad", "melding")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val tilstand = packet["forrigeTilstand"].asText()
        val harFlereFeil = packet["aktivitetslogg.aktiviteter"]
            .takeIf(JsonNode::isArray)
            ?.count { it["alvorlighetsgrad"].asText() in listOf("ERROR") }.let {
                it !== null && it > 1
            }

        packet["aktivitetslogg.aktiviteter"]
            .takeIf(JsonNode::isArray)
            ?.filter { it["alvorlighetsgrad"].asText() in listOf("WARN", "ERROR") }
            ?.onEach {
                aktivitetCounter.labels(
                    it["alvorlighetsgrad"].asText(),
                    it["melding"].asText(),
                    tilstand,
                    harFlereFeil.toString()
                ).inc()
            }
    }
}
