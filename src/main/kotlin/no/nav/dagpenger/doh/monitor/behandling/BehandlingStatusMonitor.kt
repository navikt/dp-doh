package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
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
                        listOf("forslag_til_vedtak", "behandling_avbrutt", "behandling_opprettet"),
                    )
                    it.requireKey("behandlingId", "gjelderDato", "søknadId")
                    it.interestedIn("@opprettet", "årsak", "avklaringer")
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
        withLoggingContext(mapOf("behandlingId" to behandlingId, "søknadId" to søknadId)) {
            val status =
                when (packet["@event_name"].asText()) {
                    "behandling_opprettet" -> Status.BEHANDLING_OPPRETTET
                    "forslag_til_vedtak" -> Status.FORSLAG_TIL_VEDTAK
                    "behandling_avbrutt" -> Status.BEHANDLING_AVBRUTT
                    else -> null
                }
            status?.let {
                vedtakBot?.postBehandlingStatus(
                    it,
                    behandlingId,
                    søknadId,
                    packet["@opprettet"].asLocalDateTime(),
                    packet["årsak"].takeUnless { årsak -> årsak.isMissingNode }?.asText(),
                    packet["avklaringer"].map { avklaring ->
                        avklaring["type"].asText()
                    },
                )
            }
            logger.info { "Vi har behandling med $status" + "(slackbot er konfiguert? ${vedtakBot != null})" }

            behandlingStatusCounter.labels(status.toString().lowercase()).inc()
        }
    }

    internal enum class Status {
        FORSLAG_TIL_VEDTAK,
        BEHANDLING_AVBRUTT,
        BEHANDLING_OPPRETTET,
    }
}
