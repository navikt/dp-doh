package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.tidBruktITilstand
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.Duration

internal class BehandlingEndretTilstandMonitor(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "behandling_endret_tilstand")
                    it.requireKey("forrigeTilstand", "gjeldendeTilstand", "tidBrukt")
                    it.interestedIn("behandlingId")
                }
            }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val forrigeTilstand = packet["forrigeTilstand"].asText()
        val gjeldendeTilstand = packet["gjeldendeTilstand"].asText()
        val tidBrukt = packet["tidBrukt"].asText().let { Duration.parse(it) }

        withLoggingContext(
            "behandlingId" to behandlingId,
        ) {
            logger.info { "Behandling gikk fra $forrigeTilstand til $gjeldendeTilstand på $tidBrukt" }
            tidBruktITilstand.labels(forrigeTilstand, gjeldendeTilstand).observe(tidBrukt.toSeconds().toDouble())
        }
    }
}
