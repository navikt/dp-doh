package no.nav.helse.spammer

import net.logstash.logback.argument.StructuredArguments.keyValue
import no.nav.helse.rapids_rivers.*
import org.slf4j.LoggerFactory
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME

internal class PåminnelseMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
    private val slackThreadDao: SlackThreadDao?
) : River.PacketListener {

    private companion object {
        private val log = LoggerFactory.getLogger(PåminnelseMonitor::class.java)
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "påminnelse") }
            validate { it.requireKey("vedtaksperiodeId") }
            validate { it.requireKey("antallGangerPåminnet") }
            validate { it.requireKey("tilstandsendringstidspunkt") }
            validate { it.requireKey("tilstand") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        val påminnelse = Påminnelse(packet)
        if (2 > påminnelse.antallGangerPåminnet) return
        alert(påminnelse)
    }

    override fun onError(problems: MessageProblems, context: RapidsConnection.MessageContext) {}

    private fun alert(påminnelse: Påminnelse) {
        log.error(
            "{} sitter fast i {}; har blitt påminnet {} ganger siden {}",
            keyValue("vedtaksperiodeId", påminnelse.vedtaksperiodeId),
            keyValue("tilstand", påminnelse.tilstand),
            keyValue("antallGangerPåminnet", påminnelse.antallGangerPåminnet),
            keyValue("tilstandsendringstidspunkt", påminnelse.endringstidspunkt.format(ISO_LOCAL_DATE_TIME))
        )

        if (slackThreadDao == null) return

        slackClient.postMessage(
            slackThreadDao, påminnelse.vedtaksperiodeId, String.format(
                "Vedtaksperiode <%s|%s> (<%s|tjenestekall>) sitter fast i tilstand %s. Den er forsøkt påminnet %d ganger siden %s",
                Kibana.createUrl(String.format("\"%s\"", påminnelse.vedtaksperiodeId), påminnelse.endringstidspunkt),
                påminnelse.vedtaksperiodeId,
                Kibana.createUrl(
                    String.format("\"%s\"", påminnelse.vedtaksperiodeId),
                    påminnelse.endringstidspunkt,
                    null,
                    "tjenestekall-*"
                ),
                påminnelse.tilstand,
                påminnelse.antallGangerPåminnet,
                påminnelse.endringstidspunkt.format(ISO_LOCAL_DATE_TIME)
            )
        )
    }

    private class Påminnelse(private val packet: JsonMessage) {
        val vedtaksperiodeId: String get() = packet["vedtaksperiodeId"].asText()
        val tilstand: String get() = packet["tilstand"].asText()
        val endringstidspunkt get() = packet["tilstandsendringstidspunkt"].asLocalDateTime()
        val antallGangerPåminnet: Int get() = packet["antallGangerPåminnet"].asInt()
    }
}
