package no.nav.dagpenger.doh.monitor

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class ManuellBehandlingMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: MethodsClient?,
    private val slackChannelId: String
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val manuellCounter = Counter
            .build("dp_manuell_behandling", "Søknader som blir sendt til manuell behandling")
            .labelNames("grunn")
            .register()
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "manuell_behandling")
                it.requireKey("søknad_uuid")
                it.requireKey("seksjon_navn")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        manuellCounter.labels(packet["seksjon_navn"].asText()).inc()

        slackClient?.chatPostMessage {
            val uuid = packet["søknad_uuid"].asText()
            val årsak = packet["seksjon_navn"].asText()

            it.channel(slackChannelId)
                .blocks {
                    section {
                        plainText(":office_worker: Søknad gikk til manuell behandling i Arena")
                    }
                    section {
                        markdownText(
                            listOf(
                                "*UUID*: $uuid",
                                "*Årsak*: $årsak",
                            ).joinToString("\n")
                        )
                    }
                    actions {
                        button {
                            text(":ledger: Se saksbehandlingslogg")
                            url("kibana")
                        }
                    }
                }
                .text(
                    String.format(
                        "På grunn av %s kan ikke søknaden %s automatiseres, den går til manuell behandling i Arena",
                        årsak,
                        uuid,
                    )
                )
                .iconEmoji(":sadpanda:")
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
