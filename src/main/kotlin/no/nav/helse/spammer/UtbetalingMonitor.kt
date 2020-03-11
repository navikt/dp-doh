package no.nav.helse.spammer

import no.nav.helse.rapids_rivers.*
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class UtbetalingMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("endringstidspunkt") }
            validate { it.requireAny("gjeldendeTilstand", listOf("UTBETALT", "UTBETALING_FEILET")) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        if (packet["gjeldendeTilstand"].asText() == "UTBETALING_FEILET") return utbetalingFeilet(packet)
        utbetalingOk(packet)
    }

    private fun utbetalingOk(packet: JsonMessage) {
        slackClient?.postMessage(
            String.format(
                "Utbetaling for vedtaksperiode <%s|%s> (<%s|tjenestekall>) gikk OK",
                Kibana.createUrl(String.format("\"%s\"", packet["vedtaksperiodeId"].asText()), packet["endringstidspunkt"].asLocalDateTime().minusHours(1)),
                packet["vedtaksperiodeId"].asText(),
                Kibana.createUrl(
                    String.format("\"%s\"", packet["vedtaksperiodeId"].asText()),
                    packet["endringstidspunkt"].asLocalDateTime().minusHours(1),
                    null,
                    "tjenestekall-*"
                )
            )
        )
    }

    private fun utbetalingFeilet(packet: JsonMessage) {
        slackClient?.postMessage(
            String.format(
                "Utbetaling for vedtaksperiode <%s|%s> (<%s|tjenestekall>) feilet!",
                Kibana.createUrl(String.format("\"%s\"", packet["vedtaksperiodeId"].asText()), packet["endringstidspunkt"].asLocalDateTime().minusHours(1)),
                packet["vedtaksperiodeId"].asText(),
                Kibana.createUrl(
                    String.format("\"%s\"", packet["vedtaksperiodeId"].asText()),
                    packet["endringstidspunkt"].asLocalDateTime().minusHours(1),
                    null,
                    "tjenestekall-*"
                )
            )
        )
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {}
}
