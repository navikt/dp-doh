package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStatusMonitor
import java.time.LocalDateTime

internal class VedtakBot(
    slackClient: MethodsClient,
    slackChannelId: String,
    slackTrådRepository: SlackTrådRepository,
) : SlackBot(
        slackClient,
        slackChannelId,
        username = "dp-behandling",
        slackTrådRepository = slackTrådRepository,
    ) {
    internal fun postBehandlingStatus(
        status: BehandlingStatusMonitor.Status,
        behandlingId: String,
        behandletHendelse: BehandlingStatusMonitor.BehandletHendelse,
        opprettet: LocalDateTime,
        årsak: String? = null,
        utfall: Boolean? = null,
        automatisk: Boolean? = null,
    ) {
        val (hendelseId, hendelseType) = behandletHendelse
        val tekst: String =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT ->
                    """
                    |Behandlingen er avbrutt 
                    |*$hendelseType ID:* $hendelseId 
                    |*Behandling ID:* $behandlingId
                    |${årsak?.let { "*Årsak*: $it" } ?: ""} 
                    """.trimMargin()

                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK ->
                    """Vi har et forslag til vedtak 
                    |*$hendelseType ID:* $hendelseId 
                    |*Behandling ID:* $behandlingId
                    |*Utfall:* ${tolk(utfall)}
                    """.trimMargin()

                BehandlingStatusMonitor.Status.VEDTAK_FATTET ->
                    """Vi har fattet et vedtak 
                    |*$hendelseType ID:* $hendelseId 
                    |*Behandling ID:* $behandlingId
                    |*Behandling*: ${if (automatisk == true) "Automatisk" else "Manuell"}
                    |*Utfall:* ${tolk(utfall)}
                    """.trimMargin()
            }
        chatPostMessage(trådNøkkel = behandlingId) {
            it.iconEmoji(emoji(status))
            it.blocks {
                section {
                    markdownText(tekst)
                }
                behandlingsloggKnapp(status, behandlingId, opprettet)
            }
        }
    }

    internal fun korrigertMeldekort(tekst: String) {
        chatPostMessage {
            it.iconEmoji(":rotating_light:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
            }
        }
    }

    internal fun skalBeregnemeldekort(tekst: String) {
        chatPostMessage {
            it.iconEmoji(":calc:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
            }
        }
    }

    internal fun utbetalingStatus(tekst: String) {
        chatPostMessage {
            it.iconEmoji(":moneybag:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
            }
        }
    }

    private fun tolk(utfall: Boolean?): String =
        when (utfall) {
            true -> "Innvilget :tada:"
            false -> "Avslag :x:"
            else -> "Uavklart (vi klarer ikke å finne utfallet!)"
        }

    private fun emoji(status: BehandlingStatusMonitor.Status): String {
        val emoji =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT -> ":no_entry_sign:"
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK -> ":tada:"
                BehandlingStatusMonitor.Status.VEDTAK_FATTET -> ":checked:"
            }
        return emoji
    }

    private fun LayoutBlockDsl.behandlingsloggKnapp(
        status: BehandlingStatusMonitor.Status,
        behandlingId: String,
        opprettet: LocalDateTime,
    ) {
        if (!skalBehandlingsloggVises(status)) return

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

    private fun skalBehandlingsloggVises(status: BehandlingStatusMonitor.Status): Boolean {
        val visKnapp =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT,
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK,
                BehandlingStatusMonitor.Status.VEDTAK_FATTET,

                -> true
            }
        return visKnapp
    }
}
