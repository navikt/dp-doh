package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.doh.slack.SlackClient

internal class SaksbehandlingAlertMonitor(rapidsConnection: RapidsConnection, private val slackClient: SlackClient) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "saksbehandling_alert")
                }
                validate {
                    it.requireKey(
                        "alertType",
                        "feilMelding",
                        "utvidetFeilMelding",
                    )
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val alertType = packet["alertType"].asText()
        val feilMelding = packet["feilMelding"].asText()
        val utvidetFeilMelding = packet["utvidetFeilMelding"].asText()

        runBlocking {
            slackClient.postMessage(
                text = "Saksbehandling Alert: $alertType\n$feilMelding\n$utvidetFeilMelding",
            )
        }
    }
}
