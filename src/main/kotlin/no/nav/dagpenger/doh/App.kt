package no.nav.dagpenger.doh

import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.doh.Configuration.arenaSinkBot
import no.nav.dagpenger.doh.Configuration.publiserArenaVedtak
import no.nav.dagpenger.doh.Configuration.quizMalBot
import no.nav.dagpenger.doh.Configuration.slackAlertClient
import no.nav.dagpenger.doh.Configuration.vedtakBot
import no.nav.dagpenger.doh.monitor.AppStateMonitor
import no.nav.dagpenger.doh.monitor.BehovUtenLøsningMonitor
import no.nav.dagpenger.doh.monitor.MeldingerUtenEnvelopeMonitor
import no.nav.dagpenger.doh.monitor.behandling.ArenasinkVedtakFeiletMonitor
import no.nav.dagpenger.doh.monitor.behandling.ArenasinkVedtakOpprettetMonitor
import no.nav.dagpenger.doh.monitor.behandling.BehandlingEndretTilstandMonitor
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStatusMonitor
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStårFastMonitor
import no.nav.dagpenger.doh.monitor.quiz.NyQuizMalMonitor
import no.nav.helse.rapids_rivers.RapidApplication
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    RapidApplication
        .create(
            Configuration.asMap(),
            meterRegistry = PrometheusMeterRegistry(PrometheusConfig.DEFAULT, PrometheusRegistry.defaultRegistry, Clock.SYSTEM),
        ).apply {
            AppStateMonitor(this, slackAlertClient, Duration.ofMinutes(5))
            NyQuizMalMonitor(this, quizMalBot)
            BehovUtenLøsningMonitor(this, slackAlertClient)

            BehandlingStårFastMonitor(this)
            BehandlingEndretTilstandMonitor(this)
            BehandlingStatusMonitor(this, vedtakBot)
            if (publiserArenaVedtak) {
                ArenasinkVedtakOpprettetMonitor(this, arenaSinkBot)
            }
            ArenasinkVedtakFeiletMonitor(this, arenaSinkBot)
            MeldingerUtenEnvelopeMonitor(this)
        }.start()
}
