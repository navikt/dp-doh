package no.nav.dagpenger.doh

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.monitor.VedtakFattetMonitor
import no.nav.dagpenger.doh.slack.SlackClient
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
        every {
            slack.postMessage(allAny())
        } answers {}

        rapid.sendTestMessage(vedtakEndretJson)

        verify(exactly = 1) {
            slack.postMessage(allAny())
        }
    }

    @Test
    fun `skal logge errors ved kjipe meldinger`() {
        rapid.sendTestMessage(kjipMelding)

        verify(exactly = 0) {
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
  "@opprettet": "${LocalDateTime.now()}"
}
    """.trimIndent()

//language=JSON
private val kjipMelding =
    """{
  "@event_name": "vedtak_endret",
  "gjeldendeTilstand": "VedtakFattet"
}
    """.trimIndent()
