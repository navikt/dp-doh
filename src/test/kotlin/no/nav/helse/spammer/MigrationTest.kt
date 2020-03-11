package no.nav.helse.spammer

import com.opentable.db.postgres.embedded.EmbeddedPostgres
import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.Connection

internal class MigrationTest {

    private lateinit var embeddedPostgres: EmbeddedPostgres
    private lateinit var postgresConnection: Connection

    private lateinit var hikariConfig: HikariConfig

    @BeforeEach
    fun `start postgres`() {
        embeddedPostgres = EmbeddedPostgres.builder()
            .start()

        postgresConnection = embeddedPostgres.postgresDatabase.connection

        hikariConfig = createHikariConfig(embeddedPostgres.getJdbcUrl("postgres", "postgres"))
    }

    private fun runMigration() =
        Flyway.configure()
            .dataSource(HikariDataSource(hikariConfig))
            .load()
            .migrate()

    private fun createHikariConfig(jdbcUrl: String) =
        HikariConfig().apply {
            this.jdbcUrl = jdbcUrl
            maximumPoolSize = 3
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }

    @AfterEach
    fun `stop postgres`() {
        postgresConnection.close()
        embeddedPostgres.close()
    }

    @Test
    fun `migreringer skal kjøre`() {
        val migrations = runMigration()
        assertTrue(migrations > 0, "Ingen migreringer ble kjørt")
    }
}
