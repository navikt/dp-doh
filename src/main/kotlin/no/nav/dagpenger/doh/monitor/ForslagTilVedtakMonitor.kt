package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ForslagTilVedtakMonitor(rapidsConnection: RapidsConnection, private val vedtakBot: VedtakBot?) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.demandAllOrAny("@event_name", listOf("forslag_til_vedtak", "behandling_avbrutt"))
                it.requireKey("behandlingId", "gjelderDato")
                it.interestedIn("@opprettet")
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
        withLoggingContext(mapOf("behandlingId" to behandlingId)) {
            val status = when(packet["@event_name"].asText()) {
                "forslag_til_vedtak" -> Status.FORSLAG_TIL_VEDTAK
                "behandling_avbrutt" -> Status.BEHANDLING_AVBRUTT
                else -> null
            }
            status?.let { vedtakBot?.postBehandlingStatus(it, behandlingId, packet["@opprettet"].asLocalDateTime()) }
            logger.info { "Vi har behandling med $status" + "(slackbot er konfiguert? ${vedtakBot != null})" }
        }

    }
    internal enum class Status {
        FORSLAG_TIL_VEDTAK,
        BEHANDLING_AVBRUTT
    }
}
