package no.nav.dagpenger.doh.monitor

import io.prometheus.client.Counter
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class VedtakEndretMonitor(rapidsConnection: RapidsConnection) : River.PacketListener {
    companion object {
        private val tilstandCounter = Counter
            .build("dp_vedtak_endret", "Antall tilstandsendringer")
            .labelNames("tilstand", "forrigeTilstand")
            .register()
    }

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

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        tilstandCounter
            .labels(
                packet["gjeldendeTilstand"].asText(),
                packet["forrigeTilstand"].asText()
            )
            .inc()
    }
}
