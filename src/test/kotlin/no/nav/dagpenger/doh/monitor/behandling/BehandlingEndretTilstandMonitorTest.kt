package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.prometheus.client.CollectorRegistry
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class BehandlingEndretTilstandMonitorTest {
    private val rapid = TestRapid()
    private val monitor = BehandlingEndretTilstandMonitor(rapid)

    @Test
    fun `måler tiden det tar å endre tilstand`() {
        rapid.sendTestMessage(tilstandEndretEvent)

        val måltVerdi =
            CollectorRegistry.defaultRegistry.getSampleValue(
                "dp_behandling_tid_i_tilstand_sekund_sum",
                arrayOf("forrigeTilstand", "gjeldendeTilstand"),
                arrayOf("UnderOpprettelse", "UnderBehandling"),
            )

        assertEquals(2.0, måltVerdi)
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
        |  "tidBrukt": "PT2.661S",
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
