package no.nav.dagpenger.doh.slack

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

class SlackClientTest {
    @Test
    fun `skal returnere ts ved vellykket slack-post`() {
        val mockHttpClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        every { mockResponse.statusCode() } returns 200
        every { mockResponse.body() } returns """{"ok": true, "ts": "12345.6789"}"""

        every { mockHttpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        val client = SlackClient("fake-token", "#test", mockHttpClient)
        val ts = client.postMessage("Hei Slack!")

        assertEquals("12345.6789", ts)
    }

    @Test
    fun `skal returnere null når slack svarer med feil`() {
        val mockHttpClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        every { mockResponse.statusCode() } returns 200
        every { mockResponse.body() } returns """{"ok": false, "error": "channel_not_found"}"""

        every { mockHttpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        val client = SlackClient("fake-token", "#test", mockHttpClient)
        val ts = client.postMessage("Hei Slack!")

        assertNull(ts)
    }

    @Test
    fun `skal returnere null når slack svarer feilkode`() {
        val mockHttpClient = mockk<HttpClient>()
        val mockResponse = mockk<HttpResponse<String>>()

        every { mockResponse.statusCode() } returns 404
        every { mockResponse.body() } returns """{"ok": false, "error": "channel_not_found"}"""

        every { mockHttpClient.send(any<HttpRequest>(), any<HttpResponse.BodyHandler<String>>()) } returns mockResponse

        val client = SlackClient("fake-token", "#test", mockHttpClient)
        val ts = client.postMessage("Hei Slack!")

        assertNull(ts)
    }
}
