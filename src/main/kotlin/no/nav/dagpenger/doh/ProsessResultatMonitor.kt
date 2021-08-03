package no.nav.dagpenger.doh

import com.fasterxml.jackson.databind.JsonNode
import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ProsessResultatMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val resultatCounter = Counter
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
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        resultatCounter.labels(packet["resultat"].asText()).inc()

        slackClient?.postMessage(
            text = String.format(
                "Prosessen for <%s|%s> har blitt ferdig med resultatet %s",
                Kibana.createUrl(
                    String.format("\"%s\"", packet["søknad_uuid"].asText()),
                    packet["@opprettet"].asLocalDateTime().minusHours(1)
                ),
                packet["søknad_uuid"].asText(),
                packet["resultat"].asBoolean()
            ),
            username = "dp-quiz",
            emoji = ":tada:"
        )
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
