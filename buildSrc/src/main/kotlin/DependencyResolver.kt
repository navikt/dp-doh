import org.gradle.api.Action
import org.gradle.api.artifacts.DependencyResolveDetails

object DependencyResolver : Action<DependencyResolveDetails> {
    override fun execute(details: DependencyResolveDetails) {
        when (details.requested.group) {
            "org.jetbrains.kotlin" -> details.useVersion(Kotlin.version)
            "io.prometheus" -> details.useVersion(Prometheus.version)
            "io.ktor" -> details.useVersion(Ktor.version)
            "io.micrometer" -> details.useVersion(Micrometer.version)
            "org.apache.logging.log4j" -> details.useVersion(Log4j2.version)
            "com.zaxxer" -> details.useTarget(Database.HikariCP)
        }
    }
}
