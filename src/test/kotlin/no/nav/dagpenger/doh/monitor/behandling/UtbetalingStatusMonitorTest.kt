package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import java.util.UUID
import kotlin.test.Test

class UtbetalingStatusMonitorTest {
    private val testRapid = TestRapid()
    private val vedtakBot =
        mockk<VedtakBot>().also {
            every { it.utbetalingStatus(any()) } just Runs
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
                    "behandlingId": "${UUID.randomUUID()}",
                    "sakId": "${UUID.randomUUID()}",
                    "meldekortId": "${UUID.randomUUID()}"
                }
                """.trimIndent()

            testRapid.sendTestMessage(melding)
        }

        verify(exactly = 3) {
            vedtakBot.utbetalingStatus(any())
        }
    }
}
