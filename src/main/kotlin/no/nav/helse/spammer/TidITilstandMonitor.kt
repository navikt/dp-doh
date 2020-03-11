package no.nav.helse.spammer

import io.prometheus.client.Histogram
import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import kotlin.time.ExperimentalTime
import kotlin.time.days
import kotlin.time.hours
import kotlin.time.minutes

@ExperimentalTime
internal class TidITilstandMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
    private val slackThreadDao: SlackThreadDao?
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(TidITilstandMonitor::class.java)
        private val histogram = Histogram.build(
            "vedtaksperiode_tilstand_latency_seconds",
            "Antall sekunder en vedtaksperiode er i en tilstand"
        )
            .labelNames("tilstand")
            .buckets(
                1.minutes.inSeconds,
                1.hours.inSeconds,
                12.hours.inSeconds,
                24.hours.inSeconds,
                7.days.inSeconds,
                30.days.inSeconds
            )
            .register()
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "vedtaksperiode_tid_i_tilstand") }
            validate { it.requireKey("aktørId") }
            validate { it.requireKey("fødselsnummer") }
            validate { it.requireKey("organisasjonsnummer") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("tilstand") }
            validate { it.requireKey("nyTilstand") }
            validate { it.requireKey("starttid") }
            validate { it.requireKey("sluttid") }
            validate { it.requireKey("timeout") }
            validate { it.requireKey("tid_i_tilstand") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val tidITilstand = TidITilstand(packet)

        if (tidITilstand.forventetTidITilstand == 0L || tidITilstand.tidITilstand < tidITilstand.forventetTidITilstand) return

        log.warn(
            "{} kom seg omsider videre fra {} til {} etter {} fra {}. Forventet tid i tilstand var {}",
            keyValue("vedtaksperiodeId", tidITilstand.vedtaksperiodeId),
            keyValue("tilstand", tidITilstand.tilstand),
            keyValue("nyTilstand", tidITilstand.nyTilstand),
            humanReadableTime(tidITilstand.tidITilstand),
            tidITilstand.starttid.format(ISO_LOCAL_DATE_TIME),
            humanReadableTime(tidITilstand.forventetTidITilstand)
        )

        if (tidITilstand.tilstand == "AVVENTER_GODKJENNING" && tidITilstand.nyTilstand == "TIL_INFOTRYGD") return

        if (slackThreadDao == null) return
        slackClient.postMessage(
            slackThreadDao, tidITilstand.vedtaksperiodeId, String.format(
                "Vedtaksperiode <%s|%s> (<%s|tjenestekall>) kom seg videre fra %s til %s etter %s siden %s. Forventet tid i tilstand var %s",
                Kibana.createUrl(String.format("\"%s\"", tidITilstand.vedtaksperiodeId), tidITilstand.starttid),
                tidITilstand.vedtaksperiodeId,
                Kibana.createUrl(
                    String.format("\"%s\"", tidITilstand.vedtaksperiodeId),
                    tidITilstand.starttid,
                    null,
                    "tjenestekall-*"
                ),
                tidITilstand.tilstand,
                tidITilstand.nyTilstand,
                humanReadableTime(tidITilstand.tidITilstand),
                tidITilstand.starttid.format(ISO_LOCAL_DATE_TIME),
                humanReadableTime(tidITilstand.forventetTidITilstand)
            )
        )
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {}

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
        val forventetTidITilstand: Long get() = packet["timeout"].asLong()
    }
}
