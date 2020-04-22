package no.nav.helse.spammer

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class UtbetalingMonitor(
    rapidsConnection: RapidsConnection,
    slackClient: SlackClient?,
    slackThreadDao: SlackThreadDao?
) {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("@opprettet") }
            validate { it.requireValue("gjeldendeTilstand", "TIL_UTBETALING") }
        }.register(TilUtbetaling(slackClient, slackThreadDao))

        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("@opprettet") }
            validate { it.requireValue("gjeldendeTilstand", "UTBETALING_FEILET") }
        }.register(UtbetalingFeilet(slackClient, slackThreadDao))

        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_endret") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("@opprettet") }
            validate { it.requireValue("forrigeTilstand", "TIL_UTBETALING") }
            validate { it.requireValue("gjeldendeTilstand", "AVSLUTTET") }
        }.register(UtbetalingOk(slackClient, slackThreadDao))
    }

    private class TilUtbetaling(private val slackClient: SlackClient?, private val slackThreadDao: SlackThreadDao?): River.PacketListener {
        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            if (slackThreadDao == null) return
            slackClient?.postMessage(
                slackThreadDao,
                packet["vedtaksperiodeId"].asText(),
                String.format(
                    "Utbetaling for vedtaksperiode <%s|%s> (<%s|tjenestekall>) er sendt til Spenn/Oppdrag :pray:",
                    Kibana.createUrl(String.format("\"%s\"", packet["vedtaksperiodeId"].asText()), packet["@opprettet"].asLocalDateTime().minusHours(1)),
                    packet["vedtaksperiodeId"].asText(),
                    Kibana.createUrl(
                        String.format("\"%s\"", packet["vedtaksperiodeId"].asText()),
                        packet["@opprettet"].asLocalDateTime().minusHours(1),
                        null,
                        "tjenestekall-*"
                    )
                )
            )
        }
    }

    private class UtbetalingFeilet(private val slackClient: SlackClient?, private val slackThreadDao: SlackThreadDao?): River.PacketListener {
        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            if (slackThreadDao == null) return
            slackClient?.postMessage(
                slackThreadDao,
                packet["vedtaksperiodeId"].asText(),
                String.format(
                    "Utbetaling for vedtaksperiode <%s|%s> (<%s|tjenestekall>) feilet!",
                    Kibana.createUrl(String.format("\"%s\"", packet["vedtaksperiodeId"].asText()), packet["@opprettet"].asLocalDateTime().minusHours(1)),
                    packet["vedtaksperiodeId"].asText(),
                    Kibana.createUrl(
                        String.format("\"%s\"", packet["vedtaksperiodeId"].asText()),
                        packet["@opprettet"].asLocalDateTime().minusHours(1),
                        null,
                        "tjenestekall-*"
                    )
                )
            )
        }
    }

    private class UtbetalingOk(private val slackClient: SlackClient?, private val slackThreadDao: SlackThreadDao?): River.PacketListener {
        override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
            if (slackThreadDao == null) return
            slackClient?.postMessage(
                slackThreadDao,
                packet["vedtaksperiodeId"].asText(),
                String.format(
                    "Utbetaling for vedtaksperiode <%s|%s> (<%s|tjenestekall>) gikk OK",
                    Kibana.createUrl(String.format("\"%s\"", packet["vedtaksperiodeId"].asText()), packet["@opprettet"].asLocalDateTime().minusHours(1)),
                    packet["vedtaksperiodeId"].asText(),
                    Kibana.createUrl(
                        String.format("\"%s\"", packet["vedtaksperiodeId"].asText()),
                        packet["@opprettet"].asLocalDateTime().minusHours(1),
                        null,
                        "tjenestekall-*"
                    )
                )
            )
        }
    }
}
