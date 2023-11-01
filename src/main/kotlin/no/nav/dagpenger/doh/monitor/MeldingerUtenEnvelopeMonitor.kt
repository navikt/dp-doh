package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class MeldingerUtenEnvelopeMonitor(
    rapidsConnection: RapidsConnection,
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

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        sikkerlogg.info { "Mottok pakke som ikke bruker envelope. Packet: ${packet.toJson()}" }
    }
}
