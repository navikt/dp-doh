package no.nav.dagpenger.doh.monitor

import io.prometheus.client.Counter
import io.prometheus.client.Histogram

object BehandlingMetrikker {
    val manuellCounter =
        Counter
            .build("dp_manuell_behandling", "Søknader som blir sendt til manuell behandling")
            .labelNames("grunn")
            .register()

    val resultatCounter =
        Counter
            .build("dp_prosessresultat", "Resultat av automatiseringsprosessen")
            .labelNames("resultat")
            .register()

    val behandlingStatusCounter =
        Counter
            .build("dp_behandling_status", "Søknader og status")
            .labelNames("status")
            .register()

    val behandlingVedtakCounter =
        Counter
            .build("dp_behandling_vedtak", "Behandlinger som fører til fattet vedtak")
            .labelNames("utfall", "automatisk")
            .register()

    val behandlingAvbruttCounter =
        Counter
            .build("dp_behandling_avbrutt", "Behandlinger som har blitt avbrutt")
            .labelNames("aarsak")
            .register()

    val tidBruktITilstand =
        Histogram
            .build("dp_behandling_tid_i_tilstand_sekund", "Antall sekund brukt i hver tilstand")
            .labelNames("forrigeTilstand", "gjeldendeTilstand")
            .buckets(
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
                10.dager,
                30.dager,
                60.dager,
            ).register()
}

private val Int.timer get() = (this * 60 * 60).toDouble()
private val Int.dager get() = (this * 60 * 60 * 24).toDouble()
