package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.SlackClient

internal class BehandlingPåminnelseMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    companion object {
        val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behandling_står_fast") }
                validate {
                    it.requireKey("behandlingId", "antallGangerUtsatt")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val antallGangerUtsatt = packet["antallGangerUtsatt"].asInt()
        if (antallGangerUtsatt > 10) {
            val melding =
                "Behandling med id $behandlingId har stått fast $antallGangerUtsatt ganger. Sjekk behandlingen."
            logger.warn { melding }
            slackClient?.postMessage(
                text = melding,
                emoji = ":warning:",
            )
        }
    }
}
