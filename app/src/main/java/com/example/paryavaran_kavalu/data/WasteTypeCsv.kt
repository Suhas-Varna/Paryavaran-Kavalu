package com.example.paryavaran_kavalu.data

/**
 * [ReportEntity.wasteType] stores one or more categories as comma-separated values (CSV),
 * e.g. `"Plastic"` or `"Plastic, Organic, Other"` — canonical order follows [WasteMenu.types],
 * then any extra tokens not in the menu (future-proofing).
 */
object WasteTypeCsv {

    const val SEPARATOR = ", "

    /**
     * Builds a normalized CSV string from selected category names (multi-select chips).
     */
    fun normalize(tokens: Iterable<String>): String {
        val raw = tokens.map { it.trim() }.filter { it.isNotEmpty() }
        if (raw.isEmpty()) return ""

        val knownOrdered = WasteMenu.types.filter { type ->
            raw.any { it.equals(type, ignoreCase = true) }
        }
        val menuLower = WasteMenu.types.map { it.lowercase() }.toSet()
        val extras = raw
            .filter { token -> token.lowercase() !in menuLower }
            .distinct()

        return (knownOrdered + extras).joinToString(SEPARATOR)
    }

    fun parseStored(value: String): List<String> =
        value.split(',').map { it.trim() }.filter { it.isNotEmpty() }

    /** True if [storedCsv] contains [category] as one of its comma-separated tokens. */
    fun containsCategory(storedCsv: String, category: String): Boolean {
        if (category.isBlank()) return false
        return parseStored(storedCsv).any { it.equals(category, ignoreCase = true) }
    }

    /** True if [storedCsv] contains every [category] in [categories] (multi-select AND filter). */
    fun containsAllCategories(storedCsv: String, categories: Iterable<String>): Boolean =
        categories.all { containsCategory(storedCsv, it) }

    /** UI display: compact separator between categories. */
    fun formatDisplay(storedCsv: String): String =
        parseStored(storedCsv).joinToString(" · ")

    /** Short line for map markers / tight layouts (truncates very long lists). */
    fun formatShort(storedCsv: String, maxLen: Int = 42): String {
        val s = formatDisplay(storedCsv)
        return if (s.length <= maxLen) s else s.take(maxLen - 1) + "…"
    }
}
