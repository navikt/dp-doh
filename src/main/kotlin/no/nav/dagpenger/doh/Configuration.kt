package no.nav.dagpenger.doh

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

internal object Configuration {
    private val defaultProperties = ConfigurationMap(
        mapOf(
            "KAFKA_CONSUMER_GROUP_ID" to "dp-doh-v1",
            "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
            "KAFKA_RESET_POLICY" to "latest",
            "HTTP_PORT" to "8080",
        )
    )

    private val properties = systemProperties() overriding EnvironmentVariables() overriding defaultProperties

    val slackAlertClient: SlackClient? by lazy {
        properties.getOrNull(Key("SLACK_ACCESS_TOKEN", stringType))?.let {
            SlackClient(
                accessToken = it,
                channel = properties[Key("DP_SLACKER_ALERT_CHANNEL_ID", stringType)]
            )
        }
    }

    val slackClient: SlackClient? by lazy {
        properties.getOrNull(Key("SLACK_ACCESS_TOKEN", stringType))?.let {
            SlackClient(
                accessToken = it,
                channel = System.getenv()["DP_SLACKER_CHANNEL_ID"]!!
            )
        }
    }

    fun asMap(): Map<String, String> = properties.list().reversed().fold(emptyMap()) { map, pair ->
        map + pair.second
    }
}
