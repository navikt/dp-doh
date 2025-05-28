package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.slack.SlackClient

internal class JournalføringFeiletMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "journalføring_feilet")
                }
                validate {
                    it.requireKey(
                        "ident",
                        "message",
                        "behovId",
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
        val feilmelding = packet["message"].asText()
        val behovId = packet["behovId"].asText()

        runBlocking {
            val tekst =
                """
                Feil ved opprettelse av journalpost i dp-behov-journalforing. Feilmelding:
                $feilmelding
                """.trimIndent()

            slackClient?.postMessage(
                tekst,
                Kibana.createUrl(
                    String.format("\"%s\" AND application:dp-behov-journalforing", behovId),
                    packet["behov_opprettet"].asLocalDateTime().minusMinutes(5),
                ),
            ) ?: logger.error { tekst }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
