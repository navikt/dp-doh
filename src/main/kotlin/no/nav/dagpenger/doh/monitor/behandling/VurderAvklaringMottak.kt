package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.OpenSearch
import no.nav.dagpenger.doh.slack.SlackBot
import java.time.LocalDateTime

internal class RegelKjøringLoopSlackBot(
    slackClient: MethodsClient,
    slackChannelId: String,
) : SlackBot(
        slackClient,
        slackChannelId,
        slackTrådRepository = null,
    ) {
    fun regelkjøringLoop(
        kode: String,
        behandlingId: String,
        opprettet: LocalDateTime,
    ) {
        val message =
            """
            Mottok avklaring med kode $kode 
            """.trimIndent()
        chatPostMessage {
            it.iconEmoji("warning")
            it.blocks {
                section {
                    markdownText(
                        message,
                    )
                }
                Blocks.divider()
                section {
                    markdownText(
                        """
                        *Behandling ID:* $behandlingId
                        """.trimIndent(),
                    )
                }
                Blocks.divider()
                actions {
                    button {
                        text(":ledger: Se behandlingslogg i OpenSearch")
                        url(OpenSearch.createUrl(String.format("\"%s\"", behandlingId.toString()), opprettet.minusHours(1)))
                    }
                }
            }
        }
    }
}

internal class VurderAvklaringMottak(
    rapidsconnection: RapidsConnection,
    private val regelKjøringLoopSlackBot: RegelKjøringLoopSlackBot?,
) : River.PacketListener {
    init {
        River(rapidsconnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "NyAvklaring")
                    it.requireKey("kode")
                }
                validate {
                    it.requireKey("@id", "@opprettet")
                    it.requireKey("avklaringId", "kode")
                    it.requireKey("ident", "behandlingId", "søknadId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val avklaringKode = packet["kode"].asString()
        val avklaringId = packet["avklaringId"].asString()

        withLoggingContext(
            "avklaringId" to avklaringId.toString(),
            "kode" to avklaringKode,
            "behandlingId" to packet["behandlingId"].asString(),
            "søknadId" to packet["søknadId"].asString(),
        ) {
            if (avklaringKode == "REGELKJØRING_LOOP") {
                logger.warn { "Mottok avklaring med kode $avklaringKode, sender til Slack" }
                regelKjøringLoopSlackBot?.regelkjøringLoop(
                    avklaringKode,
                    packet["behandlingId"].asString(),
                    packet["@opprettet"].asLocalDateTime(),
                )
            }
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}
