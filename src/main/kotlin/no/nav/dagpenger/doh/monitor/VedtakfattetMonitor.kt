package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.format.DateTimeFormatter

internal class VedtakfattetMonitor(rapidsConnection: RapidsConnection, private val vedtakBot: VedtakBot?) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireKey("behandlingId", "vedtakId", "virkningsdato", "utfall")
            }
        }.register(this)
    }

    private companion object {
        val formatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val vedtakId = packet["vedtakId"].asText()
        val behandlingId = packet["behandlingId"].asText()
        val virkningsdato = packet["virkningsdato"].asLocalDate()
        val utfall = packet["utfall"].asText()

        val melding = """Vi har fattet et vedtak med utfall $utfall på virkningsdato ${
            formatterer.format(
                virkningsdato,
            )
        }  (Vedtak $vedtakId for behandling $behandlingId i dev)"""

        vedtakBot?.postVedtak(
            melding,
        )
        logger.info { melding }
    }
}
