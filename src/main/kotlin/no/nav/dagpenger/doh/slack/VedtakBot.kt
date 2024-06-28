package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import com.slack.api.model.block.composition.BlockCompositions.markdownText
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
        søknadId: String,
        opprettet: LocalDateTime,
        årsak: String? = null,
        avklaringer: List<String>,
        utfall: Boolean? = null,
    ) {
        val tekst: String =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT ->
                    """
                    |Behandlingen er avbrutt 
                    |*Søknad ID:* $søknadId 
                    |*Behandling ID:* $behandlingId
                    |${årsak?.let { "*Årsak*: $it" } ?: ""} 
                    """.trimMargin()

                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK ->
                    """Vi har et forslag til vedtak 
                    |*Søknad ID:* $søknadId 
                    |*Behandling ID:* $behandlingId
                    |*Utfall:* ${if (utfall == null) "Innvilget :tada:" else "Avslag :no_entry_sign:"}
                    |*Avklaringer*: ${avklaringer.joinToString()}
                    """.trimMargin()

                BehandlingStatusMonitor.Status.VEDTAK_FATTET ->
                    """Vi har fattet et vedtak 
                    |*Søknad ID:* $søknadId 
                    |*Behandling ID:* $behandlingId
                    |*Utfall:* ${if (utfall == null) "Innvilget :tada:" else "Avslag :no_entry_sign:"}
                    """.trimMargin()
            }
        chatPostMessage(trådNøkkel = søknadId) {
            it.iconEmoji(emoji(status))
            it.blocks {
                section {
                    markdownText(tekst)
                }
                behandlingsloggKnapp(status, behandlingId, opprettet)
            }
        }
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
