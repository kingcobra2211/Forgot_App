package com.example.data.repository

object VersionComparator {
    /**
     * Compares two version strings.
     * Returns:
     *   - Negative if v1 < v2
     *   - Zero if v1 == v2
     *   - Positive if v1 > v2
     */
    fun compare(v1: String, v2: String): Int {
        val clean1 = cleanVersion(v1)
        val clean2 = cleanVersion(v2)

        val parts1 = clean1.split(".")
        val parts2 = clean2.split(".")

        val maxLength = maxOf(parts1.size, parts2.size)
        for (i in 0 until maxLength) {
            val num1 = parts1.getOrNull(i)?.toIntOrNull() ?: 0
            val num2 = parts2.getOrNull(i)?.toIntOrNull() ?: 0
            if (num1 != num2) {
                return num1.compareTo(num2)
            }
        }
        return 0
    }

    private fun cleanVersion(version: String): String {
        var clean = version.trim().lowercase()
        if (clean.startsWith("version")) {
            clean = clean.substring("version".length)
        } else if (clean.startsWith("v")) {
            clean = clean.substring(1)
        }
        clean = clean.trim()
        val dashIndex = clean.indexOf("-")
        if (dashIndex != -1) {
            clean = clean.substring(0, dashIndex)
        }
        return clean.trim()
    }
}
