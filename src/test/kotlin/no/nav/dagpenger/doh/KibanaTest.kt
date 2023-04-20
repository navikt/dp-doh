package no.nav.dagpenger.doh

import com.bazaarvoice.jackson.rison.RisonFactory
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

internal class KibanaTest {

    companion object {
        var RISON: ObjectMapper = ObjectMapper(RisonFactory())
    }

    @Test
    fun `skal lage rison json`() {
        Kibana.createUrl(
            query = """\"${UUID.randomUUID()}\"""",
            startTime = LocalDateTime.now(),
        ).also {
            assertDoesNotThrow("Ikke rison json kombatibel") {
                RISON.readTree(it)
            }
        }
    }
}
