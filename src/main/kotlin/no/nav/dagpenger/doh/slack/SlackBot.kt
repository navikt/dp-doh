package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import com.slack.api.model.kotlin_extension.block.ActionsBlockBuilder
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import mu.KotlinLogging
import no.nav.dagpenger.doh.Kibana
import java.time.LocalDateTime

internal class SlackBot(
    private val slackClient: MethodsClient,
    private val slackChannelId: String
) {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.Slack")
    }

    internal fun postResultat(
        uuid: String?,
        opprettet: LocalDateTime,
        resultat: Boolean,
        årsak: String?
    ) {
        chatPostMessage {
            val resultatTekst = if (resultat)
                "Automatisk innvilgelse" else "Automatisk avslag :no_entry_sign:"

            it.blocks {
                section { ferdigSaksbehandling() }
                section {
                    markdownText("*Resultat:* $resultatTekst\n")
                }
                section(saksinformasjon(uuid, årsak))
                actions {
                    saksbehandlingsloggKnapp(uuid, opprettet)
                }
            }
                .text(
                    String.format(
                        "Prosessen for %s har blitt ferdig med resultatet %",
                        uuid,
                        resultatTekst
                    )
                )
        }
    }

    internal fun postManuellBehandling(uuid: String?, opprettet: LocalDateTime, årsak: String?) = chatPostMessage {
        it.blocks {
            section { ferdigSaksbehandling() }
            section {
                markdownText("*Resultat:* \nManuell saksbehandling i Arena :muscle:")
            }
            section {
                saksinformasjon(uuid, årsak)
            }
            actions {
                saksbehandlingsloggKnapp(uuid, opprettet)
            }
        }
        it.text(
            String.format(
                "På grunn av %s kan ikke søknaden %s automatiseres, den går til manuell behandling i Arena",
                årsak,
                uuid,
            )
        )
    }

    private fun chatPostMessage(block: (it: ChatPostMessageRequestBuilder) -> ChatPostMessageRequestBuilder) =
        slackClient.chatPostMessage {
            it.channel(slackChannelId)
                .iconEmoji(":robot_face:")
                .username("dp-quiz")
            block(it)
        }.let { response ->
            if (!response.isOk) {
                log.error { "Kunne ikke poste på Slack fordi ${response.error}" }
                log.error { response }
            }
        }

    private fun saksinformasjon(uuid: String?, årsak: String?): SectionBlockBuilder.() -> Unit =
        {
            markdownText(
                listOf(
                    "*UUID*: $uuid",
                    "**: $årsak",
                ).joinToString("\n")
            )
        }

    private fun SectionBlockBuilder.ferdigSaksbehandling() =
        plainText(":checkered_flag: Jeg har saksbehandlet en søknad!")

    private fun ActionsBlockBuilder.saksbehandlingsloggKnapp(uuid: String?, opprettet: LocalDateTime) = button {
        text(":ledger: Se saksbehandlingslogg")
        url(Kibana.createUrl(String.format("\"%s\"", uuid), opprettet.minusHours(1)))
    }
}
