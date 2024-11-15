package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.prometheus.metrics.core.metrics.Counter

internal class AktivitetsloggMonitor(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        val aktivitetCounter =
            Counter
                .builder()
                .name("dp_aktivitet_total")
                .help("Antall aktiviteter")
                .labelNames(
                    "alvorlighetsgrad",
                    "melding",
                    "tilstand",
                    "harFlereFeil",
                ).register()
    }

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.requireValue("@event_name", "vedtak_endret")
                    it.requireKey("forrigeTilstand")
                    it.requireArray("aktivitetslogg.aktiviteter") {
                        requireKey("alvorlighetsgrad", "melding")
                    }
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val tilstand = packet["forrigeTilstand"].asText()
        val harFlereFeil =
            packet["aktivitetslogg.aktiviteter"]
                .takeIf(JsonNode::isArray)
                ?.count { it["alvorlighetsgrad"].asText() in listOf("ERROR") }
                .let {
                    it !== null && it > 1
                }

        packet["aktivitetslogg.aktiviteter"]
            .takeIf(JsonNode::isArray)
            ?.filter { it["alvorlighetsgrad"].asText() in listOf("WARN", "ERROR") }
            ?.onEach {
                aktivitetCounter
                    .labelValues(
                        it["alvorlighetsgrad"].asText(),
                        it["melding"].asText(),
                        tilstand,
                        harFlereFeil.toString(),
                    ).inc()
            }
    }
}
