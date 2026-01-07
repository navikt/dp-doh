package no.nav.dagpenger.doh

import com.bazaarvoice.jackson.rison.RisonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

internal class OpenSearchTest {
    companion object {
        val risonMapper: ObjectMapper = ObjectMapper(RisonFactory())
    }

    @Test
    fun `skal lage rison json`() {
        OpenSearch
            .createUrl(
                query = """\"${UUID.randomUUID()}\"""",
                startTime = LocalDateTime.now(),
            ).also {
                assertDoesNotThrow("Ikke rison json kombatibel") {
                    risonMapper.readTree(it)
                }
            }

        OpenSearch
            .createUrl(
                query = """application: dp-speider""",
                startTime = LocalDateTime.now().minusMinutes(30),
            ).also {
                println(it)
            }
    }
}
