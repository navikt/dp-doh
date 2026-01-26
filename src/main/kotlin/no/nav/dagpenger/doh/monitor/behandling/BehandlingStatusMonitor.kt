package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.isMissingOrNull
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingAvbruttCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingVedtakCounter
import no.nav.dagpenger.doh.slack.VedtakBot

internal class BehandlingStatusMonitor(
    rapidsConnection: RapidsConnection,
    private val vedtakBot: VedtakBot?,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition {
                    it.requireAny(
                        "@event_name",
                        listOf("behandling_opprettet", "forslag_til_behandlingsresultat", "behandlingsresultat", "behandling_avbrutt"),
                    )
                }
                validate {
                    it.requireKey("behandlingId", "behandletHendelse", "@opprettet")
                    it.interestedIn("førteTil", "automatisk", "årsak", "opprettet")
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
        val behandlingId = packet["behandlingId"].asText()

        if (behandlingId == "019bf98f-a7ea-7464-b00b-f55895d75c8b") {
            // Midlertidig filter for å unngå støy fra en spesifikk behandling som ikke er laget fra dp-behandling i dev?
            logger.info { "Ignorerer behandling med ID $behandlingId" }
            return
        }

        val behandletHendelse =
            BehandletHendelse(
                id = packet["behandletHendelse"]["id"].asText(),
                type = packet["behandletHendelse"]["type"].asText(),
            )

        withLoggingContext(
            "behandlingId" to behandlingId,
            behandletHendelse.type to behandletHendelse.id,
        ) {
            val eventName = packet["@event_name"].asText()
            logger.info { "Vi har behandling med $eventName" + "(slackbot er konfiguert? ${vedtakBot != null})" }
            behandlingStatusCounter.labelValues(eventName.lowercase()).inc()

            val status =
                when (eventName) {
                    "behandling_avbrutt" -> Status.BEHANDLING_AVBRUTT

                    "forslag_til_behandlingsresultat" -> return

                    // Vi vil bare telle, ikke poste Slack-melding
                    "behandlingsresultat" -> Status.VEDTAK_FATTET

                    "behandling_opprettet" -> return

                    // Vi vil bare telle, ikke poste Slack-melding
                    else -> return
                }

            val førteTil = packet["førteTil"].takeUnless { it.isMissingOrNull() }?.asText()
            val automatisk = packet["automatisk"].takeIf { automatisk -> automatisk.isBoolean }?.asBoolean()
            val årsak = packet["årsak"].takeUnless { årsak -> årsak.isMissingNode }?.asText()

            if (status == Status.BEHANDLING_AVBRUTT) {
                behandlingAvbruttCounter.labelValues(årsak ?: "Ukjent").inc()
                return
            }

            vedtakBot?.postBehandlingStatus(
                status = status,
                behandlingId = behandlingId,
                behandletHendelse = behandletHendelse,
                opprettet = packet["opprettet"].asLocalDateTime(),
                årsak = årsak,
                førteTil = førteTil,
                automatisk = automatisk,
            )

            if (førteTil != null && automatisk != null) {
                val automatisering = if (automatisk) "Automatisk" else "Manuell"
                behandlingVedtakCounter.labelValues(førteTil, automatisering).inc()
            }
        }
    }

    data class BehandletHendelse(
        val id: String,
        val type: String,
    )

    internal enum class Status {
        BEHANDLING_AVBRUTT,
        FORSLAG_TIL_VEDTAK,
        VEDTAK_FATTET,
    }
}
