package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import mu.withLoggingContext
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
                        listOf("forslag_til_vedtak", "behandling_avbrutt", "vedtak_fattet"),
                    )
                    it.requireKey("behandlingId", "gjelderDato", "søknadId")
                    it.interestedIn("@opprettet", "årsak", "avklaringer", "utfall", "automatisk")
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
            val status =
                when (packet["@event_name"].asText()) {
                    "behandling_avbrutt" -> Status.BEHANDLING_AVBRUTT
                    "forslag_til_vedtak" -> Status.FORSLAG_TIL_VEDTAK
                    "vedtak_fattet" -> Status.VEDTAK_FATTET
                    else -> return
                }.also {
                    logger.info { "Vi har behandling med $it" + "(slackbot er konfiguert? ${vedtakBot != null})" }
                    behandlingStatusCounter.labels(it.toString().lowercase()).inc()
                }

            val utfall = packet["utfall"].takeIf { utfall -> utfall.isBoolean }?.asBoolean()
            val automatisk = packet["automatisk"].takeIf { automatisk -> automatisk.isBoolean }?.asBoolean()

            vedtakBot?.postBehandlingStatus(
                status,
                behandlingId,
                søknadId,
                packet["@opprettet"].asLocalDateTime(),
                packet["årsak"].takeUnless { årsak -> årsak.isMissingNode }?.asText(),
                packet["avklaringer"].map { avklaring ->
                    avklaring["type"].asText()
                },
                utfall,
                automatisk,
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
