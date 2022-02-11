package no.nav.dagpenger.doh

import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.doh.monitor.AktivitetsloggMonitor
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class AktivitetsloggMonitorTest {
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
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

        registry.getSampleValue(
            "dp_aktivitet_total",
            listOf(
                "alvorlighetsgrad",
                "melding",
                "tilstand",
                "harFlereFeil"
            ).toTypedArray(),
            listOf(
                "WARN",
                "foo",
                "ny",
                "false"
            ).toTypedArray()
        ).also {
            assertEquals(it, 1.0)
        }
    }

    @Test
    fun `skal telle om det er flere feil ved tilstandsendringer`() {
        rapid.sendTestMessage(vedtakEndretMedFlereFeilJson)

        registry.getSampleValue(
            "dp_aktivitet_total",
            listOf(
                "alvorlighetsgrad",
                "melding",
                "tilstand",
                "harFlereFeil"
            ).toTypedArray(),
            listOf(
                "ERROR",
                "foo",
                "ny",
                "true"
            ).toTypedArray()
        ).also {
            assertEquals(it, 2.0)
        }
    }
}

//language=JSON
private val vedtakEndretJson =
    """{
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
    """{
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
