package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.RampBot
import kotlin.test.Test

internal class BrukerHarMeldekortMedEndretMeldesyklusIArenaMonitorTest {
    private val rampBot = mockk<RampBot>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            BrukerHarMeldekortMedEndretMeldesyklusIArenaMonitor(it, rampBot)
        }

    @Test
    fun `varsler når bruker har meldekort med endret meldesyklus i Arena`() {
        testRapid.sendTestMessage(
            // language=JSON
            """
            {
              "@event_name": "bruker-har-meldekort-med-endret-meldesyklus-i-arena",
              "referanseId": "test-referanse-id",
              "personId": "test-person-id"
            }
            """.trimIndent(),
        )

        verify(exactly = 1) {
            rampBot.postBrukerHarMeldekortMedEndretMeldesyklusIArena(referanseId = "test-referanse-id", personId = "test-person-id")
        }
    }

    @Test
    fun `varsler ikke for andre event-typer`() {
        testRapid.sendTestMessage(
            // language=JSON
            """
            {
              "@event_name": "noe-annet",
              "referanseId": "test-referanse-id"
            }
            """.trimIndent(),
        )

        verify(exactly = 0) {
            rampBot.postBrukerHarMeldekortMedEndretMeldesyklusIArena(any(), any())
        }
    }
}
