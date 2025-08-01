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

val slackClientVersion = "1.45.3"

dependencies {
    implementation(kotlin("stdlib"))
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation("com.bazaarvoice.jackson:rison:2.9.10.2")
    implementation("io.prometheus:prometheus-metrics-core:1.3.10")

    implementation("com.slack.api:slack-api-client:$slackClientVersion")
    implementation("com.slack.api:slack-api-model-kotlin-extension:$slackClientVersion")
    implementation("com.slack.api:slack-api-client-kotlin-extension:$slackClientVersion")

    testImplementation(libs.mockk)
    testImplementation(libs.rapids.and.rivers.test)
}

tasks.withType<Jar>().configureEach {
    manifest { attributes["Main-Class"] = application.mainClass }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from(configurations.runtimeClasspath.get().map { if (it.isDirectory) it else zipTree(it) })
}
