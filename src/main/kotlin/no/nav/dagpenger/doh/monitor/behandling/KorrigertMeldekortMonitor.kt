package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.VedtakBot

internal class KorrigertMeldekortMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: VedtakBot?,
) : River.PacketListener {
    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "meldekort_innsendt") }
                validate { it.requireKey("periode", "korrigeringAv") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val fraOgMed = packet["periode"]["fraOgMed"].asLocalDate()
        val tilOgMed = packet["periode"]["tilOgMed"].asLocalDate()
        val korrigeringAv = packet["korrigeringAv"].asText()
        val melding = "Mottok korrigert av meldekort ($korrigeringAv) for perioden $fraOgMed -> $tilOgMed!"

        slackClient?.korrigertMeldekort(melding)
    }
}
