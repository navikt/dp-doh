package no.nav.dagpenger.doh.monitor.behandling

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.ArenasinkBot
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ArenasinkVedtakFeiletMonitorTest {
    private val rapid = TestRapid()
    private val arenasinkBot = mockk<ArenasinkBot>(relaxed = true)

    init {
        ArenasinkVedtakFeiletMonitor(rapid, arenasinkBot)
    }

    @Test
    fun `publiserer vedtak opprettet i Arena`() {
        rapid.sendTestMessage(opprettetJson)

        verify(exactly = 1) {
            arenasinkBot.postFeilet(
                søknadId = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
                kildeId = any(),
                kildeSystem = "dp-behandling",
                opprettet = any(),
            )
        }
    }

    @Language("JSON")
    private val opprettetJson =
        """
        {
          "@event_name": "arenasink_vedtak_feilet",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "kilde": {
            "id": "018ec828-d704-775b-b300-15ef4ac047d3",
            "system": "dp-behandling"
          },
          "@id": "79d3a8a8-72ff-4573-9c85-3c693a2e4320",
          "@opprettet": "2024-04-10T21:13:39.140035",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "79d3a8a8-72ff-4573-9c85-3c693a2e4320",
              "time": "2024-04-10T21:13:39.140035"
            }
          ]
        }
        """.trimIndent()
}
