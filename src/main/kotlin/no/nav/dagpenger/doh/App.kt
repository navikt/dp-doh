package no.nav.dagpenger.doh

import no.nav.helse.rapids_rivers.RapidApplication
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    val env = System.getenv()

    val slackClient = env["SLACK_ACCESS_TOKEN"]?.let {
        SlackClient(
            accessToken = it,
            channel = env.getValue("SLACK_CHANNEL_ID")
        )
    }

    RapidApplication.create(env).apply {
        AppStateMonitor(this, slackClient)
        UløstOppgaveMonitor(this, slackClient)
        ProsessResultatMonitor(this, slackClient)
        ManuellBehandlingMonitor(this, slackClient)
    }.start()
}
