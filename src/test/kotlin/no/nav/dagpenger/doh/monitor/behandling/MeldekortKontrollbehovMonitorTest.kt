package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.doh.slack.VedtakBot
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MeldekortKontrollbehovMonitorTest {
    private val rapid = TestRapid()
    private val slack = mockk<VedtakBot>(relaxed = true)

    init {
        MeldekortKontrollbehovMonitor(rapid, slack)
    }

    @Test
    fun `varsler om meldekortberegning med kontrollbehov`() {
        rapid.sendTestMessage(meldingOmKontrollbehov)

        verify(exactly = 1) {
            slack.meldekortKontrollbehov(
                match {
                    it.contains("*Begrunnelse*:") &&
                        it.contains("- Inneholder dager med arbeidstimer") &&
                        !it.contains("Meldekortet har innhold") &&
                        !it.contains("registrert endring")
                },
            )
        }
    }

    @Test
    fun `varsler ikke for andre hendelser`() {
        rapid.sendTestMessage(
            JsonMessage
                .newMessage(
                    "behandling_opprettet",
                    mapOf("behandlingId" to UUID.randomUUID()),
                ).toJson(),
        )

        verify(exactly = 0) {
            slack.meldekortKontrollbehov(any())
        }
    }

    private val meldingOmKontrollbehov =
        JsonMessage
            .newMessage(
                "meldekortberegning_trenger_kontrollregning",
                mapOf(
                    "ident" to "12345678901",
                    "behandlingId" to UUID.randomUUID().toString(),
                    "behandletHendelseId" to UUID.randomUUID().toString(),
                    "detaljer" to
                        mapOf(
                            "meldekortSendtForSent" to false,
                            "harMeldtAnnenAktivitet" to false,
                            "harMeldtArbeidstimer" to true,
                            "harEndringISats" to false,
                            "harEndringiArbeidstid" to false,
                            "harEndringITerskel" to false,
                            "ileggesSanksjon" to false,
                            "harEndretRettighetsperiode" to false,
                        ),
                ),
            ).toJson()
}
