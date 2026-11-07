package com.snaptube.clone.core.player

/**
 * Parses the `.pls` playlist format used by the reference app when exporting
 * playlists (e.g. "SnapTube All Videos.pls").
 */
object PlsPlaylistParser {

    data class Entry(val title: String, val path: String, val lengthSeconds: Int)

    fun parse(content: String): List<Entry> {
        val titles = mutableMapOf<Int, String>()
        val files = mutableMapOf<Int, String>()
        val lengths = mutableMapOf<Int, Int>()

        for (rawLine in content.lineSequence()) {
            val line = rawLine.trim()
            if (line.isEmpty() || line.startsWith("[")) continue
            val idx = line.indexOf('=')
            if (idx <= 0) continue
            val key = line.substring(0, idx)
            val value = line.substring(idx + 1)
            val number = key.dropWhile { !it.isDigit() }.toIntOrNull() ?: continue
            when {
                key.startsWith("File", ignoreCase = true) -> files[number] = value
                key.startsWith("Title", ignoreCase = true) -> titles[number] = value
                key.startsWith("Length", ignoreCase = true) -> lengths[number] = value.toIntOrNull() ?: -1
            }
        }

        return files.keys.sorted().mapNotNull { number ->
            val path = files[number] ?: return@mapNotNull null
            Entry(
                title = titles[number] ?: path.substringAfterLast('/'),
                path = path,
                lengthSeconds = lengths[number] ?: -1
            )
        }
    }
}
