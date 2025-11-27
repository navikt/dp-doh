package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingAvbruttCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingStatusCounter
import no.nav.dagpenger.doh.monitor.BehandlingMetrikker.behandlingVedtakCounter
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
        // Tall fra https://github.com/navikt/dp-behandling/blob/2ac5c756ad235112db8c10c032956ac275692092/mediator/src/test/kotlin/no/nav/dagpenger/behandling/scenario/ScenarioTest.kt#L75
        val vedtakFattet = les("/dp-behandling/behandlingsresultat_avslag.json")

        testRapid.sendTestMessage(
            vedtakFattet,
        )

        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                status = BehandlingStatusMonitor.Status.VEDTAK_FATTET,
                behandlingId = "019ac567-fe1c-7745-81e6-8f64cc3c3712",
                behandletHendelse = BehandletHendelse(id = "a06709e0-b05f-4a20-9c55-f12c358b7ec3", type = "Søknad"),
                opprettet = any(),
                årsak = null,
                førteTil = "Avslag",
                automatisk = false,
            )
        }

        assertEquals(Metrikker.behandlingVedtak(førteTil = "Avslag", automatisering = false), 1.0)
    }

    @Test
    fun `vedtak fattet med innvilgelse`() {
        // tatt fra https://github.com/navikt/dp-behandling/blob/2ac5c756ad235112db8c10c032956ac275692092/mediator/src/test/kotlin/no/nav/dagpenger/behandling/scenario/ScenarioTest.kt#L125
        val vedtakFattet = les("/dp-behandling/behandlingsresultat_innvilgelse.json")

        testRapid.sendTestMessage(
            vedtakFattet,
        )

        verify(exactly = 1) {
            vedtakBot.postBehandlingStatus(
                status = BehandlingStatusMonitor.Status.VEDTAK_FATTET,
                behandlingId = "019ac569-d6bf-7d72-a05c-dc1085c86b4c",
                behandletHendelse = BehandletHendelse(id = "cfb917d8-707d-4afd-952f-2cfda3825825", type = "Søknad"),
                opprettet = any(),
                årsak = null,
                førteTil = "Innvilgelse",
                automatisk = false,
            )
        }

        assertEquals(Metrikker.behandlingVedtak(førteTil = "Innvilgelse", automatisering = false), 1.0)
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

        fun behandlingVedtak(
            førteTil: String,
            automatisering: Boolean,
        ): Double =
            behandlingVedtakCounter
                .labelValues(førteTil, if (automatisering) "Automatisk" else "Manuell")
                .get()
    }
}
