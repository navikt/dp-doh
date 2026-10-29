package no.nav.dagpenger.doh.monitor.behandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import java.util.UUID

class VurderAvklaringMottakTest {
    private val testRapid = TestRapid()
    private val regelKjøringLoopSlackBot = mockk<RegelKjøringLoopSlackBot>(relaxed = true)

    init {
        VurderAvklaringMottak(testRapid, regelKjøringLoopSlackBot)
    }

    @Test
    fun `reagerer på REGELKJØRING_LOOP avklaringer og poster alarm på Slack`() {
        val behandlingId = UUID.randomUUID()
        testRapid.sendTestMessage(
            nyAvklaring(
                avklaringskode = "REGELKJØRING_LOOP",
                behandlingId = behandlingId,
            ),
        )

        verify { regelKjøringLoopSlackBot.regelkjøringLoop(any(), behandlingId.toString(), any()) }
    }

    private fun nyAvklaring(
        avklaringskode: String,
        behandlingId: UUID,
    ) = // language=JSON
        """
        {
            "@event_name": "NyAvklaring",
            "ident": "12345678910",
            "avklaringId": "${UUID.randomUUID()}",
            "kode": "$avklaringskode",
            "behandlingId": "$behandlingId",
            "gjelderDato": "2024-06-24",
            "søknadId": "${UUID.randomUUID()}",
            "søknad_uuid": "4afce924-6cb4-4ab4-a92b-fe91e24f31bf",
            "@id": "74a01063-4b78-4758-89d0-52d6b358b2e2",
            "@opprettet": "2024-06-24T09:59:53.107584"
           
        }
        """.trimIndent()
}
