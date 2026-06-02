package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.behandling.InnvilgelseMedTilOgMedMonitorTest.Opprinnelse.ARVET
import no.nav.dagpenger.doh.monitor.behandling.InnvilgelseMedTilOgMedMonitorTest.Opprinnelse.NY
import no.nav.dagpenger.doh.slack.RampBot
import java.time.LocalDate
import java.time.LocalDate.now
import kotlin.test.Test

internal class InnvilgelseMedTilOgMedMonitorTest {
    private val rampBot = mockk<RampBot>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            InnvilgelseMedTilOgMedMonitor(it, rampBot)
        }

    @Test
    fun `varsler når innvilgelse har rettighetsperiode med tilOgMed != null, harRett == true, og opprinnelse == Ny`() {
        testRapid.sendTestMessage(lagTestdata(now().plusDays(1), true, NY))

        verify(exactly = 1) {
            rampBot.postInnvilgelseMedTilOgMed(
                behandlingId = "test-behandling-id",
                behandlingskjedeId = "test-behandlingskjede-id",
                opprettet = any(),
            )
        }
    }

    @Test
    fun `varsler ikke når innvilgelse har rettighetsperiode med tilOgMed != null, harRett == true, opprinnelse != Ny`() {
        testRapid.sendTestMessage(lagTestdata(now().plusDays(1), true, ARVET))

        verify(exactly = 0) {
            rampBot.postInnvilgelseMedTilOgMed(any(), any(), any())
        }
    }

    @Test
    fun `varsler ikke når innvilgelse har rettighetsperiode med tilOgMed != null, harRett == false, og opprinnelse == Ny`() {
        testRapid.sendTestMessage(lagTestdata(now().plusDays(1), false, NY))

        verify(exactly = 0) {
            rampBot.postInnvilgelseMedTilOgMed(any(), any(), any())
        }
    }

    @Test
    fun `varsler ikke når innvilgelse har rettighetsperiode med tilOgMed == null, harRett == true, og opprinnelse == Ny`() {
        testRapid.sendTestMessage(lagTestdata(null, true, NY))

        verify(exactly = 0) {
            rampBot.postInnvilgelseMedTilOgMed(any(), any(), any())
        }
    }

    private fun lagTestdata(
        tilOgMed: LocalDate?,
        harRett: Boolean,
        opprinnelse: Opprinnelse,
    ) = // language=JSON
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
              ${if (tilOgMed != null) "\"tilOgMed\": \"$tilOgMed\"," else ""}
              "harRett": $harRett,
              "opprinnelse": "${opprinnelse.jsonVerdi}"
            }
          ],
          "opprettet": "2024-04-10T12:28:31.533933"
        }
        """.trimIndent()

    @Suppress("unused")
    enum class Opprinnelse(
        val jsonVerdi: String,
    ) {
        NY("Ny"),
        ARVET("Arvet"),
    }
}
