package no.nav.dagpenger.doh.monitor.behandling

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.doh.slack.VedtakBot
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import kotlin.test.Test

class BehandlingStatusMonitorTest {
    private val vedtakBot = mockk<VedtakBot>(relaxed = true)
    private val testRapid =
        TestRapid().also {
            BehandlingStatusMonitor(it, vedtakBot)
        }

    @Test
    fun `behandling opprettet`() {
        testRapid.sendTestMessage(behandlingOpprettet)

        assertEquals(Metrikker.behandlingStatus("behandling_opprettet"), 1.0)
    }

    @Test
    fun `vedtak fattet`() {
        val vedtakFattet =
            requireNotNull(this.javaClass.getResource("/dp-behandling/vedtak_fattet.json")?.readText()) {
                "Fant ikke dp-behandling/vedtak_fattet.json"
            }

        testRapid.sendTestMessage(
            vedtakFattet,
        )

        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                status = BehandlingStatusMonitor.Status.VEDTAK_FATTET,
                behandlingId = "0192241a-8301-7b40-9581-d1c2416c9dee",
                søknadId = any(),
                opprettet = any(),
                årsak = null,
                avklaringer = emptyList(),
                utfall = false,
                automatisk = true,
            )
        }

        assertEquals(Metrikker.behandlingVedtak(utfall = false, automatisering = true), 1.0)
    }

    @Test
    fun `forslag til vedtak`() {
        testRapid.sendTestMessage(
            forslagTilVedtakMessage,
        )

        val avklaringer = slot<List<String>>()
        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK,
                "018ec78d-4f15-7a02-bdf9-0e67129a0411",
                any(),
                any(),
                null,
                capture(avklaringer),
                false,
            )
        }
        assertEquals(avklaringer.captured.size, 6)

        assertEquals(Metrikker.behandlingStatus("forslag_til_vedtak"), 1.0)
    }

    @Test
    fun `behandling avbrutt`() {
        testRapid.sendTestMessage(
            behandlingAvbrutt,
        )

        verify(exactly = 0) {
            vedtakBot.postBehandlingStatus(
                BehandlingStatusMonitor.Status.BEHANDLING_AVBRUTT,
                "018ec78d-4f15-7a02-bdf9-0e67129a0411",
                any(),
                any(),
                "For mye inntekt",
                emptyList(),
                null,
            )
        }

        assertEquals(Metrikker.behandlingAvbrutt("For mye inntekt"), 1.0)
    }

    // language=JSON
    private val behandlingOpprettet =
        """
        {
          "@event_name": "behandling_opprettet",
          "ident": "12345678901",
          "behandlingId": "018ec78d-4f15-7a02-bdf9-0e67129a0411",
          "gjelderDato": "2024-04-10",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "@id": "4461e599-e60e-41f6-b052-771d6bde0108",
          "@opprettet": "2024-04-10T12:28:31.533933"
        }
        """.trimIndent()

    // language=JSON
    private val forslagTilVedtakMessage =
        """
        {
          "@event_name": "forslag_til_vedtak",
          "utfall": false,
          "harAvklart": "Krav på dagpenger",
          "ident": "12345678901",
          "behandlingId": "018ec78d-4f15-7a02-bdf9-0e67129a0411",
          "gjelderDato": "2024-04-10",
          "fagsakId": "123",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "@id": "4461e599-e60e-41f6-b052-771d6bde0108",
          "@opprettet": "2024-04-10T12:28:31.533933",
          "avklaringer": [
            {
              "type": "SvangerskapsrelaterteSykepenger",
              "utfall": "Manuell",
              "begrunnelse": "Personen har sykepenger som kan være svangerskapsrelaterte"
            },
            {
              "type": "EØSArbeid",
              "utfall": "Manuell",
              "begrunnelse": "Personen har oppgitt arbeid fra EØS"
            },
            {
              "type": "JobbetUtenforNorge",
              "utfall": "Manuell",
              "begrunnelse": "Personen har oppgitt arbeid utenfor Norge"
            },
            {
              "type": "InntektNesteKalendermåned",
              "utfall": "Manuell",
              "begrunnelse": "Personen har inntekter som tilhører neste inntektsperiode"
            },
            {
              "type": "HattLukkedeSakerSiste8Uker",
              "utfall": "Manuell",
              "begrunnelse": "Personen har lukkede saker i Arena siste 8 uker"
            },
            {
              "type": "MuligGjenopptak",
              "utfall": "Manuell",
              "begrunnelse": " Personen har åpne saker i Arena som kan være gjenopptak "
            }
          ]
        }
        """.trimIndent()

    // language=JSON
    private val behandlingAvbrutt =
        """
        {
          "@event_name": "behandling_avbrutt",
          "harAvklart": "Krav på dagpenger",
          "ident": "12345678901",
          "behandlingId": "018ec78d-4f15-7a02-bdf9-0e67129a0411",
          "gjelderDato": "2024-04-10",
          "fagsakId": "123",
          "årsak": "For mye inntekt",
          "søknadId": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
          "@id": "4461e599-e60e-41f6-b052-771d6bde0108",
          "@opprettet": "2024-04-10T12:28:31.533933"
        }
        """.trimIndent()

    private object Metrikker {
        fun behandlingStatus(status: String): Double =
            CollectorRegistry.defaultRegistry.getSampleValue(
                "dp_behandling_status_total",
                "status" to status,
            )

        fun behandlingAvbrutt(årsak: String): Double =
            CollectorRegistry.defaultRegistry.getSampleValue(
                "dp_behandling_avbrutt_total",
                "aarsak" to årsak,
            )

        fun behandlingVedtak(
            utfall: Boolean,
            automatisering: Boolean,
        ): Double =
            CollectorRegistry.defaultRegistry.getSampleValue(
                "dp_behandling_vedtak_total",
                "utfall" to utfall.toString(),
                "automatisk" to if (automatisering) "Automatisk" else "Manuell",
            )

        private fun CollectorRegistry.getSampleValue(
            name: String,
            vararg labels: Pair<String, String>,
        ): Double {
            val labelsMap = labels.toMap()
            return getSampleValue(
                name,
                labelsMap.keys.toTypedArray<String>(),
                labelsMap.values.toTypedArray<String>(),
            )
        }
    }
}
