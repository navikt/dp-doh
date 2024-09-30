package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingAvbruttCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingVedtakCounter
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class BehandlingStatusMonitor(
    rapidsConnection: RapidsConnection,
    private val vedtakBot: VedtakBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.requireAny(
                        "@event_name",
                        listOf("behandling_opprettet", "forslag_til_vedtak", "vedtak_fattet", "behandling_avbrutt"),
                    )
                    it.rejectKey("meldingOmVedtakProdusent") // Unngå å telle republiseringer fra dp-saksbehandling
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
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val søknadId = packet["søknadId"].asText()

        withLoggingContext(
            "behandlingId" to behandlingId,
            "søknadId" to søknadId,
        ) {
            val eventName = packet["@event_name"].asText()
            logger.info { "Vi har behandling med $eventName" + "(slackbot er konfiguert? ${vedtakBot != null})" }
            behandlingStatusCounter.labels(eventName.lowercase()).inc()

            val status =
                when (eventName) {
                    "behandling_avbrutt" -> Status.BEHANDLING_AVBRUTT
                    "forslag_til_vedtak" -> Status.FORSLAG_TIL_VEDTAK
                    "vedtak_fattet" -> Status.VEDTAK_FATTET
                    "behandling_opprettet" -> return // Vi vil bare telle, ikke poste Slack-melding
                    else -> return
                }

            val utfall =
                when (status) {
                    Status.VEDTAK_FATTET -> packet["fastsatt"]["utfall"].takeIf { fastsatt -> fastsatt.isBoolean }?.asBoolean()
                    else -> packet["utfall"].takeIf { fastsatt -> fastsatt.isBoolean }?.asBoolean()
                }
            val automatisk = packet["automatisk"].takeIf { automatisk -> automatisk.isBoolean }?.asBoolean()
            val årsak = packet["årsak"].takeUnless { årsak -> årsak.isMissingNode }?.asText()

            if (status == Status.BEHANDLING_AVBRUTT) {
                behandlingAvbruttCounter.labels(årsak ?: "Ukjent").inc()
                return
            }

            vedtakBot?.postBehandlingStatus(
                status = status,
                behandlingId = behandlingId,
                søknadId = søknadId,
                opprettet = packet["@opprettet"].asLocalDateTime(),
                årsak = årsak,
                avklaringer =
                    packet["avklaringer"].map { avklaring ->
                        avklaring["type"].asText()
                    },
                utfall = utfall,
                automatisk = automatisk,
            )

            if (status == Status.VEDTAK_FATTET && utfall != null && automatisk != null) {
                val automatisering = if (automatisk) "Automatisk" else "Manuell"
                behandlingVedtakCounter.labels(utfall.toString(), automatisering).inc()
            }
        }
    }

    internal enum class Status {
        BEHANDLING_AVBRUTT,
        FORSLAG_TIL_VEDTAK,
        VEDTAK_FATTET,
    }
}
