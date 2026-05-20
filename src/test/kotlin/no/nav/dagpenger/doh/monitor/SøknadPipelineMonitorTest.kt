package no.nav.dagpenger.doh.monitor

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.SlackClient
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SøknadPipelineMonitorTest {
    private val slackClient = mockk<SlackClient>(relaxed = true)
    private val rapid =
        TestRapid().also {
            SøknadPipelineMonitor(it, slackClient)
        }

    @Test
    fun `varsler på Slack ved søknad_aldri_nådd_behandling`() {
        rapid.sendTestMessage(søknadAldriBleBehandlet(antallVarsler = 1))
        verify(exactly = 1) { slackClient.postMessage(any(), emoji = ":warning:") }
    }

    @Test
    fun `bruker brann-emoji ved mange varsler`() {
        rapid.sendTestMessage(søknadAldriBleBehandlet(antallVarsler = 4))
        verify(exactly = 1) { slackClient.postMessage(any(), emoji = ":fire:") }
    }

    // language=JSON
    private fun søknadAldriBleBehandlet(antallVarsler: Int): String =
        """
        {
            "@event_name": "søknad_aldri_nådd_behandling",
            "søknadId": "${UUID.randomUUID()}",
            "ident": "12345678901",
            "mottatt": "${LocalDateTime.now().minusHours(1)}",
            "antallVarsler": $antallVarsler
        }
        """.trimIndent()
}
