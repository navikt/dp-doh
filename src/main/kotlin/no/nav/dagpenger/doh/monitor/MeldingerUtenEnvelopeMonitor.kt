package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging

internal class MeldingerUtenEnvelopeMonitor(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.${this::class.java.simpleName}")
    }

    init {
        River(rapidsConnection)
            .apply {
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
