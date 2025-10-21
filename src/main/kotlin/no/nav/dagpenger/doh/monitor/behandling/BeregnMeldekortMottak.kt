package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.VedtakBot

internal class BeregnMeldekortMottak(
    rapidsConnection: RapidsConnection,
    private val slackClient: VedtakBot?,
) : River.PacketListener {
    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "beregn_meldekort") }
                validate { it.requireKey("meldekortId", "ident", "eksternMeldekortId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val meldekortId = packet["meldekortId"].asText()
        val eksternMeldekortId = packet["eksternMeldekortId"].asText().takeIf { !it.isNullOrEmpty() }

        val melding =
            """
            |Skal beregne meldekort
            |*Meldekort ID*: $eksternMeldekortId
            |(intent meldekort ID: $meldekortId)
            """.trimMargin()

        withLoggingContext(
            "meldekortId" to meldekortId,
            "eksternMeldekortId" to eksternMeldekortId,
        ) {
            slackClient?.skalBeregnemeldekort(
                melding,
            )
        }
    }
}
