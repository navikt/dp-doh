package no.nav.helse.spammer

import com.fasterxml.jackson.databind.JsonNode
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.temporal.ChronoUnit
import kotlin.time.ExperimentalTime

@ExperimentalTime
internal class TidITilstandMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
    private val slackThreadDao: SlackThreadDao?
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(TidITilstandMonitor::class.java)
        private val sikkerLog = LoggerFactory.getLogger("tjenestekall")
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "vedtaksperiode_tid_i_tilstand")
                it.requireKey("aktørId", "fødselsnummer", "organisasjonsnummer",
                    "vedtaksperiodeId", "tilstand", "nyTilstand",
                    "timeout_første_påminnelse", "tid_i_tilstand", "endret_tilstand_på_grunn_av.event_name")
                it.require("starttid", JsonNode::asLocalDateTime)
                it.require("sluttid", JsonNode::asLocalDateTime)
                it.require("makstid", JsonNode::asLocalDateTime)
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val tidITilstand = TidITilstand(packet)

        if (tidITilstand.tidITilstand < tidITilstand.forventetTidITilstand) return

        log.info(
            "{} kom seg omsider videre fra {} til {} etter {} fra {} på grunn av mottatt {}. Forventet tid i tilstand var {}",
            keyValue("vedtaksperiodeId", tidITilstand.vedtaksperiodeId),
            keyValue("tilstand", tidITilstand.tilstand),
            keyValue("nyTilstand", tidITilstand.nyTilstand),
            humanReadableTime(tidITilstand.tidITilstand),
            tidITilstand.starttid.format(ISO_LOCAL_DATE_TIME),
            packet["endret_tilstand_på_grunn_av.event_name"].asText(),
            humanReadableTime(tidITilstand.forventetTidITilstand)
        )

        if (tidITilstand.tilstand == "AVVENTER_GODKJENNING" && tidITilstand.nyTilstand == "TIL_INFOTRYGD") return

        if (slackThreadDao == null) return
        slackClient.postMessage(
            slackThreadDao, tidITilstand.vedtaksperiodeId, String.format(
                "Vedtaksperiode <%s|%s> (<%s|tjenestekall>) kom seg videre fra %s til %s etter %s siden %s (på grunn av mottatt %s). Forventet tid i tilstand var %s",
                Kibana.createUrl(
                    String.format("\"%s\" AND NOT level:Debug AND NOT level:Info", tidITilstand.vedtaksperiodeId),
                    tidITilstand.starttid
                ),
                tidITilstand.vedtaksperiodeId,
                Kibana.createUrl(
                    String.format("\"%s\" AND NOT level:Debug AND NOT level:Info", tidITilstand.vedtaksperiodeId),
                    tidITilstand.starttid,
                    null,
                    "tjenestekall-*"
                ),
                tidITilstand.tilstand,
                tidITilstand.nyTilstand,
                humanReadableTime(tidITilstand.tidITilstand),
                tidITilstand.starttid.format(ISO_LOCAL_DATE_TIME),
                packet["endret_tilstand_på_grunn_av.event_name"].asText(),
                humanReadableTime(tidITilstand.forventetTidITilstand)
            )
        )
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {
        sikkerLog.error("forstod ikke vedtaksperiode_tid_i_tilstand:\n${problems.toExtendedReport()}")
    }

    private class TidITilstand(private val packet: JsonMessage) {
        val aktørId: String get() = packet["aktørId"].asText()
        val fødselsnummer: String get() = packet["fødselsnummer"].asText()
        val organisasjonsnummer: String get() = packet["organisasjonsnummer"].asText()
        val vedtaksperiodeId: String get() = packet["vedtaksperiodeId"].asText()
        val tilstand: String get() = packet["tilstand"].asText()
        val nyTilstand: String get() = packet["nyTilstand"].asText()
        val starttid: LocalDateTime get() = packet["starttid"].asLocalDateTime()
        val sluttid: LocalDateTime get() = packet["sluttid"].asLocalDateTime()
        val tidITilstand: Long get() = packet["tid_i_tilstand"].asLong()
        val forventetTidITilstand: Long get() = packet["makstid"].asLocalDateTime()
            .takeUnless { it == LocalDateTime.MAX }
            ?.let { ChronoUnit.SECONDS.between(starttid, it) }
            ?: Long.MAX_VALUE
    }
}
