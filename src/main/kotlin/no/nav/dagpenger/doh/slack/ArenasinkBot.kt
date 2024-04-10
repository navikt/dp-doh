package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.monitor.BehandlingStatusMonitor
import java.time.LocalDateTime

internal class ArenasinkBot(slackClient: MethodsClient, slackChannelId: String) : SlackBot(
    slackClient,
    slackChannelId,
    username = "dp-arena-sink",
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
        søknadId: String,
        sakId: Int,
        vedtakId: Int,
        status: String,
        rettighet: String,
        utfall: Boolean,
        kildeId: String,
        kildeSystem: String,
        opprettet: LocalDateTime,
    ) {
        val utfallTekst = if (utfall) "Innvilget" else "Avslått"
        chatPostMessage {
            it.iconEmoji(":rowboat:")
            it.blocks {
                section {
                    markdownText(
                        "Vi har opprettet vedtak i Arena med utfall: $utfallTekst",
                    )
                }
                Blocks.divider()
                section {
                    markdownText(
                        """
                        *Søknad ID:* $søknadId
                        *Sak ID:* $sakId
                        *Vedtak ID:* $vedtakId
                        *Vedtak Status:* $status
                        *Rettighet:* $rettighet
                        *Utfall:* $utfallTekst
                        *Kilde ID:* $kildeId
                        *Kilde System:* $kildeSystem
                        """.trimIndent(),
                    )
                }
                Blocks.divider()
                actions {
                    button {
                        text(":ledger: Se behandlingslogg i Kibana")
                        url(Kibana.createUrl(String.format("\"%s\"", kildeId), opprettet.minusHours(1)))
                    }
                }
            }
        }
    }
}
