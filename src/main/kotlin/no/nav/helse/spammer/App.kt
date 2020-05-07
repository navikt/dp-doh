package no.nav.helse.spammer

import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection
import kotlin.time.ExperimentalTime

@ExperimentalTime
fun main() {
    val env = System.getenv()
    val dataSourceBuilder = env["DATABASE_HOST"]?.let { DataSourceBuilder(env) }

    val slackClient = env["SLACK_ACCESS_TOKEN"]?.let {
        SlackClient(
            accessToken = it,
            channel = env.getValue("SLACK_CHANNEL_ID")
        )
    }

    val slackThreadDao = dataSourceBuilder?.let { SlackThreadDao(dataSourceBuilder.getDataSource()) }

    RapidApplication.create(env).apply {
        TidITilstandMonitor(this, slackClient, slackThreadDao)
        UtbetalingMonitor(this, slackClient, slackThreadDao)
        BehovUtenLøsningMonitor(this, slackClient)
        AvstemmingMonitor(this, slackClient)
        AppStateMonitor(this, slackClient)
    }.apply {
        register(object : RapidsConnection.StatusListener {
            override fun onStartup(rapidsConnection: RapidsConnection) {
                dataSourceBuilder?.migrate()
            }
        })
    }.start()
}
