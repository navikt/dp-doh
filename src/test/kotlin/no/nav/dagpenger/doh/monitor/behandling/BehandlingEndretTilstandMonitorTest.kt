package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.tidBruktITilstand
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class BehandlingEndretTilstandMonitorTest {
    private val rapid = TestRapid()
    private val monitor = BehandlingEndretTilstandMonitor(rapid)

    @Test
    fun `måler tiden det tar å endre tilstand`() {
        rapid.sendTestMessage(tilstandEndretEvent)

        val målinger = tidBruktITilstand.collect()

        assertEquals(1, målinger.dataPoints.sumOf { it.count }, "Antall overganger observert")

        with(tidBruktITilstand.collect().dataPoints.single()) {
            assertEquals(4.0, sum, "Sekunder brukt på å endre tilstand")
            assertEquals("UnderOpprettelse", labels.get("forrigeTilstand"))
            assertEquals("UnderBehandling", labels.get("gjeldendeTilstand"))
        }
    }

    @Language("JSON")
    private val tilstandEndretEvent =
        """{
        |  "@event_name": "behandling_endret_tilstand",
        |  "ident": "11109233444",
        |  "behandlingId": "019078d2-a873-74b4-95f2-0a66dbe59afd",
        |  "forrigeTilstand": "UnderOpprettelse",
        |  "gjeldendeTilstand": "UnderBehandling",
        |  "forventetFerdig": "2024-07-03T16:39:50.006487",
        |  "tidBrukt": "PT4.661S",
        |  "@id": "013fbdec-07cb-438c-acb1-573f92c9919f",
        |  "@opprettet": "2024-07-03T15:39:50.007145",
        |  "system_read_count": 0,
        |  "system_participating_services": [
        |    {
        |      "id": "013fbdec-07cb-438c-acb1-573f92c9919f",
        |      "time": "2024-07-03T15:39:50.007145"
        |    }
        |  ]
        |}
        """.trimMargin()
}
