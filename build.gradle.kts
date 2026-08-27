plugins {
    application
    id("common")
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

application {
    mainClass.set("no.nav.dagpenger.doh.AppKt")
}

val slackClientVersion = "1.50.0"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation("com.bazaarvoice.jackson:rison:2.9.10.2")
    // RisonFactory extends Jackson 2's JsonFactory (com.fasterxml.jackson.core), so OpenSearch
    // needs an explicit Jackson 2 ObjectMapper to work with it (library is not Jackson 3-compatible).
    implementation("com.fasterxml.jackson.core:jackson-databind:2.22.2")
    implementation("io.prometheus:prometheus-metrics-core:1.8.0")

    implementation("com.slack.api:slack-api-client:$slackClientVersion")
    implementation("com.slack.api:slack-api-model-kotlin-extension:$slackClientVersion")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slackClientVersion")

    testImplementation(libs.mockk)
    testImplementation(libs.kotest.assertions.core)
    testImplementation(libs.rapids.and.rivers.test)
}

tasks.withType<Jar>().configureEach {
    manifest { attributes["Main-Class"] = application.mainClass }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
