package com.smartattendance.app.core

/**
 * Normalizes a program name by trimming, collapsing internal whitespace, and lowercasing.
 */
fun normalizeProgramName(name: String): String =
    name.trim().replace(Regex("\\s+"), " ").lowercase()

/**
 * Normalizes a roll number for case-insensitive uniqueness checks while preserving display text.
 */
fun normalizeRollNumber(roll: String): String =
    roll.trim().lowercase()

/**
 * Natural alphanumeric comparator so "2" < "10" and "CS-02" < "CS-10".
 */
object NaturalRollComparator : Comparator<String> {
    private val chunkRegex = Regex("(\\d+|\\D+)")

    override fun compare(a: String, b: String): Int {
        val chunksA = chunkRegex.findAll(a.trim()).map { it.value }.toList()
        val chunksB = chunkRegex.findAll(b.trim()).map { it.value }.toList()
        val limit = minOf(chunksA.size, chunksB.size)

        for (i in 0 until limit) {
            val partA = chunksA[i]
            val partB = chunksB[i]
            val numA = partA.toBigIntegerOrNull()
            val numB = partB.toBigIntegerOrNull()

            val cmp = if (numA != null && numB != null) {
                val numCmp = numA.compareTo(numB)
                if (numCmp != 0) numCmp else partA.length.compareTo(partB.length)
            } else {
                partA.compareTo(partB, ignoreCase = true)
            }
            if (cmp != 0) return cmp
        }
        val sizeCmp = chunksA.size.compareTo(chunksB.size)
        return if (sizeCmp != 0) sizeCmp else a.compareTo(b, ignoreCase = true)
    }
}
