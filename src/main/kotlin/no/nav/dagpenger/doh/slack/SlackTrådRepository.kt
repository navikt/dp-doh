package no.nav.dagpenger.doh.slack

import java.time.LocalDateTime

interface SlackTrådRepository {
    fun hentTråd(trådId: String): SlackTråd?

    fun lagreTråd(slackTråd: SlackTråd)
}

data class SlackTråd(val søknadId: String, val threadTs: String, val opprettet: LocalDateTime = LocalDateTime.now())
