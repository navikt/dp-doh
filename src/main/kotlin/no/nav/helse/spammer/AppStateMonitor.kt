package no.nav.helse.spammer

import com.fasterxml.jackson.databind.JsonNode
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

internal class AppStateMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "app_status") }
            validate { it.requireKey("states") }
            validate { it.require("since", JsonNode::asLocalDateTime) }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        packet["states"]
            .fields()
            .asSequence()
            .filter { it.value.isIntegralNumber && it.value.asInt() == 0 }
            .map { it.key }
            .toList()
            .takeIf(List<*>::isNotEmpty)
            ?.also { apps ->
                val appString = apps.last().let { siste ->
                    if (apps.size == 1) siste
                    else apps.subList(0, apps.size - 1).joinToString() + " og $siste"
                }
                slackClient?.postMessage(String.format(
                    "%s har ikke svart på ping sendt %s siden.",
                    appString,
                    humanReadableTime(ChronoUnit.SECONDS.between(packet["since"].asLocalDateTime(), LocalDateTime.now()))
                ))
            }
    }
}
