package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.slack.SlackClient

internal class OpprettJournalpostFeiletMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "opprett_journalpost_feilet")
                }
                validate {
                    it.requireKey(
                        "@opprettet",
                        "behovId",
                        "søknadId",
                        "type",
                    )
                }
            }.also {
                if (slackClient == null) return@also
                it.register(this)
            }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behovId = packet["behovId"].asText()
        val søknadId = packet["søknadId"].asText()
        val type = packet["type"].asText()

        runBlocking {
            slackClient?.postMessage(
                text = "Vi har feilet i å opprette journalpost for $type med søknadId $søknadId og behovId $behovId",
                Kibana.createUrl(
                    String.format("\"%s\" AND application:dp-behov-journalforing", behovId),
                    packet["@opprettet"].asLocalDateTime().minusMinutes(5),
                ),
            )
        }
    }
}
