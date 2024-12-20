package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingAvbruttCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingVedtakCounter
import no.nav.dagpenger.doh.slack.VedtakBot

internal class BehandlingStatusMonitor(
    rapidsConnection: RapidsConnection,
    private val vedtakBot: VedtakBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.forbid("meldingOmVedtakProdusent") // Unngå å telle republiseringer fra dp-saksbehandling
                    it.requireAny(
                        "@event_name",
                        listOf("behandling_opprettet", "forslag_til_vedtak", "vedtak_fattet", "behandling_avbrutt"),
                    )
                }
                validate {
                    it.requireKey("behandlingId", "søknadId")
                    it.interestedIn("@opprettet", "årsak", "avklaringer", "fastsatt", "utfall", "automatisk")
                }
            }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val søknadId = packet["søknadId"].asText()

        withLoggingContext(
            "behandlingId" to behandlingId,
            "søknadId" to søknadId,
        ) {
            val eventName = packet["@event_name"].asText()
            logger.info { "Vi har behandling med $eventName" + "(slackbot er konfiguert? ${vedtakBot != null})" }
            behandlingStatusCounter.labelValues(eventName.lowercase()).inc()

            val status =
                when (eventName) {
                    "behandling_avbrutt" -> Status.BEHANDLING_AVBRUTT
                    "forslag_til_vedtak" -> Status.FORSLAG_TIL_VEDTAK
                    "vedtak_fattet" -> Status.VEDTAK_FATTET
                    "behandling_opprettet" -> return // Vi vil bare telle, ikke poste Slack-melding
                    else -> return
                }

            val utfall = packet["fastsatt"].takeUnless { it.isMissingOrNull() }?.let { it["utfall"].asBoolean() }
            val automatisk = packet["automatisk"].takeIf { automatisk -> automatisk.isBoolean }?.asBoolean()
            val årsak = packet["årsak"].takeUnless { årsak -> årsak.isMissingNode }?.asText()

            if (status == Status.BEHANDLING_AVBRUTT) {
                behandlingAvbruttCounter.labelValues(årsak ?: "Ukjent").inc()
                return
            }

            vedtakBot?.postBehandlingStatus(
                status = status,
                behandlingId = behandlingId,
                søknadId = søknadId,
                opprettet = packet["@opprettet"].asLocalDateTime(),
                årsak = årsak,
                avklaringer = emptyList(),
                /*packet["avklaringer"].map { avklaring ->
                    avklaring["type"].asText()
                },*/
                utfall = utfall,
                automatisk = automatisk,
            )

            if (status == Status.VEDTAK_FATTET && utfall != null && automatisk != null) {
                val automatisering = if (automatisk) "Automatisk" else "Manuell"
                behandlingVedtakCounter.labelValues(utfall.toString(), automatisering).inc()
            }
        }
    }

    internal enum class Status {
        BEHANDLING_AVBRUTT,
        FORSLAG_TIL_VEDTAK,
        VEDTAK_FATTET,
    }
}
