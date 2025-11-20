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

internal class UtbetalingStatusMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: VedtakBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireAny("@event_name", UTBETALING_EVENTER) }
                validate {
                    it.requireKey("behandlingId", "sakId", "behandletHendelseId", "@opprettet")
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
        val eventName = packet["@event_name"].asText()
        val behandlingId = packet["behandlingId"].asText()

        withLoggingContext("behandlingId" to behandlingId) {
            logger.info { "Mottok utbetaling hendelse: $eventName for behandlingId: $behandlingId" }

            val tekst = createSlackMessage(eventName, packet) ?: return
            logger.info { "$tekst (slackbot er konfiguert? ${slackClient != null})" }
            slackClient?.utbetalingStatus(
                tekst = tekst,
                eksternSakId = packet["eksternSakId"].asText(),
                behandlingId = behandlingId,
                opprettet = packet["@opprettet"].asLocalDateTime(),
            )
        }
    }

    private fun createSlackMessage(
        eventName: String,
        packet: JsonMessage,
    ): String? {
        val behandlingId = packet["behandlingId"].asText()
        val sakId = packet["sakId"].asText()
        val eksternSakId = packet["eksternSakId"].asText()
        val eksternBehandlingId = packet["eksternBehandlingId"].asText()
        val behandletHendelseId = packet["behandletHendelseId"].asText()

        val (icon, status) =
            when (eventName) {
                "utbetaling_feilet" -> ":alert:" to "Utbetaling feilet :alert:"
                "utbetaling_utført" -> ":dollar:" to "Utbetaling utført :dagpenger:"
                else -> return null
            }

        return """
        |$icon $status
        |*Behandling:* $behandlingId 
        | - Helved-ref: `$eksternBehandlingId`
        |*SakId:* $sakId 
        | - Helved-ref: `$eksternSakId`
        |*Behandlet hendelse med id:* $behandletHendelseId
            """.trimMargin()
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
        private val UTBETALING_EVENTER =
            listOf(
                "utbetaling_mottatt",
                "utbetaling_sendt",
                "utbetaling_feilet",
                "utbetaling_utført",
            )
    }
}
