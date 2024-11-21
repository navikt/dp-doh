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

internal class ArenasinkVedtakOpprettetMonitor(
    rapidsConnection: RapidsConnection,
    private val arenasinkBot: ArenasinkBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "arenasink_vedtak_opprettet")
                }
                validate {
                    it.requireKey("søknadId", "sakId", "vedtakId", "vedtakstatus", "rettighet", "utfall", "kilde")
                    it.interestedIn("@opprettet")
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
        val søknadId = packet["søknadId"].asText()
        val kildeId = packet["kilde"]["id"].asText()
        withLoggingContext(
            "søknadId" to søknadId,
            "kildeId" to kildeId,
        ) {
            val utfall = packet["utfall"].asBoolean()
            arenasinkBot?.postVedtak(
                søknadId = packet["søknadId"].asText(),
                sakId = packet["sakId"].asInt(),
                vedtakId = packet["vedtakId"].asInt(),
                status = packet["vedtakstatus"].asText(),
                rettighet = packet["rettighet"].asText(),
                utfall = utfall,
                kildeId = kildeId,
                kildeSystem = packet["kilde"]["system"].asText(),
                opprettet = packet["@opprettet"].asLocalDateTime(),
            )
            logger.info { "Vi har fattet vedtak med $utfall" + "(slackbot er konfiguert? ${arenasinkBot != null})" }
        }
    }
}
