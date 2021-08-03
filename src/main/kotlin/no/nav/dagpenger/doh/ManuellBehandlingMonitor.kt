package no.nav.dagpenger.doh

import io.prometheus.client.Counter
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class ManuellBehandlingMonitor(
    rapidsConnection: RapidsConnection,
    private val slackClient: SlackClient?
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
                it.requireKey("søknad_uuid")
                it.requireKey("seksjon_navn")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        manuellCounter.labels(packet["seksjon_navn"].asText()).inc()

        slackClient?.postMessage(
            text = String.format(
                "På grunn av %s kan ikke søknaden %s automatiseres, den går til manuell behandling i Arena",
                packet["seksjon_navn"].asText(),
                packet["søknad_uuid"].asText(),
            ),
            username = "dp-quiz",
            emoji = ":sadpanda:"
        )
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        sikkerlogg.info(problems.toExtendedReport())
    }
}
