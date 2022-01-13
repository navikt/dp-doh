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
                "0-3G",
                "Inntekt siste 36 mnd"
            ).toTypedArray()
        ).also {
            assertEquals(it, 3.0)
        }
    }
}

@Language("JSON")
private val behovJSON = """{
  "@event_name": "faktum_svar",
  "@behov": ["InntektSiste12Mnd", "InntektSiste3År"],
  "@løsning": [],
  "@opprettet": "2021-07-13T09:58:33.532432",
  "@id": "35d6da40-44a6-4774-a2a9-d5543374b714",
  "søknad_uuid": "6c7cd545-a06c-4769-b401-31795637a381",
  "resultat": false,
  "saksbehandles_på_ekte": true,
  "identer": [
    {
      "id": "12345678910",
      "type": "folkeregisterident",
      "historisk": false
    }
  ],
  "fakta": [
    {
      "navn": "Ønsker dagpenger fra dato",
      "id": "1",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 2000000.0
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 2000.0
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 20000.0
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 200000.0
    },
    {
      "navn": "Søknadstidspunkt",
      "id": "13",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-02"
    },
    {
      "navn": "Innsendt søknadsId",
      "id": "17",
      "roller": [],
      "type": "dokument",
      "godkjenner": [],
      "svar": {
        "lastOppTidsstempel": "2021-07-13T09:58:33.490810",
        "url": "ABCD123"
      }
    },
    {
      "navn": "Behandlingsdato",
      "id": "23",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    },
    {
      "navn": "Ordinær",
      "id": "27.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": false
    },
    {
      "navn": "Permittert",
      "id": "28.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": true
    },
    {
      "navn": "Lønnsgaranti",
      "id": "29.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": false
    },
    {
      "navn": "PermittertFiskeforedling",
      "id": "30.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": false
    },
    {
      "navn": "FagsakId i Arena",
      "id": "52",
      "roller": [],
      "type": "dokument",
      "godkjenner": [],
      "svar": {
        "lastOppTidsstempel": "2021-07-13T09:58:33.488590",
        "url": "123123"
      }
    },
    {
      "navn": "Over 67 år fra-dato",
      "id": "55",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-01"
    },
    {
      "navn": "Virkningsdato",
      "id": "4",
      "roller": [],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    },
    {
      "navn": "Første dato av virkningsdato og behandlingsdato",
      "id": "45",
      "roller": [],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    }
  ],
  "subsumsjoner": [
    {
      "lokalt_resultat": true,
      "navn": "Sjekk at `sluttårsaker med id 26` er lik 1",
      "forklaring": "saksbehandlerforklaring",
      "type": "Enkel subsumsjon"
    },
    {
      "lokalt_resultat": false,
      "navn": "under 67år",
      "type": "Deltre subsumsjon",
      "forklaring": "saksbehandlerforklaring",
      "subsumsjoner": [
        {
          "lokalt_resultat": false,
          "navn": "Sjekk at 'Virkningsdato med id 4' er før 'Over 67 år fra-dato med id 55'",
          "forklaring": "saksbehandlerforklaring",
          "type": "Enkel subsumsjon"
        }
      ]
    }
  ]
}
""".trimIndent()

@Language("JSON")
private val resultatJSON = """{
  "@event_name": "prosess_resultat",
  "@opprettet": "2021-07-13T09:58:33.532432",
  "@id": "35d6da40-44a6-4774-a2a9-d5543374b714",
  "søknad_uuid": "6c7cd545-a06c-4769-b401-31795637a381",
  "resultat": false,
  "saksbehandles_på_ekte": true,
  "identer": [
    {
      "id": "12345678910",
      "type": "folkeregisterident",
      "historisk": false
    }
  ],
  "fakta": [
    {
      "navn": "Ønsker dagpenger fra dato",
      "id": "1",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 2000000.0
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 2000.0
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 20000.0
    },
    {
      "navn": "Inntekt siste 36 mnd",
      "id": "6",
      "roller": [
        "nav"
      ],
      "type": "inntekt",
      "godkjenner": [],
      "svar": 200000.0
    },
    {
      "navn": "Søknadstidspunkt",
      "id": "13",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-02"
    },
    {
      "navn": "Innsendt søknadsId",
      "id": "17",
      "roller": [],
      "type": "dokument",
      "godkjenner": [],
      "svar": {
        "lastOppTidsstempel": "2021-07-13T09:58:33.490810",
        "url": "ABCD123"
      }
    },
    {
      "navn": "Behandlingsdato",
      "id": "23",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    },
    {
      "navn": "Ordinær",
      "id": "27.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": false
    },
    {
      "navn": "Permittert",
      "id": "28.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": true
    },
    {
      "navn": "Lønnsgaranti",
      "id": "29.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": false
    },
    {
      "navn": "PermittertFiskeforedling",
      "id": "30.1",
      "roller": [
        "nav"
      ],
      "type": "boolean",
      "godkjenner": [],
      "svar": false
    },
    {
      "navn": "FagsakId i Arena",
      "id": "52",
      "roller": [],
      "type": "dokument",
      "godkjenner": [],
      "svar": {
        "lastOppTidsstempel": "2021-07-13T09:58:33.488590",
        "url": "123123"
      }
    },
    {
      "navn": "Over 67 år fra-dato",
      "id": "55",
      "roller": [
        "nav"
      ],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-01"
    },
    {
      "navn": "Virkningsdato",
      "id": "4",
      "roller": [],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    },
    {
      "navn": "Første dato av virkningsdato og behandlingsdato",
      "id": "45",
      "roller": [],
      "type": "localdate",
      "godkjenner": [],
      "svar": "2018-01-05"
    }
  ],
  "subsumsjoner": [
    {
      "lokalt_resultat": true,
      "navn": "Sjekk at `sluttårsaker med id 26` er lik 1",
      "forklaring": "saksbehandlerforklaring",
      "type": "Enkel subsumsjon"
    },
    {
      "lokalt_resultat": false,
      "navn": "under 67år",
      "type": "Deltre subsumsjon",
      "forklaring": "saksbehandlerforklaring",
      "subsumsjoner": [
        {
          "lokalt_resultat": false,
          "navn": "Sjekk at 'Virkningsdato med id 4' er før 'Over 67 år fra-dato med id 55'",
          "forklaring": "saksbehandlerforklaring",
          "type": "Enkel subsumsjon"
        }
      ]
    }
  ]
}
""".trimIndent()
