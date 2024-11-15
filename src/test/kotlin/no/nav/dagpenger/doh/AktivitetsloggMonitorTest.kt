package no.nav.dagpenger.doh

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.dagpenger.doh.monitor.AktivitetsloggMonitor
import no.nav.dagpenger.doh.monitor.AktivitetsloggMonitor.Companion.aktivitetCounter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AktivitetsloggMonitorTest {
    // private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val rapid by lazy {
        TestRapid().apply {
            AktivitetsloggMonitor(this)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal måle tilstandsendringer`() {
        rapid.sendTestMessage(vedtakEndretJson)

        assertEquals(aktivitetCounter.labelValues("WARN", "foo", "ny", "false").get(), 1.0)
    }

    @Test
    fun `skal telle om det er flere feil ved tilstandsendringer`() {
        rapid.sendTestMessage(vedtakEndretMedFlereFeilJson)

        assertEquals(aktivitetCounter.labelValues("ERROR", "foo", "ny", "true").get(), 2.0)
    }
}

//language=JSON
private val vedtakEndretJson =
    """
    {
      "@event_name": "vedtak_endret",
      "gjeldendeTilstand": "ferdig",
      "forrigeTilstand": "ny",
      "aktivitetslogg": {
        "aktiviteter": [
          {
            "alvorlighetsgrad": "WARN",
            "melding": "foo"
          }
        ]
      }
    }
    """.trimIndent()

//language=JSON
private val vedtakEndretMedFlereFeilJson =
    """
    {
      "@event_name": "vedtak_endret",
      "gjeldendeTilstand": "ferdig",
      "forrigeTilstand": "ny",
      "aktivitetslogg": {
        "aktiviteter": [
          {
            "alvorlighetsgrad": "ERROR",
            "melding": "foo"
          },
          {
            "alvorlighetsgrad": "ERROR",
            "melding": "foo"
          }
        ]
      }
    }
    """.trimIndent()
