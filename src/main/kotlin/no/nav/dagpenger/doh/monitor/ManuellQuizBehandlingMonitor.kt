package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.manuellCounter
import no.nav.dagpenger.doh.slack.QuizResultatBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ManuellQuizBehandlingMonitor(
    rapidsConnection: RapidsConnection,
    private val quizResultatBot: QuizResultatBot? = null,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "manuell_behandling")
                it.requireKey(
                    "@opprettet",
                    "søknad_uuid",
                    "seksjon_navn",
                )
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val uuid = packet["søknad_uuid"].asText()
        val årsak = packet["seksjon_navn"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        quizResultatBot?.postManuellBehandling(uuid, opprettet, årsak)
        manuellCounter.labels(årsak).inc()
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
