package no.nav.dagpenger.doh

import no.nav.dagpenger.doh.Configuration.arenaSinkBot
import no.nav.dagpenger.doh.Configuration.publiserArenaVedtak
import no.nav.dagpenger.doh.Configuration.quizMalBot
import no.nav.dagpenger.doh.Configuration.slackAlertClient
import no.nav.dagpenger.doh.Configuration.stsbSlackAlertChannel
import no.nav.dagpenger.doh.Configuration.vedtakBot
import no.nav.dagpenger.doh.monitor.AppStateMonitor
import no.nav.dagpenger.doh.monitor.BehovUtenLøsningMonitor
import no.nav.dagpenger.doh.monitor.MeldingerUtenEnvelopeMonitor
import no.nav.dagpenger.doh.monitor.OpprettJournalpostFeiletMonitor
import no.nav.dagpenger.doh.monitor.SaksbehandlingAlertMonitor
import no.nav.dagpenger.doh.monitor.behandling.ArenasinkVedtakFeiletMonitor
import no.nav.dagpenger.doh.monitor.behandling.ArenasinkVedtakOpprettetMonitor
import no.nav.dagpenger.doh.monitor.behandling.BehandlingEndretTilstandMonitor
import no.nav.dagpenger.doh.monitor.behandling.BehandlingPåminnelseMonitor
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStatusMonitor
import no.nav.dagpenger.doh.monitor.behandling.BeregnMeldekortMottak
import no.nav.dagpenger.doh.monitor.behandling.UtbetalingFeilUtbetalingsdagerMonitor
import no.nav.dagpenger.doh.monitor.behandling.UtbetalingStatusMonitor
import no.nav.dagpenger.doh.monitor.quiz.NyQuizMalMonitor
import no.nav.helse.rapids_rivers.RapidApplication
import java.time.Duration
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    RapidApplication
        .create(
            Configuration.asMap(),
        ).apply {
            AppStateMonitor(this, slackAlertClient, Duration.ofMinutes(5))
            NyQuizMalMonitor(this, quizMalBot)
            BehovUtenLøsningMonitor(this, slackAlertClient)
            BehandlingPåminnelseMonitor(this, slackAlertClient)

            BehandlingEndretTilstandMonitor(this)
            BehandlingStatusMonitor(this, vedtakBot)
            if (publiserArenaVedtak) {
                ArenasinkVedtakOpprettetMonitor(this, arenaSinkBot)
            }
            ArenasinkVedtakFeiletMonitor(this, arenaSinkBot)
            MeldingerUtenEnvelopeMonitor(this)
            SaksbehandlingAlertMonitor(this, Configuration.slackClient(stsbSlackAlertChannel))

            OpprettJournalpostFeiletMonitor(this, slackAlertClient)
            BeregnMeldekortMottak(this, vedtakBot)
            UtbetalingStatusMonitor(this, vedtakBot)
            UtbetalingFeilUtbetalingsdagerMonitor(this, vedtakBot)

            // TODO: Denne må bli litt mer presis
            //  KorrigertMeldekortMonitor(this, vedtakBot)
        }.start()
}
