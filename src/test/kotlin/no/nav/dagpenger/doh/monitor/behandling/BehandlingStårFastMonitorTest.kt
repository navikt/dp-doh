package no.nav.dagpenger.doh.monitor.behandling

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class BehandlingStårFastMonitorTest {
    private val rapid = TestRapid()
    private val monitor = BehandlingStårFastMonitor(rapid)

    @Test
    fun `behandlinger som allerede står fast fanges opp`() {
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().minusDays(1)))
    }

    @Test
    fun `sjekker om meldingen har satt fast, men bare ett varsel`() {
        val behandlingId = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusSeconds(1), behandlingId))
        runBlocking { delay(1000) }
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusHours(1)))
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusHours(1), behandlingId))

        assertEquals(1, rapid.inspektør.size)
    }

    @Test
    fun `sjekker flere meldinger har satt fast`() {
        val behandlingId1 = UUID.randomUUID()
        val behandlingId2 = UUID.randomUUID()
        val behandlingId3 = UUID.randomUUID()
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusSeconds(1), behandlingId1))
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusSeconds(1), behandlingId2))

        // Denne kommer seg videre, og skal ikke varsles om
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusSeconds(1), behandlingId3))
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusSeconds(5), behandlingId3))

        runBlocking { delay(1000) }
        rapid.sendTestMessage(tilstandEndretEvent(LocalDateTime.now().plusHours(1)))

        assertEquals(2, rapid.inspektør.size)
    }

    @Language("JSON")
    private fun tilstandEndretEvent(
        forventetFerdig: LocalDateTime,
        behandlingId: UUID = UUID.randomUUID(),
    ) = """{
        |  "@event_name": "behandling_endret_tilstand",
        |  "ident": "11109233444",
        |  "behandlingId": "$behandlingId",
        |  "forrigeTilstand": "UnderOpprettelse",
        |  "gjeldendeTilstand": "UnderBehandling",
        |  "forventetFerdig": "$forventetFerdig",
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
