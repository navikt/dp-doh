package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import org.junit.jupiter.api.Assertions.assertTrue
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertContains

class UtbetalingFeilUtbetalingsdagerMonitorTest {
    private val testRapid = TestRapid()
    private val vedtakBot =
        mockk<VedtakBot>().also {
            every { it.utbetalingStatus(any<String>(), any<String>(), any<String>(), any<LocalDateTime>()) } just Runs
        }

    @Test
    fun `skal ta i mot utbetaling hendelser`() {
        UtbetalingFeilUtbetalingsdagerMonitor(testRapid, vedtakBot)
        testRapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "utbetaling_feil_grensedato",
                    mapOf(
                        "behandlingId" to "123e4567-e89b-12d3-a456-426614174000",
                        "eksternBehandlingId" to "Ej5FZ+ibEtOkVkJmFBdAAA==",
                        "sakId" to "123e4567-e89b-12d3-a456-426614174001",
                        "eksternSakId" to "Ej5FZ+ibEtOkVkJmFBdAAQ==",
                        "førsteUtbetalingsdag" to "2024-01-01",
                        "førsteDagFraHelVed" to "2024-01-05",
                    ),
                ).toJson(),
        )

        val melding = slot<String>()
        verify(exactly = 1) {
            vedtakBot.utbetalingStatus(capture(melding), any<String>(), any<String>(), any<LocalDateTime>())
        }
        assertTrue(melding.isCaptured)
        assertContains(melding.captured, ":alert: :alert: :alert: Utbetalingsdager stemmer ikke med behandling")
    }
}
