package no.nav.dagpenger.doh.monitor

import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.doh.slack.SlackClient
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import kotlin.test.assertContains

class AppStateMonitorTest {
    private val slack = mockk<SlackClient>(relaxed = true)
    private val nedetidFørAlarm = Duration.ofMinutes(5)

    private val rapid = TestRapid().apply { AppStateMonitor(this, slack, Duration.ofMinutes(5)) }

    @Test
    fun `lager ikke varsler om appen vært nede lenge nok`() {
        val nå = LocalDateTime.now()
        rapid.sendTestMessage(
            getAppDownMessage(
                opprettet = nå,
                sistAktiv = nå.minus(nedetidFørAlarm).minusSeconds(1),
            ).toJson(),
        )

        val melding = slot<String>()
        verify(exactly = 1) { slack.postMessage(capture(melding)) }

        assertContains(melding.captured, "${nedetidFørAlarm.toMinutes()} minutter")
    }

    @Test
    fun `lager ikke varsler om appen bare har vært nede en liten stund`() {
        val nå = LocalDateTime.now()
        rapid.sendTestMessage(
            getAppDownMessage(
                opprettet = nå,
                sistAktiv = nå.minus(nedetidFørAlarm).plusMinutes(2),
            ).toJson(),
        )

        verify(exactly = 0) { slack.postMessage(any()) }
    }

    private fun getAppDownMessage(
        opprettet: LocalDateTime,
        sistAktiv: LocalDateTime,
    ) = JsonMessage.newMessage(
        "app_status",
        mapOf(
            "@opprettet" to opprettet,
            "states" to
                listOf(
                    mapOf(
                        "app" to "app1",
                        "state" to "DOWN",
                        "last_active_time" to sistAktiv,
                        "instances" to
                            listOf(
                                mapOf(
                                    "instance" to "instance1",
                                    "state" to "DOWN",
                                    "last_active_time" to sistAktiv,
                                ),
                            ),
                    ),
                ),
        ),
    )
}
