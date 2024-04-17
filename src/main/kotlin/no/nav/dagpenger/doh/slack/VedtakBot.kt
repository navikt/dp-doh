package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStatusMonitor
import java.time.LocalDateTime

internal class VedtakBot(slackClient: MethodsClient, slackChannelId: String, slackTrådRepository: SlackTrådRepository) : SlackBot(
    slackClient,
    slackChannelId,
    username = "dp-behandling",
    slackTrådRepository = slackTrådRepository,
) {
    internal fun postBehandlingStatus(
        status: BehandlingStatusMonitor.Status,
        behandlingId: String,
        søknadId: String,
        opprettet: LocalDateTime,
    ) {
        val tekst: String =
            when (status) {
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK -> "Vi har et forslag til vedtak :tada:  "
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT -> "Behandlingen er avbrutt :no_entry_sign:  "
                BehandlingStatusMonitor.Status.BEHANDLING_OPPRETTET -> """
                    Vi opprettet en behandling basert på søknad
                    *Søknad ID:* $søknadId """.trimIndent()
            }
        chatPostMessage(trådNøkkel = søknadId) {
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
        søknadId: String,
        opprettet: LocalDateTime,
    ) {
        val utfallTekst = if (utfall) "Innvilget" else "Avslått"
        chatPostMessage(trådNøkkel = søknadId) {
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
        chatPostMessage(trådNøkkel = søknadId) {
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
