package no.nav.dagpenger.doh

import io.kotest.matchers.shouldBe
import io.prometheus.client.CollectorRegistry
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test

internal class VedtakEndretMonitorTest {
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val rapid by lazy {
        TestRapid().apply {
            VedtakEndretMonitor(this)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal måle tilstandsendringer`() {
        rapid.sendTestMessage(vedtakEndretJson)

        registry.getSampleValue(
            "dp_vedtak_endret",
            listOf(
                "tilstand",
                "forrigeTilstand"
            ).toTypedArray(),
            listOf(
                "ferdig",
                "ny"
            ).toTypedArray()
        ).also {
            it shouldBe 1.0
        }
    }
}

//language=JSON
private val vedtakEndretJson = """{
  "@event_name": "vedtak_endret",
  "gjeldendeTilstand": "ferdig",
  "forrigeTilstand": "ny"
}""".trimIndent()
