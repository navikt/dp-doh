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

internal class UtbetalingStatusMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: VedtakBot?,
) : River.PacketListener {
    private val utbetalingEventer =
        listOf("utbetaling_mottatt", "utbetaling_sendt", "utbetaling_feilet", "utbetaling_utført")

    init {
        River(rapidsConnection).apply {
            precondition {
                it.requireAllOrAny(
                    "@event_name",
                    utbetalingEventer,
                )
            }
            validate {
                it.requireKey(
                    "behandlingId",
                    "sakId",
                    "meldekortId",
                )
            }
        }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val eventName = packet["@event_name"].asText()
        val behandlingId = packet["sakId"].asText()

        withLoggingContext(
            "behandlingId" to behandlingId,
        ) {
            val tekst =
                when (eventName) {
                    "utbetaling_sendt" ->
                        """
                        | Utbetaling sendt til utbetaling
                        |*Behandling:* ${packet["behandlingId"].asText()}
                        |*SakId:* $behandlingId
                        |*MeldekortId:* ${packet["meldekortId"].asText()}     
                        """.trimIndent()

                    "utbetaling_feilet" ->
                        """
                    |:alert: Utbetaling feilet :alert:
                    |*Behandling:* ${packet["behandlingId"].asText()}
                    |*SakId:* $behandlingId
                    |*MeldekortId:* ${packet["meldekortId"].asText()}
                        """.trimMargin()

                    "utbetaling_utført" ->
                        """
                    |Utbetaling utført
                    |*Behandling:* ${packet["behandlingId"].asText()}
                    |*SakId:* $behandlingId
                    |*MeldekortId:* ${packet["meldekortId"].asText()}
                        """.trimMargin()

                    else -> return
                }

            logger.info { tekst + "(slackbot er konfiguert? ${slackClient != null})" }

            slackClient?.utbetalingStatus(tekst = tekst)
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
