package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import io.prometheus.client.Counter
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.Kibana
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ProsessResultatMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: MethodsClient?,
    private val slackChannelId: String
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val resultatCounter = Counter
            .build("dp_prosessresultat", "Resultat av automatiseringsprosessen")
            .labelNames("resultat")
            .register()
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "prosess_resultat")
                it.requireKey("søknad_uuid", "resultat")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("fakta")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val uuid = packet["søknad_uuid"].asText()
        val årsak = packet["seksjon_navn"].asText()

        resultatCounter.labels(packet["resultat"].asText()).inc()

        withLoggingContext(
            "søknad_uuid" to uuid
        ) {
            log.info { "Mottok manuell behandling" }

            slackClient?.chatPostMessage {
                val saksbehandlingslogg = Kibana.createUrl(
                    String.format("\"%s\"", uuid),
                    packet["@opprettet"].asLocalDateTime().minusHours(1)
                )
                val resultat = if (packet["resultat"].asBoolean())
                    "Automatisk innvilgelse" else "Automatisk avslag :no_entry_sign:"

                it.channel(slackChannelId)
                    .blocks {
                        section {
                            plainText(":checkered_flag: Jeg har saksbehandlet en søknad!")
                        }
                        section {
                            markdownText("*Resultat:* $resultat\n")
                        }
                        section {
                            markdownText(
                                listOf(
                                    "*UUID*: $uuid",
                                    "**: $årsak",
                                ).joinToString("\n")
                            )
                        }
                        actions {
                            button {
                                text(":ledger: Se saksbehandlingslogg")
                                url(saksbehandlingslogg)
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
                    .iconEmoji(":robot_face:")
                    .username("dp-quiz")
            }?.let { response ->
                if (!response.isOk) {
                    log.error { "Kunne ikke poste på Slack fordi ${response.error}" }
                }
            }
        }
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
