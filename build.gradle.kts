import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent

plugins {
    application
    kotlin("jvm") version "1.9.0"
    id(Spotless.spotless) version "6.16.0"
}

repositories {
    mavenCentral()
    maven(url = "https://packages.confluent.io/maven/")
    maven("https://jitpack.io")
}

application {
    applicationName = "dp-doh"
    mainClass.set("no.nav.dagpenger.doh.AppKt")
}

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val slackClientVersion = "1.30.0"

dependencies {
    implementation(kotlin("stdlib"))

    implementation(RapidAndRiversKtor2)
    implementation(Konfig.konfig)
    implementation(Kotlin.Logging.kotlinLogging)
    implementation("com.bazaarvoice.jackson:rison:2.9.10.2")

    testImplementation("io.mockk:mockk:1.13.5")
    testImplementation(Junit5.api)
    testRuntimeOnly(Junit5.engine)

    implementation("com.slack.api:slack-api-client:$slackClientVersion")
    implementation("com.slack.api:slack-api-model-kotlin-extension:$slackClientVersion")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slackClientVersion")
}

spotless {
    kotlin {
        ktlint("0.48.2")
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint("0.48.2")
    }
}

tasks.withType<Jar>().configureEach {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes(mapOf("Main-Class" to application.mainClass.get()))
    }

    from(
        configurations.runtimeClasspath.get().map {
            if (it.isDirectory) it else zipTree(it)
        },
    )
}

tasks.named("compileKotlin") {
    dependsOn("spotlessCheck")
}

tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        showExceptions = true
        showStackTraces = true
        exceptionFormat = TestExceptionFormat.FULL
        events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        showStandardStreams = true
    }
}
