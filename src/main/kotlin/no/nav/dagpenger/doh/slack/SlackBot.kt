package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import mu.KotlinLogging

internal abstract class SlackBot(
    private val slackClient: MethodsClient,
    private val slackChannelId: String,
    private val username: String = "dp-quiz",
    private val slackTrådRepository: SlackTrådRepository? = null,
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
        val threadTs = trådNøkkel?.let { slackTrådRepository?.hentTråd(it) }?.threadTs
        slackClient.chatPostMessage {
            it.channel(slackChannelId)
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
            trådNøkkel?.let { slackTrådRepository?.lagreTråd(SlackTråd(it, response.ts)) }
        }
    }
}
