package no.nav.dagpenger.doh.monitor.behandling

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingStårFastMonitor(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val behandlinger = Behandlinger()

    init {
        River(rapidsConnection)
            .apply {
                validate {
                    it.demandValue("@event_name", "behandling_endret_tilstand")
                    it.requireKey("gjeldendeTilstand", "forventetFerdig")
                    it.interestedIn("behandlingId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val behandlingId = packet["behandlingId"].asText().let { UUID.fromString(it) }
        val gjeldendeTilstand = packet["gjeldendeTilstand"].asText()
        val forventetFerdig = packet["forventetFerdig"].asLocalDateTime()

        behandlinger.registrer(behandlingId, gjeldendeTilstand, forventetFerdig)

        // Finn alle behandlinger som står fast
        logger.info { "Sjekker alle eksisterende behandlinger" }
        behandlinger.hengende { overvåket ->
            logger.warn { "Behandlingen ${overvåket.behandlingId} står fast i ${overvåket.gjeldendeTilstand}" }

            context.publish(BehandlingStårFast(overvåket.behandlingId).toJson())
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    private data class BehandlingStårFast(
        val behandlingId: UUID,
    ) {
        fun toJson() =
            JsonMessage
                .newMessage(
                    mapOf(
                        "@event_name" to "behandling_står_fast",
                        "behandlingId" to behandlingId.toString(),
                    ),
                ).toJson()
    }
}

private class Behandlinger private constructor(
    val behandlinger: MutableList<Behandling>,
) {
    constructor() : this(mutableListOf())

    fun registrer(
        behandlingId: UUID,
        gjeldendeTilstand: String,
        forventetFerdig: LocalDateTime,
    ) {
        // Kan stå i denne tilstanden uendelig
        if (forventetFerdig == LocalDateTime.MAX) {
            logger.info { "Behandlingen $behandlingId videre til $gjeldendeTilstand" }
            behandlinger.removeIf { it.behandlingId == behandlingId }
            return
        }

        // Allerede utdatert ved innlesning
        if (forventetFerdig.isBefore(LocalDateTime.now())) {
            // Her bør vi kanskje ikke publisere, for det kan hende det er vi som henger etter
            logger.warn { "Behandlingen $behandlingId står allerede fast i $gjeldendeTilstand" }
            behandlinger.removeIf { it.behandlingId == behandlingId }
            return
        }

        // Ikke sett før, start overvåkning
        if (behandlinger.find { it.behandlingId == behandlingId } == null) {
            logger.info { "Aldri sett behandling $behandlingId, begynner å følge med nå" }
        }

        // Oppdater neste gang vi forventer denne å være ferdig
        behandlinger.add(Behandling(behandlingId, gjeldendeTilstand, forventetFerdig))
    }

    fun hengende(
        now: LocalDateTime = LocalDateTime.now(),
        block: (Behandling) -> Unit,
    ) = behandlinger
        .filter { it.forventetFerdig.isBefore(now) }
        .forEach {
            block(it)
            behandlinger.remove(it)
        }.also {
            logger.info {
                "Overvåker ${behandlinger.size} behandlinger. Eldste behandling forventes ferdig innen ${
                    behandlinger.minOf { it.forventetFerdig }
                }"
            }
        }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}

private data class Behandling(
    val behandlingId: UUID,
    val gjeldendeTilstand: String,
    val forventetFerdig: LocalDateTime,
)
