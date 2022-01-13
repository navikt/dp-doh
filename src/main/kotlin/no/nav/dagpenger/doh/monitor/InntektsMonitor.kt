package no.nav.dagpenger.doh.monitor

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.ktor.utils.io.core.use
import io.prometheus.client.Histogram
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.net.URL

// Logger fordeling av inntekt på vedtakene som går gjennom quiz.
internal class InntektsMonitor(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val objectMapper = ObjectMapper()
        private val inntektsteller =
            Histogram.build("dp_inntekt", "Inntekt for automatisering").labelNames("inntektsgruppe", "type")
                .linearBuckets(0.0, 50000.0, 12).register()
        private val gjeldendeGrunnbeløp by lazy {
            URL("https://g.nav.no/api/v1/grunnbeloep").openStream().bufferedReader().use {
                objectMapper.readTree(it).run {
                    this["grunnbeloep"].asDouble()
                }
            }
        }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "prosess_resultat")
                it.requireKey("søknad_uuid", "resultat")
                it.require("@opprettet", JsonNode::asLocalDateTime)
                it.requireKey("fakta")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        packet["fakta"].filter { it["type"].asText() == "inntekt" }.forEach {
            val inntekt = it["svar"].asDouble()
            inntektsteller.labels(
                inntektsgruppe(inntekt),
                it["navn"].asText()
            ).observe(inntekt)
        }
    }

    private fun inntektsgruppe(inntekt: Number): String {
        return when (inntekt.toDouble()) {
            in 0.G..3.G -> "0-3G"
            in 3.G..6.G -> "3-6G"
            else -> ">6G"
        }
    }

    private val Int.G get() = this * gjeldendeGrunnbeløp
}
