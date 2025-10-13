package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import org.junit.jupiter.api.Test
import java.util.UUID

internal class BeregnMeldekortMottakTest {
    private val eksternMeldekortId: Long = 1234567890L
    private val meldekortId: UUID = UUID.randomUUID()
    private val ident = "12345678901"

    private val rapid = TestRapid()
    private val slack = mockk<VedtakBot>(relaxed = true)

    init {
        BeregnMeldekortMottak(rapid, slack)
    }

    @Test
    fun `varsler ved beregning av  meldekort`() {
        rapid.sendTestMessage(meldekortJSON)

        verify {
            slack.skalBeregnemeldekort(any())
        }
    }

    private val meldekortJSON =
        JsonMessage
            .newMessage(
                "beregn_meldekort",
                mapOf(
                    "meldekortId" to meldekortId,
                    "ident" to ident,
                    "eksternMeldekortId" to eksternMeldekortId,
                ),
            ).toJson()
}
