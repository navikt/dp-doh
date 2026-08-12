package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.doh.slack.SlackClient
import org.junit.jupiter.api.Assertions.assertTrue
import kotlin.test.Test
import kotlin.test.assertContains

class UtbetalingHengerMonitorTest {
    private val testRapid = TestRapid()
    private val slackClient =
        mockk<SlackClient>().also {
            every { it.postMessage(any(), any(), any(), any()) } returns null
        }

    @Test
    fun `skal varsle om utbetaling som henger`() {
        UtbetalingHengerMonitor(testRapid, slackClient)
        testRapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "utbetaling_henger",
                    mapOf(
                        "behandlingId" to "123e4567-e89b-12d3-a456-426614174000",
                        "sakId" to "123e4567-e89b-12d3-a456-426614174001",
                        "ventetidMinutter" to 65L,
                        "antallVarslerSåLangt" to 1,
                    ),
                ).toJson(),
        )

        val melding = slot<String>()
        verify(exactly = 1) {
            slackClient.postMessage(capture(melding), any(), any(), any())
        }
        assertTrue(melding.isCaptured)
        assertContains(melding.captured, "har ventet på svar fra Oppdrag i 65 minutter")
        assertContains(melding.captured, "varslet 1. gang")
    }
}
