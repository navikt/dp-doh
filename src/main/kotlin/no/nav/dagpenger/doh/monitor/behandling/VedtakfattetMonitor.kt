package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.resultatCounter
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
                it.requireKey("behandlingId", "utfall", "gjelderDato", "søknadId")
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
        val søknadId = packet["søknadId"].asText()
        withLoggingContext(mapOf("behandlingId" to behandlingId, "søknadId" to søknadId)) {
            val utfall = packet["utfall"].asBoolean()
            resultatCounter.labels(packet["utfall"].asText()).inc()
            // TODO: Lag bedre counters med resultat og sånt
            behandlingStatusCounter.labels("vedtak_fattet").inc()
            vedtakBot?.postVedtak(utfall, behandlingId, søknadId, packet["@opprettet"].asLocalDateTime())
            logger.info { "Vi har fattet vedtak med $utfall" + "(slackbot er konfiguert? ${vedtakBot != null})" }
        }
    }
}
