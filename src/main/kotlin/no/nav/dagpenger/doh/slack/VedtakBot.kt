package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import com.slack.api.model.kotlin_extension.block.dsl.LayoutBlockDsl
import no.nav.dagpenger.doh.OpenSearch
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
        førteTil: String? = null,
        automatisk: Boolean? = null,
    ) {
        val (hendelseId, hendelseType) = behandletHendelse
        val tekst: String =
            when (status) {
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT -> {
                    """
                    *Behandling avbrutt:* ${årsak ?: "Ukjent årsak"}
                    *Gjelder:* $hendelseType
                    *Referanser:* ${referanseTekst(hendelseType, hendelseId, behandlingId)}
                    """.trimIndent()
                }

                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK -> {
                    """
                    *Forslag til vedtak:* ${utfallTekst(førteTil)}
                    *Gjelder:* $hendelseType
                    *Referanser:* ${referanseTekst(hendelseType, hendelseId, behandlingId)}
                    """.trimIndent()
                }

                BehandlingStatusMonitor.Status.VEDTAK_FATTET -> {
                    """
                    *Vedtak fattet:* ${utfallTekst(førteTil)}
                    *Behandlingsmåte:* ${if (automatisk == true) "Automatisk" else "Manuell"}
                    *Gjelder:* $hendelseType
                    *Referanser:* ${referanseTekst(hendelseType, hendelseId, behandlingId)}
                    """.trimIndent()
                }
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

    internal fun meldekortKontrollbehov(tekst: String) {
        chatPostMessage {
            it.iconEmoji(":warning:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
            }
        }
    }

    internal fun utbetalingStatus(
        tekst: String,
        eksternSakId: String,
        behandlingId: String,
        opprettet: LocalDateTime,
    ) {
        chatPostMessage(trådNøkkel = behandlingId, replyBroadCast = true) {
            it.iconEmoji(":moneybag:")
            it.blocks {
                section {
                    markdownText(tekst)
                }
                actions {
                    button {
                        text(":ledger: Søk etter Helved-referanse i OpenSearch")
                        url(
                            OpenSearch.createUrl(
                                String.format("\"%s\"", eksternSakId),
                                opprettet.minusHours(1),
                            ),
                        )
                    }
                }
            }
        }
    }

    private fun utfallTekst(førteTil: String?): String {
        val utfall = førteTil ?: "Ukjent utfall"
        return " førte til: $utfall ${førteTilEmoji(førteTil)}".trim()
    }

    private fun referanseTekst(
        hendelseType: String,
        hendelseId: String,
        behandlingId: String,
    ): String = "$hendelseType ID: `$hendelseId`, Behandling ID: `$behandlingId`"

    private fun førteTilEmoji(førteTil: String?): String =
        when (førteTil) {
            "Innvilgelse" -> ":tada:"
            "Avslag" -> ":no_entry_sign:"
            "Stans" -> ":stop_sign:"
            "Gjenopptak" -> ":repeat:"
            "Endring" -> ":pencil2:"
            else -> ""
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
                text(":ledger: Se behandlingslogg i OpenSearch")
                url(
                    OpenSearch.createUrl(
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
