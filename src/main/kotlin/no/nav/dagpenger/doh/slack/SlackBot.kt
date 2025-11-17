package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.SlackApiException
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.InetAddress
import javax.net.ssl.SSLHandshakeException

internal abstract class SlackBot(
    private val slackClient: MethodsClient,
    private val slackChannelId: String,
    private val username: String = "dp-quiz",
    private val slackTrådRepository: SlackTrådRepository?,
) {
    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.Slack")
    }

    protected fun chatPostMessage(
        replyBroadCast: Boolean = false,
        trådNøkkel: String? = null,
        block: (it: ChatPostMessageRequestBuilder) -> ChatPostMessageRequestBuilder,
    ) {
        val threadTs =
            trådNøkkel
                ?.let { nøkkel ->
                    slackTrådRepository?.hentTråd(nøkkel).also {
                        log.info { "Hentet tråd for $nøkkel med $it" }
                    }
                }?.threadTs
        try {
            slackClient
                .chatPostMessage {
                    it
                        .channel(slackChannelId)
                        .iconEmoji(":robot_face:")
                        .username(username)
                        .replyBroadcast(replyBroadCast)
                    threadTs?.let { ts -> it.threadTs(ts) }
                    block(it)
                }.let { response ->
                    if (!response.isOk) {
                        log.error { "Kunne ikke poste på Slack fordi ${response.errors}" }
                        log.error { response }
                    }
                    val threadTs = response.ts
                    trådNøkkel?.let {
                        log.info { "Lagrer tråd for $it med $threadTs" }
                        slackTrådRepository?.lagreTråd(
                            SlackTråd(it, response.ts),
                        )
                    }
                    log.info { "Publiserte melding på Slack med threadTs=$threadTs" }
                }
        } catch (e: SlackApiException) {
            if (e.response.code == 429) {
                log.error { "Rate limit exceeded" }
            } else {
                log.error(e) { "Slack API feilet" }
                throw e
            }
        } catch (e: SSLHandshakeException) {
            log.error(e) {
                val ips = InetAddress.getAllByName("slack.com")
                val ip = ips.joinToString { it.hostAddress }
                "SSL handshake feilet. Slack.com resolvet til: $ip"
            }
        }
    }
}
