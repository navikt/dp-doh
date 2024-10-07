package no.nav.dagpenger.doh

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import com.slack.api.Slack
import com.slack.api.methods.MethodsClient
import no.nav.dagpenger.doh.slack.ArenasinkBot
import no.nav.dagpenger.doh.slack.InMemorySlackTrådRepository
import no.nav.dagpenger.doh.slack.QuizMalBot
import no.nav.dagpenger.doh.slack.SlackClient
import no.nav.dagpenger.doh.slack.VedtakBot

internal object Configuration {
    val quizResultatSlackChannelId by lazy {
        properties[Key("dp.slacker.channel.id", stringType)]
    }

    val quizMalSlackChannelId by lazy {
        properties[Key("dp.slacker.mal.channel.id", stringType)]
    }

    val vedtakBotSlackChannelId: String? by lazy {
        properties.getOrNull(Key("DP_SLACKER_VEDTAK_CHANNEL_ID", stringType))
    }

    val publiserArenaVedtak by lazy {
        properties[Key("DP_ARENA_SINK_OPPRETTET_MELDING", stringType)].toBoolean()
    }

    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "KAFKA_CONSUMER_GROUP_ID" to "dp-doh-v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "HTTP_PORT" to "8080",
                "DP_ARENA_SINK_OPPRETTET_MELDING" to "true",
            ),
        )
    private val properties = systemProperties() overriding EnvironmentVariables() overriding defaultProperties
    val slackAlertClient: SlackClient? by lazy {
        properties.getOrNull(Key("SLACK_ACCESS_TOKEN", stringType))?.let {
            SlackClient(
                accessToken = it,
                channel = properties[Key("DP_SLACKER_ALERT_CHANNEL_ID", stringType)],
            )
        }
    }

    val slackBotClient: MethodsClient? by lazy {
        properties.getOrNull(Key("SLACK_ACCESS_TOKEN", stringType))?.let { token -> Slack.getInstance().methods(token) }
    }

    private val slackTrådRepository by lazy { InMemorySlackTrådRepository() }
    val vedtakBot: VedtakBot? by lazy {
        slackBotClient?.let { slackBotClient -> vedtakBotSlackChannelId?.let { VedtakBot(slackBotClient, it, slackTrådRepository) } }
    }

    val arenaSinkBot by lazy {
        slackBotClient?.let { slackBotClient -> vedtakBotSlackChannelId?.let { ArenasinkBot(slackBotClient, it, slackTrådRepository) } }
    }

    val quizMalBot: QuizMalBot? by lazy {
        slackBotClient?.let { QuizMalBot(it, quizMalSlackChannelId) }
    }

    fun asMap(): Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }
}
