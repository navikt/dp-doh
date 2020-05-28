package no.nav.dagpenger.doh

import java.time.LocalDateTime
import javax.sql.DataSource
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using

internal class SlackThreadDao(private val dataSource: DataSource) {

        fun hentThreadTs(vedtaksperiodeId: String): Pair<String, LocalDateTime>? {
            return using(sessionOf(dataSource)) { session ->
                session.run(queryOf("SELECT thread_ts, opprettet FROM slack_thread WHERE vedtaksperiode_id = ? ORDER BY opprettet DESC LIMIT 1", vedtaksperiodeId).map {
                    it.string(1) to it.localDateTime(2)
                }.asSingle)
            }
        }

        fun lagreThreadTs(vedtaksperiodeId: String, threadTs: String) {
            using(sessionOf(dataSource)) { session ->
                session.run(
                    queryOf(
                        "INSERT INTO slack_thread (vedtaksperiode_id, thread_ts, opprettet) VALUES (?, ?, ?)",
                        vedtaksperiodeId,
                        threadTs,
                        LocalDateTime.now()
                    ).asExecute
                )
            }
        }
}
