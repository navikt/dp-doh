package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import kotlin.test.Test

internal class InnvilgelseMedTilOgMedMonitorTest {
    private val vedtakBot = mockk<VedtakBot>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            InnvilgelseMedTilOgMedMonitor(it, vedtakBot)
        }

    @Test
    fun `varsler når innvilgelse har rettighetsperiode med tilOgMed og opprinnelse Ny`() {
        testRapid.sendTestMessage(innvilgelseSomErNyMedTilOgMed)

        verify(exactly = 1) {
            vedtakBot.postInnvilgelseMedTilOgMed(
                behandlingId = "test-behandling-id",
                behandlingskjedeId = "test-behandlingskjede-id",
                opprettet = any(),
            )
        }
    }

    @Test
    fun `varsler ikke når rettighetsperiode er Ny, men mangler tilOgMed`() {
        testRapid.sendTestMessage(innvilgelseSomErNyMenManglerTilOgMed)

        verify(exactly = 0) {
            vedtakBot.postInnvilgelseMedTilOgMed(any(), any(), any())
        }
    }

    @Test
    fun `varsler ikke når tilOgMed er med, men opprinnelse ikke er Ny`() {
        testRapid.sendTestMessage(innvilgelseMedTilOgMedMenIkkeNy)

        verify(exactly = 0) {
            vedtakBot.postInnvilgelseMedTilOgMed(any(), any(), any())
        }
    }

    // language=JSON
    private val innvilgelseSomErNyMedTilOgMed =
        """
        {
          "@event_name": "behandlingsresultat",
          "behandlingId": "test-behandling-id",
          "behandlingskjedeId" : "test-behandlingskjede-id",
          "behandletHendelse": {
            "id": "test-hendelse-id",
            "type": "Søknad"
          },
          "ident": "12345678901",
          "automatisk": false,
          "rettighetsperioder": [
            {
              "fraOgMed": "2024-01-01",
              "tilOgMed": "2024-06-30",
              "harRett": true,
              "opprinnelse": "Ny"
            },
            {
              "fraOgMed": "2024-01-02",
              "tilOgMed": "2024-06-28",
              "harRett": true,
              "opprinnelse": "Ny"
            }
          ],
          "opprettet": "2024-04-10T12:28:31.533933"
        }
        """.trimIndent()

    // language=JSON
    private val innvilgelseSomErNyMenManglerTilOgMed =
        """
        {
          "@event_name": "behandlingsresultat",
          "behandlingId": "test-behandling-id-2",
          "behandlingskjedeId" : "019ac567-fe1c-7745-81e6-8f64cc3c3712",
          "behandletHendelse": {
            "id": "test-hendelse-id-2",
            "type": "Søknad"
          },
          "ident": "12345678901",
          "automatisk": false,
          "rettighetsperioder": [
            {
              "fraOgMed": "2024-01-01",
              "harRett": true,
              "opprinnelse": "Ny"
            }
          ],
          "opprettet": "2024-04-10T12:28:31.533933"
        }
        """.trimIndent()

    // language=JSON
    private val innvilgelseMedTilOgMedMenIkkeNy =
        """
        {
          "@event_name": "behandlingsresultat",
          "behandlingId": "test-behandling-id-3",
          "behandlingskjedeId" : "019ac567-fe1c-7745-81e6-8f64cc3c3712",
          "behandletHendelse": {
            "id": "test-hendelse-id-3",
            "type": "Søknad"
          },
          "ident": "12345678901",
          "automatisk": false,
          "rettighetsperioder": [
            {
              "fraOgMed": "2024-01-01",
              "tilOgMed": "2024-06-30",
              "harRett": true,
              "opprinnelse": "Arvet"
            }
          ],
          "opprettet": "2024-04-10T12:28:31.533933"
        }
        """.trimIndent()
}
