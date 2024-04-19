package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStatusMonitor
import java.time.LocalDateTime

internal class VedtakBot(slackClient: MethodsClient, slackChannelId: String, slackTrådRepository: SlackTrådRepository) :
    SlackBot(
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
        årsak: String? = null,
    ) {
        val tekst: String =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_OPPRETTET ->
                    """
                    Vi opprettet en behandling basert på søknad
                    *Søknad ID:* $søknadId """.trimIndent()
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT ->
                    """
                    Behandlingen er avbrutt :no_entry_sign:
                    ${årsak?.let { "*Årsak*: $it" } ?: ""} 
                    """.trimIndent()
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK -> "Vi har et forslag til vedtak :tada:"
            }
        val emoji =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_OPPRETTET -> ":dagpenger:"
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT -> ":no_entry_sign:"
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK -> ":tada:"
            }
        val visKnapp =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_OPPRETTET -> true
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT,
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK,
                -> false
            }
        val broadcast = status == BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK
        chatPostMessage(trådNøkkel = søknadId, replyBroadCast = broadcast) {
            it.iconEmoji(":dagpenger:")
            it.iconEmoji(emoji)
            it.blocks {
                section {
                    markdownText(tekst)
                }
                if (visKnapp) {
                    Blocks.divider()
                    actions {
                        button {
                            text(":ledger: Se behandlingslogg i Kibana")
                            url(
                                Kibana.createUrl(
                                    String.format("\"%s\"", behandlingId),
                                    opprettet.minusHours(1),
                                ),
                            )
                        }
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
