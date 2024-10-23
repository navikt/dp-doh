package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.slack.ArenasinkBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ArenasinkVedtakFeiletMonitor(
    rapidsConnection: RapidsConnection,
    private val arenasinkBot: ArenasinkBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.requireValue("@event_name", "arenasink_vedtak_feilet")
                    it.requireKey("kilde")
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
        val kildeId = packet["kilde"]["id"].asText()
        withLoggingContext(
            "kildeId" to kildeId,
        ) {
            arenasinkBot?.postFeilet(
                kildeId = kildeId,
                kildeSystem = packet["kilde"]["system"].asText(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
            )
            logger.info { "Vi klarte ikke fatte vedtak i Arena" + "(slackbot er konfiguert? ${arenasinkBot != null})" }
        }
    }
}
