package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.SlackClient

internal class SøknadPipelineMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "søknad_aldri_nådd_behandling") }
                validate { it.requireKey("søknadId", "mottatt", "antallVarsler") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknadId"].asText()
        val mottatt = packet["mottatt"].asText()
        val antallVarsler = packet["antallVarsler"].asInt()

        val melding = "Søknad $søknadId (mottatt $mottatt) har ikke nådd behandling. Antall varsler: $antallVarsler"
        logger.warn { melding }

        val emoji = if (antallVarsler > 3) ":fire:" else ":warning:"
        slackClient?.postMessage(text = melding, emoji = emoji)
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
    }
}
