package no.nav.dagpenger.doh.monitor.quiz

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import no.nav.dagpenger.doh.slack.QuizMalBot

internal class NyQuizMalMonitor(
    rapidsConnection: RapidsConnection,
    private val quizMalBot: QuizMalBot? = null,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "ny_quiz_mal")
                    it.requireKey(
                        "@opprettet",
                        "versjon_navn",
                        "versjon_id",
                    )
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val navn = packet["versjon_navn"].asText()
        val versjonId = packet["versjon_id"].asInt()
        quizMalBot?.postNyMal(navn, versjonId)
    }
}
