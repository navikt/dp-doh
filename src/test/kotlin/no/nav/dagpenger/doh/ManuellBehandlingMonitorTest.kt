package no.nav.dagpenger.doh

import io.mockk.mockk
import no.nav.dagpenger.doh.monitor.ManuellBehandlingMonitor
import no.nav.dagpenger.doh.slack.SlackBot
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class ManuellBehandlingMonitorTest {
    private val slack = mockk<SlackBot>(relaxed = true)
    private val rapid by lazy {
        TestRapid().apply {
            ManuellBehandlingMonitor(this, slack)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal poste søknader som går til manuell på Slack`() {
        /*every {
            slack.chatPostMessage { captureLambda() }
        } answers {
            lambda<() -> ChatPostMessageResponse>().invoke()
        }*/

        rapid.sendTestMessage(manuellBehandlingJson)
        /*verify(exactly = 1) {
            slack.chatPostMessage { allAny() }
        }*/
    }
}

//language=JSON
private val manuellBehandlingJson =
    """{
  "@event_name": "manuell_behandling",
  "@opprettet": "${LocalDateTime.now()}",
  "søknad_uuid": "${UUID.randomUUID()}",
  "seksjon_navn": "ny"
}
    """.trimIndent()
