package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class VedtakfattetMonitor(rapidsConnection: RapidsConnection, private val vedtakBot: VedtakBot?) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireKey("behandlingId", "utfall", "gjelderDato")
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
            val utfall = packet["utfall"].asBoolean()
            vedtakBot?.postVedtak(utfall, behandlingId, packet["@opprettet"].asLocalDateTime())
            logger.info { "Vi har fattet vedtak med $utfall" + "(slackbot er konfiguert? ${vedtakBot != null})" }
        }
    }
}
