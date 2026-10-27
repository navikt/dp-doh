package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.string.shouldEndWith
import io.kotest.matchers.string.shouldNotContain
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.doh.slack.SlackClient
import kotlin.test.Test

class SaksbehandlingAlertMonitorTest {
    private val slackClient = mockk<SlackClient>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            SaksbehandlingAlertMonitor(it, slackClient)
        }

    @Test
    fun `tagger @dp-kvintett ved identendring for sak`() {
        testRapid.sendTestMessage(alert("IDENTENDRING_FOR_SAK"))

        postetTekst() shouldEndWith "\n<!subteam^S0BP1MHDRT7>"
    }

    @Test
    fun `tagger ingen for andre alertTyper`() {
        testRapid.sendTestMessage(alert("ANNEN_ALERT"))

        postetTekst() shouldNotContain "<"
    }

    private fun postetTekst(): String {
        val tekst = slot<String>()
        verify(exactly = 1) { slackClient.postMessage(capture(tekst), any()) }
        return tekst.captured
    }

    private fun alert(alertType: String) =
        JsonMessage
            .newMessage(
                "saksbehandling_alert",
                mapOf("alertType" to alertType, "feilMelding" to "En feilmelding"),
            ).toJson()
}
