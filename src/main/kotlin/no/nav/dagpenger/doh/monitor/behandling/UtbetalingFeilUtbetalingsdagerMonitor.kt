package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.VedtakBot

internal class UtbetalingFeilUtbetalingsdagerMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: VedtakBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "utbetaling_feil_grensedato") }
                validate {
                    it.requireKey("behandlingId", "sakId", "@opprettet", "førsteUtbetalingsdag", "førsteDagFraHelVed")
                    it.interestedIn("eksternBehandlingId", "eksternSakId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asString()

        withLoggingContext("behandlingId" to behandlingId) {
            val tekst = createSlackMessage(packet) ?: return
            logger.info { "$tekst (slackbot er konfiguert? ${slackClient != null})" }
            slackClient?.utbetalingStatus(
                tekst = tekst,
                eksternSakId = packet["eksternSakId"].asString(),
                behandlingId = behandlingId,
                opprettet = packet["@opprettet"].asLocalDateTime(),
            )
        }
    }

    private fun createSlackMessage(packet: JsonMessage): String {
        val behandlingId = packet["behandlingId"].asString()
        val sakId = packet["sakId"].asString()
        val eksternSakId = packet["eksternSakId"].asString()
        val eksternBehandlingId = packet["eksternBehandlingId"].asString()

        return """
        |${":alert: :alert: :alert: Utbetalingsdager stemmer ikke med behandling"}
        |*Forventet dato:* ${packet["førsteUtbetalingsdag"].asString()}
        |*Dato fra Hel Ved:* ${packet["førsteDagFraHelVed"].asString()}
        |*Behandling:* $behandlingId 
        | - Helved-ref: `$eksternBehandlingId`
        |*SakId:* $sakId 
        | - Helved-ref: `$eksternSakId`
            """.trimMargin()
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
