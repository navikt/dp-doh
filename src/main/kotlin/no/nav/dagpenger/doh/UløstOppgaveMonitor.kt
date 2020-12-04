package no.nav.dagpenger.doh

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class UløstOppgaveMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "oppgave")
                it.requireKey("søknad_uuid")
                it.requireArray("fakta") {
                    requireKey("svar")
                }
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        slackClient?.postMessage(
            text = String.format(
                "Nå må noen løse oppgaver her! <%s|%s> venter på en saksbehandler.",
                "https://arbeid.dev.nav.no/arbeid/dagpenger/saksbehandling/oppgaver",
                packet["søknad_uuid"].asText()
            ),
            emoji = ":tada:"
        )
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
