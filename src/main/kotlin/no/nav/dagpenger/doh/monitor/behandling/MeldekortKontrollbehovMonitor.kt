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

internal class MeldekortKontrollbehovMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: VedtakBot,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "meldekortberegning_trenger_kontrollregning") }
                validate {
                    it.requireKey("behandlingId", "behandletHendelseId", "detaljer")
                    it.interestedIn("ident")
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
        val behandletHendelseId = packet["behandletHendelseId"].asText()
        val ident = packet["ident"].asText("")
        val aktiveDetaljer = aktiveDetaljer(packet)

        val melding =
            """
            |Meldekortberegning trenger kontrollregning
            |*Behandling ID*: $behandlingId
            |*Behandlet hendelse ID*: $behandletHendelseId
            |*Detaljer*: $aktiveDetaljer
            """.trimMargin()

        withLoggingContext("behandlingId" to behandlingId, "behandletHendelseId" to behandletHendelseId) {
            logger.info { "Publiserer kontrollbehov til Slack" }
            slackClient.meldekortKontrollbehov(melding)
        }
    }

    private fun aktiveDetaljer(packet: JsonMessage): String =
        packet["detaljer"]
            .fields()
            .asSequence()
            .filter { (_, verdi) -> verdi.isBoolean && verdi.asBoolean() }
            .map { (nøkkel, _) -> nøkkel }
            .sorted()
            .joinToString(", ")
            .ifBlank { "Ingen aktive detaljer" }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
