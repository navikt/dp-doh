package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test

class KorrigertMeldekortMonitorTest {
    private val rapid = TestRapid()
    private val slack = mockk<VedtakBot>(relaxed = true)

    init {
        KorrigertMeldekortMonitor(rapid, slack)
    }

    @Test
    fun `varsler ved korrigert meldekort`() {
        rapid.sendTestMessage(meldekortJSON)

        verify {
            slack.korrigertMeldekort(any())
        }
    }

    @Language("JSON")
    private val meldekortJSON =
        """
        {
          "@event_name": "meldekort_innsendt",
          "id": "0199710e-b38e-7b39-ae82-d9cef6f336db",
          "ident": "27848499094",
          "periode": {
            "fraOgMed": "2025-08-18",
            "tilOgMed": "2025-08-31"
          },
          "dager": [
            {
              "dagIndex": 0,
              "dato": "2025-08-18",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 1,
              "dato": "2025-08-19",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 2,
              "dato": "2025-08-20",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 3,
              "dato": "2025-08-21",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 4,
              "dato": "2025-08-22",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 5,
              "dato": "2025-08-23",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 6,
              "dato": "2025-08-24",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 7,
              "dato": "2025-08-25",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 8,
              "dato": "2025-08-26",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 9,
              "dato": "2025-08-27",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 10,
              "dato": "2025-08-28",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 11,
              "dato": "2025-08-29",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 12,
              "dato": "2025-08-30",
              "aktiviteter": [],
              "meldt": true
            },
            {
              "dagIndex": 13,
              "dato": "2025-08-31",
              "aktiviteter": [],
              "meldt": true
            }
          ],
          "sisteFristForTrekk": "2025-09-28",
          "kanSendesFra": "2025-09-20",
          "kanSendes": true,
          "kanEndres": true,
          "begrunnelseEndring": null,
          "registrertArbeidssoker": true,
          "originalMeldekortId": "123-123-123",
          "status": "Innsendt",
          "kilde": {
            "rolle": "Bruker",
            "ident": "27848499094"
          },
          "type": "Original",
          "opprettetAv": "Dagpenger",
          "innsendtTidspunkt": "2025-09-22T12:53:45"
        }
        """.trimIndent()
}
