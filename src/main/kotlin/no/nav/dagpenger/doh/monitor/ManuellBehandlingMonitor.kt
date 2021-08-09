package no.nav.dagpenger.doh.monitor

import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.dagpenger.doh.slack.SlackBot
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

/**
 * Enn så lenge går 98% til manuell så den lager mer støy enn den gir informasjon.
 * Skrur den av, også kan vi heller skru den på igjen i framtida om vi øker graden av automatiske
 */
internal class ManuellBehandlingMonitor(
    rapidsConnection: RapidsConnection,
    private val slackBot: SlackBot?,
) : River.PacketListener {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
        private val manuellCounter = Counter
            .build("dp_manuell_behandling", "Søknader som blir sendt til manuell behandling")
            .labelNames("grunn")
            .register()
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "manuell_behandling")
                it.requireKey(
                    "@opprettet",
                    "søknad_uuid",
                    "seksjon_navn"
                )
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val uuid = packet["søknad_uuid"].asText()
        val årsak = packet["seksjon_navn"].asText()
        val opprettet = packet["@opprettet"].asLocalDateTime()
        slackBot?.postManuellBehandling(uuid, opprettet, årsak)
        manuellCounter.labels(årsak).inc()
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
