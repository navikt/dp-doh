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
        val begrunnelse = begrunnelse(packet)

        val melding =
            """
            |Meldekortberegning trenger kontrollregning
            |*Behandling ID*: $behandlingId
            |*Behandlet hendelse ID*: $behandletHendelseId
            |*Begrunnelse*:
            |$begrunnelse
            """.trimMargin()

        withLoggingContext("behandlingId" to behandlingId, "behandletHendelseId" to behandletHendelseId) {
            logger.info { "Publiserer kontrollbehov til Slack" }
            slackClient.meldekortKontrollbehov(melding)
        }
    }

    private fun begrunnelse(packet: JsonMessage): String {
        val begrunnelser =
            packet["detaljer"]
                .fields()
                .asSequence()
                .filter { (_, verdi) -> verdi.isBoolean && verdi.asBoolean() }
                .map { (nøkkel, _) -> detaljTilTekst(nøkkel) }
                .sorted()
                .toList()

        return if (begrunnelser.isEmpty()) {
            "- Kontrollbehov flagget uten spesifisert detalj."
        } else {
            begrunnelser.joinToString(separator = "\n") { "- $it" }
        }
    }

    private fun detaljTilTekst(nøkkel: String): String =
        when (nøkkel) {
            "meldekortSendtForSent" -> "Meldekortet er sent for sent"
            "harMeldtAnnenAktivitet" -> "Inneholder annen aktivitet enn arbeid"
            "harMeldtArbeidstimer" -> "Inneholder dager med arbeidstimer"
            "harEndringISats" -> "Har fått endret sats"
            "harEndringiArbeidstid" -> "Har fått endret arbeidstid"
            "harEndringITerskel" -> "Har fått endring i terskel for tap av arbeid"
            "ileggesSanksjon" -> "Har blitt ilagt sanksjon"
            "harEndretRettighetsperiode" -> "Har fått endret rettighetsperiode (stans/gjenopptak)"
            else -> nøkkel
        }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
