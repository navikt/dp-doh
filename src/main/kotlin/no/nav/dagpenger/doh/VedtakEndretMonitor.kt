package no.nav.dagpenger.doh

import io.prometheus.client.Counter
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val tilstandCounter = Counter
    .build("dp_vedtak_endret", "Antall tilstandsendringer")
    .labelNames("tilstand", "forrigeTilstand")
    .register()

internal class VedtakEndretMonitor(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "vedtak_endret")
                it.requireKey(
                    "gjeldendeTilstand",
                    "forrigeTilstand"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        tilstandCounter
            .labels(
                packet["gjeldendeTilstand"].asText(),
                packet["forrigeTilstand"].asText()
            )
            .inc()
    }
}