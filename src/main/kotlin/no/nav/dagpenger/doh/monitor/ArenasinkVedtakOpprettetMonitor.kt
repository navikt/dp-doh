package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.slack.ArenasinkBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ArenasinkVedtakOpprettetMonitor(
    rapidsConnection: RapidsConnection,
    private val arenasinkBot: ArenasinkBot?,
) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "arenasink_vedtak_opprettet")
                it.requireKey("søknadId", "sakId", "vedtakId", "vedtakstatus", "rettighet", "utfall", "kilde")
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
        val søknadId = packet["søknadId"].asText()
        val kildeId = packet["kilde"]["id"].asText()
        withLoggingContext(
            "søknadId" to søknadId,
            "kildeId" to kildeId,
        ) {
            val utfall = packet["utfall"].asBoolean()
            arenasinkBot?.postVedtak(
                packet["søknadId"].asText(),
                packet["sakId"].asInt(),
                packet["vedtakId"].asInt(),
                packet["vedtakstatus"].asText(),
                packet["rettighet"].asText(),
                utfall,
                kildeId,
                packet["kilde"]["system"].asText(),
                packet["@opprettet"].asLocalDateTime(),
            )
            logger.info { "Vi har fattet vedtak med $utfall" + "(slackbot er konfiguert? ${arenasinkBot != null})" }
        }
    }
}
