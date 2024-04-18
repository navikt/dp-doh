package no.nav.dagpenger.doh.monitor

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.behandling.ManuellBehandlingMonitor
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ManuellBehandlingMonitorTest {
    private val vedtakBotMock = mockk<VedtakBot>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            ManuellBehandlingMonitor(it, vedtakBot = vedtakBotMock)
        }

    @Test
    fun `skal lytte på behov med ManuellBehandlingAvklart`() {
        val årsaker = slot<List<String>>()
        testRapid.sendTestMessage(manuellBehandlingAvklartBehov)
        verify(exactly = 1) { vedtakBotMock.postManuellBehandling(any(), any(), capture(årsaker), any()) }
        assertTrue { årsaker.isCaptured }
        val faktiskeÅrsaker = årsaker.captured
        assertTrue("Burde ha årsaker") { faktiskeÅrsaker.isNotEmpty() }
        val forventedÅrsaker = listOf("begrunnelse1", "begrunnelse2")
        assertEquals(faktiskeÅrsaker, forventedÅrsaker)
    }

    // language=JSON
    private val manuellBehandlingAvklartBehov =
        """
        {
          "@id": "b86f74e1-c156-4889-9aa6-d7b577efd440",
          "ident": "16836599563",
          "@behov": [
            "AvklaringManuellBehandling"
          ],
          "@behovId": "0a2afbc8-5706-4525-8e54-80e528533671",
          "@løsning": {
            "AvklaringManuellBehandling": true
          },
          "@final": true,
          "søknadId": "60c76d22-14d9-4b3d-999d-e50ab4fa4f36",
          "@opprettet": "2024-04-16T08:47:57.947819347",
          "@event_name": "behov",
          "vurderinger": [
            {
              "type": "type1",
              "begrunnelse": "begrunnelse1",
              "utfall": "Manuell"
            },
             {
              "type": "type2",
              "begrunnelse": "begrunnelse2",
              "utfall": "Manuell"
            },
             {
              "type": "type3",
              "begrunnelse": "begrunnelse3",
              "utfall": "Automatisk"
            }
        ],
          "behandlingId": "018ee5a9-7ff1-7c7c-b50d-d1bb2e486082",
          "system_read_count": 1,
          "system_participating_services": [
            {
              "id": "b86f74e1-c156-4889-9aa6-d7b577efd440",
              "time": "2024-04-16T08:47:57.947819347",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-manuell-behandling:2024.04.16-06.41-072e145",
              "service": "dp-manuell-behandling",
              "instance": "dp-manuell-behandling-844f99c4c4-sdvd6"
            },
            {
              "id": "b86f74e1-c156-4889-9aa6-d7b577efd440",
              "time": "2024-04-16T08:47:57.988024037",
              "image": "europe-north1-docker.pkg.dev/nais-management-233d/teamdagpenger/dp-behandling:2024.04.15-13.27-7342da4",
              "service": "dp-behandling",
              "instance": "dp-behandling-564fd59454-hks9j"
            }
          ]
        }
        """.trimIndent()
}
