package no.nav.helse.spammer

internal fun humanReadableTime(time: Long): String {
    return mutableListOf<String>().apply {
        addIfNotZero(time / 86400, "%d dag", "%d dager")
        addIfNotZero(time % 86400 / 3600, "%d time", "%d timer")
        addIfNotZero(time % 3600 / 60, "%d minutt", "%d minutter")
        addIfNotZero(time % 60, "%d sekund", "%d sekunder")
    }.let {
        when {
            it.isEmpty() -> "N/A"
            it.size == 1 -> it.joinToString()
            else -> it.subList(0, it.size - 1).joinToString() + " og ${it.last()}"
        }
    }
}

private fun MutableList<String>.addIfNotZero(value: Long, singular: String, plural: String) {
    if (value == 0L) return
    add(String.format(if (value == 1L) singular else plural, value))
}
