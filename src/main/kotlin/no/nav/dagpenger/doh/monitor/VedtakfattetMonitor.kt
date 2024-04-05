package no.nav.dagpenger.doh.monitor

import mu.KotlinLogging
import no.nav.dagpenger.doh.Kibana
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.format.DateTimeFormatter

internal class VedtakfattetMonitor(rapidsConnection: RapidsConnection, private val vedtakBot: VedtakBot?) :
    River.PacketListener {
    init {
        River(rapidsConnection).apply {
            validate {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireKey("behandlingId", "utfall", "gjelderDato")
                it.interestedIn("@opprettet")
            }
        }.register(this)
    }

    private companion object {
        val formatterer = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        val logger = KotlinLogging.logger { }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asText()
        val utfall =
            when (packet["utfall"].asBoolean()) {
                true -> "innvilget"
                false -> "avslått"
            }

        val kibanaUrl =
            Kibana.createUrl(
                String.format("\"%s\"", behandlingId),
                packet["@opprettet"].asLocalDateTime().minusHours(1),
            )
        val melding = """Vi har fattet et vedtak med utfall $utfall (Følge <$kibanaUrl|lenke til Kibana>)"""

        vedtakBot?.postVedtak(
            melding,
        )
        logger.info { melding + "(slackbot er konfiguert? ${vedtakBot != null})" }
    }
}
