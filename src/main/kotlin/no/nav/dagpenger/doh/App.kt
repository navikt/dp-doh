package no.nav.dagpenger.doh

import no.nav.dagpenger.doh.Configuration.arenaSinkBot
import no.nav.dagpenger.doh.Configuration.publiserArenaVedtak
import no.nav.dagpenger.doh.Configuration.quizMalBot
import no.nav.dagpenger.doh.Configuration.quizResultatBot
import no.nav.dagpenger.doh.Configuration.slackAlertClient
import no.nav.dagpenger.doh.Configuration.vedtakBot
import no.nav.dagpenger.doh.monitor.AppStateMonitor
import no.nav.dagpenger.doh.monitor.ArenasinkVedtakFeiletMonitor
import no.nav.dagpenger.doh.monitor.ArenasinkVedtakOpprettetMonitor
import no.nav.dagpenger.doh.monitor.BehandlingStatusMonitor
import no.nav.dagpenger.doh.monitor.BehovUtenLøsningMonitor
import no.nav.dagpenger.doh.monitor.ManuellBehandlingMonitor
import no.nav.dagpenger.doh.monitor.ManuellQuizBehandlingMonitor
import no.nav.dagpenger.doh.monitor.MeldingerUtenEnvelopeMonitor
import no.nav.dagpenger.doh.monitor.NyQuizMalMonitor
import no.nav.dagpenger.doh.monitor.ProsessResultatMonitor
import no.nav.dagpenger.doh.monitor.VedtakfattetMonitor
import no.nav.helse.rapids_rivers.RapidApplication
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    RapidApplication.create(Configuration.asMap()).apply {
        AppStateMonitor(this, slackAlertClient)
        ProsessResultatMonitor(this, quizResultatBot)
        NyQuizMalMonitor(this, quizMalBot)
        BehovUtenLøsningMonitor(this, slackAlertClient)
        VedtakfattetMonitor(this, vedtakBot)
        BehandlingStatusMonitor(this, vedtakBot)
        ManuellBehandlingMonitor(this, vedtakBot)
        if (publiserArenaVedtak) {
            ArenasinkVedtakOpprettetMonitor(this, arenaSinkBot)
        }

        ArenasinkVedtakFeiletMonitor(this, arenaSinkBot)

        /**
         * Enn så lenge går 98% til manuell så den lager mer støy enn den gir informasjon.
         * Skrur Slack posting av, også kan vi heller skru den på igjen i framtida om vi øker graden av automatiske
         * For å skru på Slack posting igjen, legg til slackBot variabelen i konstruktøren
         */
        ManuellQuizBehandlingMonitor(this)
        MeldingerUtenEnvelopeMonitor(this)
    }.start()
}
