package no.nav.dagpenger.doh.monitor

import io.prometheus.metrics.core.metrics.Counter
import io.prometheus.metrics.core.metrics.Histogram

object BehandlingMetrikker {
    val manuellCounter =
        Counter
            .builder()
            .name("dp_manuell_behandling")
            .help("Søknader som blir sendt til manuell behandling")
            .labelNames("grunn")
            .register()

    val resultatCounter =
        Counter
            .builder()
            .name("dp_prosessresultat")
            .help("Resultat av automatiseringsprosessen")
            .labelNames("resultat")
            .register()

    val behandlingStatusCounter =
        Counter
            .builder()
            .name("dp_behandling_status")
            .help("Søknader og status")
            .labelNames("status")
            .register()

    val behandlingVedtakCounter =
        Counter
            .builder()
            .name("dp_behandling_vedtak")
            .help("Behandlinger som fører til fattet vedtak")
            .labelNames("utfall", "automatisk")
            .register()

    val behandlingAvbruttCounter =
        Counter
            .builder()
            .name("dp_behandling_avbrutt")
            .help("Behandlinger som har blitt avbrutt")
            .labelNames("aarsak")
            .register()

    val tidBruktITilstand =
        Histogram
            .builder()
            .name("dp_behandling_tid_i_tilstand_sekund")
            .help("Antall sekund brukt i hver tilstand")
            .labelNames("forrigeTilstand", "gjeldendeTilstand")
            .classicOnly() // TODO: denne tas bort når prometheus scraper native metrics
            .classicUpperBounds(
                1.0,
                5.0,
                10.0,
                30.0,
                60.0,
                // Halvtime:
                60.0 * 30,
                1.timer,
                2.timer,
                6.timer,
                12.timer,
                1.dager,
                2.dager,
                5.dager,
                1.uker,
                2.uker,
                4.uker,
            ).register()
}

private val Int.timer get() = (this * 60 * 60).toDouble()
private val Int.dager get() = (this.timer * 24)
private val Int.uker get() = (this * 7).dager
