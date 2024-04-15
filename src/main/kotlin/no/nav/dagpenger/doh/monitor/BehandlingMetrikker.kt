package no.nav.dagpenger.doh.monitor

import io.prometheus.client.Counter

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
}
