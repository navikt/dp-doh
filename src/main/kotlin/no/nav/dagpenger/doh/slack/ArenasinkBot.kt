package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks
import com.slack.api.model.block.Blocks
import no.nav.dagpenger.doh.Kibana
import java.time.LocalDateTime

internal class ArenasinkBot(
    slackClient: MethodsClient,
    slackChannelId: String,
    slackTrådRepository: SlackTrådRepository,
) : SlackBot(
        slackClient,
        slackChannelId,
        username = "dp-arena-sink",
        slackTrådRepository = slackTrådRepository,
    ) {
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
        chatPostMessage(trådNøkkel = kildeId) {
            it.iconEmoji(":sink:")
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

    fun postFeilet(
        kildeId: String,
        kildeSystem: String,
        feiltype: String,
        opprettet: LocalDateTime,
    ) {
        chatPostMessage(trådNøkkel = kildeId) {
            it.iconEmoji(":sink:")
            it.blocks {
                section {
                    markdownText(
                        "Vi klarte ikke å opprette vedtak i Arena",
                    )
                }
                Blocks.divider()
                section {
                    markdownText(
                        """
                        *Behandling ID:* $kildeId    
                        *Kilde System:* $kildeSystem
                        *Feiltype:* $feiltype
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
