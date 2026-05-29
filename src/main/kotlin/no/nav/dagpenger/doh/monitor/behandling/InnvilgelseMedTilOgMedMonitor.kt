package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDate
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.slack.RampBot
import java.time.LocalDate

internal class InnvilgelseMedTilOgMedMonitor(
    rapidsConnection: RapidsConnection,
    private val rampBot: RampBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behandlingsresultat")
                }
                validate {
                    it.requireKey(
                        "behandletHendelse",
                        "behandlingId",
                        "behandlingskjedeId",
                        "ident",
                        "automatisk",
                        "rettighetsperioder",
                        "opprettet",
                    )
                }
                validate { it.forbidValue("regelverk", "Ferietillegg") }
            }.register(this)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val behandlingskjedeId = packet["behandlingskjedeId"].asText()
        val opprettet = packet["opprettet"].asLocalDateTime()

        packet["rettighetsperioder"]
            .map { rettighetsperiode ->
                Rettighetsperiode(
                    fraOgMed = rettighetsperiode["fraOgMed"].asLocalDate(),
                    tilOgMed = rettighetsperiode["tilOgMed"]?.asLocalDate(),
                    harRett = rettighetsperiode["harRett"].asBoolean(),
                    opprinnelse = rettighetsperiode["opprinnelse"].asText(),
                )
            }.filter { it.tilOgMed != null && it.opprinnelse == "Ny" }
            .takeUnless { it.isEmpty() }
            ?.let {
                withLoggingContext(
                    "behandlingId" to behandlingId,
                    "event_name" to "behandlingsresultat",
                    "behandlingskjedeId" to behandlingskjedeId,
                ) {
                    logger.info {
                        "Mottatt innvilgelsesvedtak med tilOgMed-dato: $it"
                    }

                    rampBot?.postInnvilgelseMedTilOgMed(
                        behandlingId = behandlingId,
                        behandlingskjedeId = behandlingskjedeId,
                        opprettet = opprettet,
                    )
                }
            }
    }
}

private data class Rettighetsperiode(
    val fraOgMed: LocalDate,
    val tilOgMed: LocalDate?,
    val harRett: Boolean,
    val opprinnelse: String,
)
