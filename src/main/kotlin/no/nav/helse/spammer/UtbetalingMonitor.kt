package no.nav.helse.spammer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class UtbetalingMonitor(
    rapidsConnection: RapidsConnection,
    slackClient: SlackClient?
) {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("endringstidspunkt") }
            validate { it.requireValue("gjeldendeTilstand", "UTBETALING_FEILET") }
        }.register(UtbetalingFeilet(slackClient))

        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("endringstidspunkt") }
            validate { it.requireValue("forrigeTilstand", "TIL_UTBETALING") }
            validate { it.requireValue("gjeldendeTilstand", "AVSLUTTET") }
        }.register(UtbetalingOk(slackClient))
    }

    private class UtbetalingFeilet(private val slackClient: SlackClient?): River.PacketListener {
        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
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
    }

    private class UtbetalingOk(private val slackClient: SlackClient?): River.PacketListener {
        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
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
    }
}
