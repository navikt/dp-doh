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
import no.nav.dagpenger.doh.monitor.behandling.Behandling.Companion.eldste
import java.time.LocalDateTime
import java.util.UUID

internal class BehandlingStårFastMonitor(
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val behandlinger = Behandlinger()

    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireValue("@event_name", "behandling_endret_tilstand")
                }
                validate {
                    it.requireKey("gjeldendeTilstand", "forventetFerdig", "ident")
                    it.interestedIn("behandlingId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val behandlingId = packet["behandlingId"].asText().let { UUID.fromString(it) }
        val ident = packet["ident"].asText()
        val gjeldendeTilstand = packet["gjeldendeTilstand"].asText()
        val forventetFerdig = packet["forventetFerdig"].asLocalDateTime()

        withLoggingContext(
            "behandlingId" to behandlingId.toString(),
        ) {
            behandlinger.registrer(behandlingId, ident, gjeldendeTilstand, forventetFerdig)
        }

        // Finn alle behandlinger som står fast
        logger.info { "Sjekker alle eksisterende behandlinger" }
        behandlinger.hengende { hengendeBehandling ->
            logger.warn { "Behandlingen ${hengendeBehandling.behandlingId} står fast i ${hengendeBehandling.gjeldendeTilstand}" }

            context.publish(hengendeBehandling.ident, BehandlingStårFast(hengendeBehandling).toJson())
        }
    }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }

    private data class BehandlingStårFast(
        val behandling: Behandling,
    ) {
        fun toJson() =
            JsonMessage
                .newMessage(
                    mapOf(
                        "@event_name" to "behandling_står_fast",
                        "ident" to behandling.ident,
                        "behandlingId" to behandling.behandlingId.toString(),
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
        ident: String,
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

        // Fjern forrige tilstand fra overvåkningen
        logger.info { "Overvåker $behandlingId i $gjeldendeTilstand med frist til $forventetFerdig" }
        behandlinger.removeIf { it.behandlingId == behandlingId }

        // Oppdater neste gang vi forventer denne å være ferdig
        behandlinger.add(Behandling(behandlingId, ident, gjeldendeTilstand, forventetFerdig))
    }

    fun hengende(
        now: LocalDateTime = LocalDateTime.now(),
        block: (Behandling) -> Unit,
    ) = behandlinger
        .filter { it.erUtdatert(now) }
        .forEach {
            block(it)
            it.utsett()
            logger.info { "Utsetter forventetFerdig for behandling ${it.behandlingId}" }
        }.also {
            logger.info {
                "Overvåker ${behandlinger.size} behandlinger." +
                    if (behandlinger.isNotEmpty()) {
                        " Eldste behandling forventes ferdig innen ${behandlinger.eldste()}"
                    } else {
                        ""
                    }
            }
        }

    private companion object {
        private val logger = KotlinLogging.logger { }
    }
}

private class Behandling(
    val behandlingId: UUID,
    val ident: String,
    val gjeldendeTilstand: String,
    private var forventetFerdig: LocalDateTime,
) {
    companion object {
        fun List<Behandling>.eldste() = minOf { it.forventetFerdig }
    }

    fun erUtdatert(now: LocalDateTime) = forventetFerdig.isBefore(now)

    fun utsett() {
        forventetFerdig = forventetFerdig.plusMinutes(35)
    }
}
