package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import com.slack.api.model.block.Blocks
import com.slack.api.model.kotlin_extension.block.SectionBlockBuilder
import mu.KotlinLogging
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.monitor.BehandlingStatusMonitor
import java.time.LocalDateTime

internal class QuizResultatBot(slackClient: MethodsClient, slackChannelId: String) : SlackBot(
    slackClient,
    slackChannelId,
) {
    internal fun postResultat(
        uuid: String,
        opprettet: LocalDateTime,
        resultat: Boolean,
    ) {
        chatPostMessage {
            val resultatTekst =
                if (resultat) {
                    "Automatisk innvilgelse"
                } else {
                    "Automatisk avslag :no_entry_sign:"
                }

            it.blocks {
                section { ferdigSaksbehandlet() }
                section {
                    markdownText(
                        listOf(
                            "*Resultat:* $resultatTekst",
                            "*UUID:* $uuid",
                        ).joinToString("\n"),
                    )
                    accessory {
                        image(
                            imageUrl = "https://a.slack-edge.com/production-standard-emoji-assets/13.0/apple-large/2705.png",
                            altText = "Checkmark",
                        )
                    }
                }
                divider()
                actions {
                    button {
                        text(":ledger: Se saksbehandlingslogg")
                        url(Kibana.createUrl(String.format("\"%s\"", uuid), opprettet.minusHours(1)))
                    }
                }
            }
                .text(
                    "Prosessen for $uuid har blitt ferdig med resultatet $resultat",
                )
        }
    }

    internal fun postManuellBehandling(
        uuid: String,
        opprettet: LocalDateTime,
        årsak: String,
    ) = chatPostMessage {
        it.blocks {
            section {
                ferdigSaksbehandlet()
            }
            section {
                val detective =
                    listOf(
                        ":male-detective:",
                        ":female-detective:",
                    ).random()
                markdownText(
                    listOf(
                        "*Resultat:* Manuell saksbehandling i Arena $detective",
                        "*UUID:* $uuid",
                        "*Årsak:* $årsak",
                    ).joinToString("\n"),
                )
                accessory {
                    image(
                        imageUrl = "https://images-na.ssl-images-amazon.com/images/I/61tkcGZeUKL.png",
                        altText = "Sad trombone",
                    )
                }
            }
            divider()
            actions {
                button {
                    text(":ledger: Se saksbehandlingslogg")
                    url(Kibana.createUrl(String.format("\"%s\"", uuid), opprettet.minusHours(1)))
                }
            }
        }
        it.text(
            "På grunn av $årsak kan ikke søknaden $uuid automatiseres, den går til manuell behandling i Arena",
        )
    }

    private fun SectionBlockBuilder.ferdigSaksbehandlet() =
        plainText("Jeg har saksbehandlet en søknad! :checkered_flag: :checkered_flag: :checkered_flag:")
}

internal class QuizMalBot(slackClient: MethodsClient, slackChannelId: String) : SlackBot(slackClient, slackChannelId) {
    internal fun postNyMal(
        navn: String,
        versjonId: Int,
    ) {
        chatPostMessage {
            it.iconEmoji(":robot_face:")
            it.blocks {
                section { plainText("Ny mal med prossesnavn $navn og versjonid $versjonId") }

                actions {
                    button {
                        text(":ledger: Se commit logg fra dp-quiz")
                        url("https://github.com/navikt/dp-quiz/commits/main")
                    }
                }
            }
        }
    }
}

internal class VedtakBot(slackClient: MethodsClient, slackChannelId: String) : SlackBot(
    slackClient,
    slackChannelId,
    username = "dp-behandling",
) {
    internal fun postBehandlingStatus(
        status: BehandlingStatusMonitor.Status,
        behandlingId: String,
        opprettet: LocalDateTime,
    ) {
        val tekst: String =
            when (status) {
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK -> "Vi har et forslag til vedtak :tada: "
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT -> "Behandlingen er avbrutt :no_entry_sign: "
            }
        chatPostMessage {
            it.iconEmoji(":dagpenger:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
                Blocks.divider()
                actions {
                    button {
                        text(":ledger: Se behandlingslogg i Kibana")
                        url(Kibana.createUrl(String.format("\"%s\"", behandlingId), opprettet.minusHours(1)))
                    }
                }
            }
        }
    }

    internal fun postVedtak(
        utfall: Boolean,
        behandlingId: String,
        opprettet: LocalDateTime,
    ) {
        val utfallTekst = if (utfall) "Innvilget" else "Avslått"
        chatPostMessage {
            it.iconEmoji(":dagpenger:")
            it.blocks {
                section {
                    markdownText(
                        "Vi har fattet et vedtak med utfall: $utfallTekst",
                    )
                }
                Blocks.divider()
                actions {
                    button {
                        text(":ledger: Se behandlingslogg i Kibana")
                        url(Kibana.createUrl(String.format("\"%s\"", behandlingId), opprettet.minusHours(1)))
                    }
                }
            }
        }
    }

    fun postManuellBehandling(
        behandlingId: String?,
        søknadId: String,
        årsaker: List<String>,
        opprettet: LocalDateTime,
    ) {
        chatPostMessage {
            it.iconEmoji(":dagpenger:")
            it.blocks {
                section {
                    markdownText(
                        listOf(
                            "*Resultat:* Manuell saksbehandling i Arena 🕵",
                            "*BehandlingId:* $behandlingId",
                            "*SøknadId:* $søknadId",
                            "*Årsaker:* ${årsaker.joinToString()}",
                        ).joinToString("\n"),
                    )
                }
                Blocks.divider()
                actions {
                    button {
                        text(":ledger: Se behandlingslogg i Kibana")
                        url(Kibana.createUrl(String.format("\"%s\"", behandlingId), opprettet.minusHours(1)))
                    }
                }
            }
        }
    }
}

internal abstract class SlackBot(
    private val slackClient: MethodsClient,
    private val slackChannelId: String,
    private val username: String = "dp-quiz",
) {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.Slack")
    }

    protected fun chatPostMessage(block: (it: ChatPostMessageRequestBuilder) -> ChatPostMessageRequestBuilder) =
        slackClient.chatPostMessage {
            it.channel(slackChannelId)
                .iconEmoji(":robot_face:")
                .username(username)
            block(it)
        }.let { response ->
            if (!response.isOk) {
                log.error { "Kunne ikke poste på Slack fordi ${response.errors}" }
                log.error { response }
            }
        }
}
