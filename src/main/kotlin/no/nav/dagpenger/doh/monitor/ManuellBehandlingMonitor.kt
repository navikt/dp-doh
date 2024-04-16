package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.manuellCounter
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ManuellBehandlingMonitor(
    rapidsConnection: RapidsConnection,
    private val vedtakBot: VedtakBot? = null,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAllOrAny("@behov", listOf("AvklaringManuellBehandling"))
                it.requireValue("@løsning.AvklaringManuellBehandling", true)
                it.requireArray("vurderinger")  {
                    it.requireKey("utfall", "begrunnelse")
                }
                it.requireKey("søknadId", "behandlingId", "vurderinger", "@opprettet")
            }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val årsaker =
            packet["vurderinger"].filter { it["utfall"].asText() == "Manuell" }.map { it["begrunnelse"].asText() }
        val opprettet = packet["@opprettet"].asLocalDateTime()
        vedtakBot?.postManuellBehandling(behandlingId, årsaker, opprettet)
        årsaker.forEach {
            manuellCounter.labels(it).inc()
        }
    }
}
