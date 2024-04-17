package no.nav.dagpenger.doh.monitor

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.behandling.ArenasinkVedtakOpprettetMonitor
import no.nav.dagpenger.doh.slack.ArenasinkBot
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class ArenasinkVedtakOpprettetMonitorTest {
    private val rapid = TestRapid()
    private val arenasinkBot = mockk<ArenasinkBot>(relaxed = true)

    init {
        ArenasinkVedtakOpprettetMonitor(rapid, arenasinkBot)
    }

    @Test
    fun `publiserer vedtak opprettet i Arena`() {
        rapid.sendTestMessage(opprettetJson)

        verify(exactly = 1) {
            arenasinkBot.postVedtak(
                søknadId = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
                sakId = 123,
                vedtakId = any(),
                status = any(),
                rettighet = "PERM",
                utfall = false,
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
          "@event_name": "arenasink_vedtak_opprettet",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "sakId": 123,
          "vedtakId": 0,
          "vedtakstatus": "",
          "rettighet": "PERM",
          "utfall": false,
          "kilde": {
            "id": "018ec828-d704-775b-b300-15ef4ac047d3",
            "system": "dp-behandling"
          },
          "@id": "5c7aaecf-01e8-4981-920a-8021db97b003",
          "@opprettet": "2024-04-10T19:05:22.765321",
          "system_read_count": 0,
          "system_participating_services": [
            {
              "id": "5c7aaecf-01e8-4981-920a-8021db97b003",
              "time": "2024-04-10T19:05:22.765321"
            }
          ]
        }
        """.trimIndent()
}
