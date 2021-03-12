package no.nav.dagpenger.doh

import no.nav.dagpenger.doh.Configuration.slackClient
import no.nav.helse.rapids_rivers.RapidApplication
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    RapidApplication.create(Configuration.asMap()).apply {
        AppStateMonitor(this, slackClient)
        UløstOppgaveMonitor(this, slackClient)
        ProsessResultatMonitor(this, slackClient)
        ManuellBehandlingMonitor(this, slackClient)
    }.start()
}
