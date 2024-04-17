package no.nav.dagpenger.doh.slack

class InMemorySlackTrådRepository : SlackTrådRepository {
    private val tråder = mutableMapOf<String, SlackTråd>()

    override fun hentTråd(søknadId: String): SlackTråd? = tråder[søknadId]

    override fun lagreTråd(slackTråd: SlackTråd) {
        tråder[slackTråd.søknadId] = slackTråd
    }
}
