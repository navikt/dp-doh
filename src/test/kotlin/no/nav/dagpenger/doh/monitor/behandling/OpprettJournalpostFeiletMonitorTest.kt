package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.OpprettJournalpostFeiletMonitor
import no.nav.dagpenger.doh.slack.SlackClient
import java.util.UUID
import kotlin.test.Test

class OpprettJournalpostFeiletMonitorTest {
    private val slackClient = mockk<SlackClient>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            OpprettJournalpostFeiletMonitor(it, slackClient)
        }

    @Test
    fun `melder fra når en journalpost feiler`() {
        testRapid.sendTestMessage(journalpostFeilet)

        verify(exactly = 1) {
            slackClient.postMessage(any())
        }
    }

    val journalpostFeilet =
        JsonMessage
            .newMessage(
                "opprett_journalpost_feilet",
                mapOf(
                    "behovId" to UUID.randomUUID().toString(),
                    "søknadId" to UUID.randomUUID().toString(),
                    "type" to "type",
                ),
            ).toJson()
}
