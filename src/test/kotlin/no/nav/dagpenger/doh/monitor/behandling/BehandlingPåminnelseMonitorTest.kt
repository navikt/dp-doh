package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.SlackClient
import java.util.UUID
import kotlin.test.Test

class BehandlingPåminnelseMonitorTest {
    private val slackClient = mockk<SlackClient>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            BehandlingPåminnelseMonitor(it, slackClient)
        }

    @Test
    fun `varsle om påminnelser`() {
        testRapid.sendTestMessage(påminnelse(utsatteGanger = 1))
        verify(exactly = 0) { slackClient.postMessage(any(), any()) }
        testRapid.sendTestMessage(påminnelse(utsatteGanger = 11))
        verify(exactly = 1) { slackClient.postMessage(any(), any()) }
    }

    // language=JSON
    private fun påminnelse(utsatteGanger: Int): String =
        """
        {
            "@event_name": "behandling_står_fast",
            "behandlingId": "${UUID.randomUUID()}",
            "antallGangerUtsatt": $utsatteGanger
         
        }
        """.trimIndent()
}
