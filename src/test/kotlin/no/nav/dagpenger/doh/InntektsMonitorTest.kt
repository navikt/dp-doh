package no.nav.dagpenger.doh

import io.prometheus.client.CollectorRegistry
import no.nav.dagpenger.doh.monitor.InntektsMonitor
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class InntektsMonitorTest {
    private val registry: CollectorRegistry = CollectorRegistry.defaultRegistry
    private val rapid by lazy {
        TestRapid().apply {
            InntektsMonitor(this)
        }
    }

    @AfterEach
    fun cleanUp() {
        rapid.reset()
    }

    @Test
    fun `skal poste måle inntekt i Prometheus`() {
        rapid.sendTestMessage(behovJSON)

        registry.getSampleValue(
            "dp_inntekt_count",
            listOf(
                "inntektsgruppe",
                "type",
            ).toTypedArray(),
            listOf(
                "3-6G",
                "InntektSiste3År"
            ).toTypedArray()
        ).also {
            assertEquals(it, 1.0)
        }
    }
}

@Language("JSON")
private val behovJSON = """{
  "@event_name": "faktum_svar",
  "@opprettet": "2022-01-13T15:03:12.274510120",
  "@id": "b4d15915-e89a-4e6f-a968-a6d1caa3fd90",
  "søknad_uuid": "01acedb6-1136-4bbb-b394-d799eb0b2d67",
  "seksjon_navn": "inntekter",
  "fakta": [
    {
      "id": "6",
      "behov": "InntektSiste3År",
      "clazz": "inntekt"
    },
    {
      "id": "7",
      "behov": "InntektSiste12Mnd",
      "clazz": "inntekt"
    }
  ],
  "@behov": [
    "InntektSiste3År",
    "InntektSiste12Mnd"
  ],
  "Virkningstidspunkt": "2022-01-13",
  "FangstOgFiskeInntektSiste36mnd": false,
  "ØnskerDagpengerFraDato": "2022-01-01",
  "Søknadstidspunkt": "2022-01-13",
  "system_read_count": 1,
  "system_participating_services": [
    {
      "service": "dp-oppslag-inntekt",
      "instance": "dp-oppslag-inntekt-84c69b97d-nvvcf",
      "time": "2022-01-13T15:03:12.288632"
    },
    {
      "service": "dp-doh",
      "instance": "dp-doh-7dcc747f98-j49dc",
      "time": "2022-01-13T14:04:38.686890519"
    }
  ],
  "@løsning": {
    "InntektSiste3År": 398571.08,
    "InntektSiste12Mnd": 274792.8
  }
}
""".trimIndent()
