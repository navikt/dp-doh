package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.RampBot

internal class BrukerHarMeldekortMedEndretMeldesyklusIArenaMonitor(
    rapidsConnection: RapidsConnection,
    private val rampBot: RampBot?,
) : River.PacketListener {
    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "bruker-har-meldekort-med-endret-meldesyklus-i-arena") }
                validate { it.requireKey("referanseId") }
                validate { it.requireKey("personId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val referanseId = packet["referanseId"].asText()
        val personId = packet["personId"].asText()
        logger.info { "Mottok bruker-har-meldekort-med-endret-meldesyklus-i-arena for referanseId=$referanseId personId=$personId" }
        rampBot?.postBrukerHarMeldekortMedEndretMeldesyklusIArena(referanseId = referanseId, personId = personId)
    }
}
