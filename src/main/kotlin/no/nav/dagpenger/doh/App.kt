package no.nav.dagpenger.doh

import no.nav.dagpenger.doh.Configuration.slackAlertClient
import no.nav.dagpenger.doh.Configuration.slackBot
import no.nav.dagpenger.doh.Configuration.slackClient
import no.nav.dagpenger.doh.monitor.AppStateMonitor
import no.nav.dagpenger.doh.monitor.ProsessResultatMonitor
import no.nav.dagpenger.doh.monitor.UløstOppgaveMonitor
import no.nav.helse.rapids_rivers.RapidApplication
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    RapidApplication.create(Configuration.asMap()).apply {
        AppStateMonitor(this, slackAlertClient)
        UløstOppgaveMonitor(this, slackClient)
        ProsessResultatMonitor(this, slackBot)
    }.start()
}
