package no.nav.dagpenger.doh.slack

import com.slack.api.RequestConfigurator
import com.slack.api.methods.MethodsClient
import com.slack.api.methods.request.chat.ChatPostMessageRequest.ChatPostMessageRequestBuilder
import com.slack.api.methods.response.chat.ChatPostMessageResponse
import io.mockk.confirmVerified
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class SlackBotTest {
    private val client = mockk<MethodsClient>()
    private val slackBot = SlackBot(client, "test")

    @BeforeEach
    fun setup() {
        every {
            client.chatPostMessage(allAny<RequestConfigurator<ChatPostMessageRequestBuilder>>())
        } answers {
            ChatPostMessageResponse().apply {
                isOk = true
            }
        }
    }

    @AfterEach
    fun tearDown() {
        verify {
            client.chatPostMessage(allAny<RequestConfigurator<ChatPostMessageRequestBuilder>>())
        }

        confirmVerified(client)
    }

    @Test
    fun `Vi kan poste meldinger om manuell behandling`() {
        slackBot.postManuellBehandling("123", LocalDateTime.now(), "årsak")
    }

    @Test
    fun `Vi kan poste meldinger om automatiske vedtak`() {
        slackBot.postResultat("123", LocalDateTime.now(), false)
    }

    @Test
    fun `Vi logger feil fra Slack`() {
        every {
            client.chatPostMessage(allAny<RequestConfigurator<ChatPostMessageRequestBuilder>>())
        } answers {
            ChatPostMessageResponse().apply {
                isOk = false
            }
        }
        slackBot.postResultat("123", LocalDateTime.now(), false)
        // Klarer ikke å asserte at det logges, så må bare sjekke outputten :(
    }
}
