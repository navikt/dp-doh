package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import no.nav.dagpenger.doh.OpenSearch
import java.time.LocalDateTime
import java.time.LocalDateTime.now

internal class RampBot(
    slackClient: MethodsClient,
    slackChannelId: String,
    slackTrådRepository: SlackTrådRepository,
) : SlackBot(
        slackClient,
        slackChannelId,
        slackTrådRepository = slackTrådRepository,
        username = "dp-ramp",
    ) {
    internal fun postBrukerHarMeldekortMedEndretMeldesyklusIArena(
        referanseId: String,
        personId: String,
    ) {
        val tekst =
            """
            |Bruker har meldekort med endret meldesyklus i Arena
            |*ReferanseId:* $referanseId
            |*PersonId:* $personId
            """.trimMargin()

        chatPostMessage(trådNøkkel = referanseId) {
            it.iconEmoji(":warning:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
                actions {
                    button {
                        text(":ledger: Se logg i OpenSearch")
                        url(
                            OpenSearch.createUrl(
                                String.format("\"%s\"", referanseId),
                                now().minusHours(1),
                            ),
                        )
                    }
                    button {
                        text(":slack: Se instruks i Slack")
                        url(text = "https://nav-it.slack.com/docs/T5LNAMWNA/F0B7R87TA01")
                    }
                }
            }
        }
    }

    internal fun postInnvilgelseMedTilOgMed(
        behandlingId: String,
        behandlingskjedeId: String,
        opprettet: LocalDateTime,
    ) {
        val tekst =
            """
            |Innvilgelsesvedtak mottatt med tilOgMed-dato
            |*BehandlingId:* $behandlingId
            |*BehandlingskjedeId:* $behandlingskjedeId
            """.trimMargin()

        chatPostMessage(trådNøkkel = behandlingId) {
            it.iconEmoji(":warning:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
                actions {
                    button {
                        text(":ledger: Se logg i OpenSearch")
                        url(
                            OpenSearch.createUrl(
                                String.format("\"%s\"", behandlingId),
                                opprettet.minusHours(1),
                            ),
                        )
                    }
                    button {
                        text(":slack: Se instruks i Slack")
                        url(text = "https://nav-it.slack.com/docs/T5LNAMWNA/F0B7R87TA01")
                    }
                }
            }
        }
    }
}
