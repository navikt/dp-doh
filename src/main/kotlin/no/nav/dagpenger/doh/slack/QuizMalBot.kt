package no.nav.dagpenger.doh.slack

import com.slack.api.methods.MethodsClient
import com.slack.api.methods.kotlin_extension.request.chat.blocks

internal class QuizMalBot(slackClient: MethodsClient, slackChannelId: String) : SlackBot(slackClient, slackChannelId, slackTrådRepository = null) {
    internal fun postNyMal(
        navn: String,
        versjonId: Int,
    ) {
        chatPostMessage {
            it.iconEmoji(":robot_face:")
            it.blocks {
                section { plainText("Ny mal med prossesnavn $navn og versjonid $versjonId") }

                actions {
                    button {
                        text(":ledger: Se commit logg fra dp-quiz")
                        url("https://github.com/navikt/dp-quiz/commits/main")
                    }
                }
            }
        }
    }
}
