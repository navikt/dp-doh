package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.dagpenger.doh.slack.SlackBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class NyQuizMalMonitor(
    rapidsConnection: RapidsConnection,
    private val slackBot: SlackBot? = null,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "ny_quiz_mal")
                it.requireKey(
                    "@opprettet",
                    "versjon_navn",
                    "versjon_id"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val navn = packet["versjon_navn"].asText()
        val versjonId = packet["versjon_id"].asInt()
        slackBot?.postNyMal(navn, versjonId)
    }
}
