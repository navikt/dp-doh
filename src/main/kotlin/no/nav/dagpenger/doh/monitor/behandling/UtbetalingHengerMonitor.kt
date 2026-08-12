package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.doh.slack.SlackClient

/**
 * Varsler når en utbetaling har ventet lenge på svar fra Oppdrag/Helved uten at status har endret seg.
 * Hendelsen kan komme flere ganger for samme utbetaling (se dp-mellom-barken-og-veden sin
 * `GjentagendeVarsling`) - `antallVarslerSåLangt` forteller hvor mange ganger dette er varslet om før.
 */
internal class UtbetalingHengerMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "utbetaling_henger")
                }
                validate {
                    it.requireKey(
                        "behandlingId",
                        "sakId",
                        "ventetidMinutter",
                        "antallVarslerSåLangt",
                    )
                }
            }.also {
                if (slackClient == null) return@also
                it.register(this)
            }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val sakId = packet["sakId"].asText()
        val ventetidMinutter = packet["ventetidMinutter"].asLong()
        val antallVarslerSåLangt = packet["antallVarslerSåLangt"].asInt()

        runBlocking {
            slackClient?.postMessage(
                text =
                    """
                    Utbetaling henger, har ventet på svar fra Oppdrag i $ventetidMinutter minutter (varslet $antallVarslerSåLangt. gang)
                    *Behandling:* `$behandlingId`
                    *SakId:* `$sakId`
                    """.trimIndent(),
                emoji = ":rotating_light:",
            )
        }
    }
}
