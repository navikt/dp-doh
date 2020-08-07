package no.nav.dagpenger.doh

import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class VedtakFattetMonitorTest {
    private val slack = mockk<SlackClient>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            VedtakFattetMonitor(this, slack)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal poste fattede vedtak på Slack`() {
        rapid.sendTestMessage(vedtakEndretJson)

        verify(exactly = 1) {
            slack.postMessage(allAny())
        }
    }
}

//language=JSON
private val vedtakEndretJson =
    """{
  "@event_name": "vedtak_endret",
  "gjeldendeTilstand": "VedtakFattet",
  "forrigeTilstand": "ny",
  "vedtakId": "123",
  "behov_opprettet": "${LocalDateTime.now()}"
}
    """.trimIndent()
