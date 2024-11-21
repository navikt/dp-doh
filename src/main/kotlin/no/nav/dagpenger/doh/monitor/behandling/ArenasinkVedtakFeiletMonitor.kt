package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.doh.slack.ArenasinkBot

internal class ArenasinkVedtakFeiletMonitor(
    rapidsConnection: RapidsConnection,
    private val arenasinkBot: ArenasinkBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "arenasink_vedtak_feilet") }
                validate {
                    it.requireKey("kilde")
                    it.interestedIn("@opprettet", "feiltype")
                }
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
        val kildeId = packet["kilde"]["id"].asText()
        val feiltype = packet["feiltype"].asText()
        withLoggingContext(
            "kildeId" to kildeId,
        ) {
            arenasinkBot?.postFeilet(
                kildeId = kildeId,
                kildeSystem = packet["kilde"]["system"].asText(),
                feiltype = feiltype,
                opprettet = packet["@opprettet"].asLocalDateTime(),
            )
            logger.info { "Vi klarte ikke fatte vedtak i Arena" + "(slackbot er konfiguert? ${arenasinkBot != null})" }
        }
    }
}
