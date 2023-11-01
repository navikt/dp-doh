package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.dagpenger.doh.slack.QuizResultatBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ProsessResultatMonitor(
    rapidsConnection: RapidsConnection,
    private val resultatBot: QuizResultatBot?,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val resultatCounter =
            Counter
                .build("dp_prosessresultat", "Resultat av automatiseringsprosessen")
                .labelNames("resultat")
                .register()
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "prosess_resultat")
                it.requireKey("søknad_uuid", "resultat")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("fakta")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        resultatBot?.postResultat(
            uuid = packet["søknad_uuid"].asText(),
            opprettet = packet["@opprettet"].asLocalDateTime(),
            resultat = packet["resultat"].asBoolean(),
        )

        resultatCounter.labels(packet["resultat"].asText()).inc()
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
    ) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
