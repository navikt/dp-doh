package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingAvbruttCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingVedtakCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingVilkårCounter
import no.nav.dagpenger.doh.monitor.behandling.BehandlingStatusMonitor.BehandletHendelse
import no.nav.dagpenger.doh.slack.VedtakBot
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
    fun `vedtak fattet med avslag`() {
        val vedtakFattet = les("/dp-behandling/vedtak_fattet_avslag.json")

        testRapid.sendTestMessage(
            vedtakFattet,
        )

        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                status = BehandlingStatusMonitor.Status.VEDTAK_FATTET,
                behandlingId = "0192241a-8301-7b40-9581-d1c2416c9dee",
                behandletHendelse = BehandletHendelse(type = "Søknad", id = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"),
                opprettet = any(),
                årsak = null,
                utfall = false,
                automatisk = true,
            )
        }

        assertEquals(Metrikker.behandlingVedtak(utfall = false, automatisering = true), 1.0)
    }

    @Test
    fun `vedtak fattet med innvilgelse`() {
        val vedtakFattet = les("/dp-behandling/vedtak_fattet_innvilgelse.json")

        testRapid.sendTestMessage(
            vedtakFattet,
        )

        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                status = BehandlingStatusMonitor.Status.VEDTAK_FATTET,
                behandlingId = "01924774-13c6-7411-a408-20e95689a030",
                behandletHendelse = BehandletHendelse(type = "Søknad", id = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"),
                opprettet = any(),
                årsak = null,
                utfall = true,
                automatisk = false,
            )
        }

        assertEquals(Metrikker.behandlingVedtak(utfall = true, automatisering = false), 1.0)
    }

    @Test
    fun `forslag til vedtak`() {
        testRapid.sendTestMessage(
            forslagTilVedtakMessage,
        )

        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                status = BehandlingStatusMonitor.Status.FORSLAG_TIL_VEDTAK,
                behandlingId = "018ec78d-4f15-7a02-bdf9-0e67129a0411",
                behandletHendelse = BehandletHendelse(type = "Søknad", id = "4afce924-6cb4-4ab4-a92b-fe91e24f31bf"),
                opprettet = any(),
                årsak = null,
                utfall = false,
                automatisk = true,
            )
        }

        assertEquals(Metrikker.behandlingStatus("forslag_til_vedtak"), 1.0)
        assertEquals(
            Metrikker.vilkår(
                status = "forslag_til_vedtak",
                utfall = "false",
                automatisk = "true",
                navn = "Alder og sånt",
                vurdering = "Oppfylt",
            ),
            1.0,
        )
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
            )
        }

        assertEquals(Metrikker.behandlingAvbrutt("For mye inntekt"), 1.0)
    }

    private fun les(navn: String): String =
        requireNotNull(this.javaClass.getResource(navn)?.readText()) {
            "Fant ikke $navn"
        }

    // language=JSON
    private val behandlingOpprettet =
        """
        {
          "@event_name": "behandling_opprettet",
            "behandletHendelse" : {
              "id": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
              "type": "Søknad",
              "datatype": "UUID"
            },
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
            "behandletHendelse" : {
              "id": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
              "type": "Søknad",
              "datatype": "UUID"
            },
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
          "fastsatt": {
            "utfall": false
          },
          "automatisk": true,
          "vilkår": [
            {
              "navn": "Alder og sånt",
              "status": "Oppfylt"
            }
          ]
        }
        """.trimIndent()

    // language=JSON
    private val behandlingAvbrutt =
        """
        {
          "@event_name": "behandling_avbrutt",
            "behandletHendelse" : {
              "id": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
              "type": "Søknad",
              "datatype": "UUID"
            },
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
        fun behandlingStatus(status: String): Double = behandlingStatusCounter.labelValues(status).get()

        fun behandlingAvbrutt(årsak: String): Double = behandlingAvbruttCounter.labelValues(årsak).get()

        fun vilkår(
            status: String,
            utfall: String,
            automatisk: String,
            navn: String,
            vurdering: String,
        ): Double = behandlingVilkårCounter.labelValues(status, utfall, automatisk, navn, vurdering).get()

        fun behandlingVedtak(
            utfall: Boolean,
            automatisering: Boolean,
        ): Double =
            behandlingVedtakCounter
                .labelValues(utfall.toString(), if (automatisering) "Automatisk" else "Manuell")
                .get()
    }
}
