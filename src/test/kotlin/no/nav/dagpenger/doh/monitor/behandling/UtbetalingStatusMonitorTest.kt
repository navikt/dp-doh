package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContains
import kotlin.test.assertEquals

class UtbetalingStatusMonitorTest {
    private val testRapid = TestRapid()
    private val slackMeldinger = mutableListOf<String>()
    private val vedtakBot =
        mockk<VedtakBot>().also {
            every { it.utbetalingStatus(capture(slackMeldinger), any<String>(), any<String>(), any<LocalDateTime>()) } just Runs
        }

    @Test
    fun `skal ta i mot utbetaling hendelser`() {
        UtbetalingStatusMonitor(testRapid, vedtakBot)
        val utbetalingHendelser =
            listOf(
                "utbetaling_mottatt",
                "utbetaling_sendt",
                "utbetaling_feilet",
                "utbetaling_utført",
            )

        utbetalingHendelser.forEach { eventName ->
            //language=JSON
            val melding =
                """
                {
                  "@event_name": "$eventName",
                  "ident": "12345678901",
                  "behandlingId": "123e4567-e89b-12d3-a456-426614174000",
                  "eksternBehandlingId": "Ej5FZ+ibEtOkVkJmFBdAAA==",
                  "sakId": "123e4567-e89b-12d3-a456-426614174001",
                  "eksternSakId": "Ej5FZ+ibEtOkVkJmFBdAAQ==",
                  "behandletHendelseId": "m1",
                  "behandletHendelseType": "Meldekort",
                  "meldekortId": "m1",
                  "status": "MOTTATT",
                  "@id": "c0dd639d-b676-4bc9-a41f-71510ab837a6",
                  "@opprettet": "2025-11-21T12:10:18.210682",
                  "system_read_count": 0,
                  "system_participating_services": [
                    {
                      "id": "c0dd639d-b676-4bc9-a41f-71510ab837a6",
                      "time": "2025-11-21T12:10:18.210682"
                    }
                  ]
                }
                """.trimIndent()

            testRapid.sendTestMessage(melding)
        }

        verify(exactly = 2) {
            vedtakBot.utbetalingStatus(any<String>(), any<String>(), any<String>(), any<LocalDateTime>())
        }

        assertEquals(2, slackMeldinger.size)
        assertContains(slackMeldinger.first(), "*Utbetaling feilet:* Utbetalingen stoppet underveis")
        assertContains(slackMeldinger.first(), "*Referanser:* Behandling ID:")
        assertContains(slackMeldinger.first(), "*Helved-referanser:* Behandling")

        assertContains(slackMeldinger.last(), "*Utbetaling utført:* Utbetalingen ble gjennomført")
        assertContains(slackMeldinger.last(), "*Referanser:* Behandling ID:")
        assertContains(slackMeldinger.last(), "*Helved-referanser:* Behandling")
    }
}
