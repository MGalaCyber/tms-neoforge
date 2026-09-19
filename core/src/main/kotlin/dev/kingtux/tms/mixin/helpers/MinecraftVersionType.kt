package dev.kingtux.tms.mixin.helpers

enum class SupportMarker {
    GreaterThan,
    LessThan,
    LessThanOrEqual,
    NotEqual,
    Equal
}

/**
 * Every range attached to a [MinecraftVersion] must match for the mixin to apply.
 *
 * The ranges are AND-ed, not OR-ed, so a middle band can be expressed as a pair:
 * `{">26.2", "<26.3"}` is "26.2 up to but not including 26.3". An empty list means
 * "no constraint", matching the plugin's treatment of an unannotated mixin.
 */
fun supportsAll(
    ranges: List<MinecraftVersionSupportRange>,
    minecraftVersion: MinecraftVersionType
): Boolean = ranges.all { it.supports(minecraftVersion) }

data class MinecraftVersionSupportRange(
    val marker: SupportMarker,
    val version: MinecraftVersionType
) {
    companion object {
        fun parse(version: String): MinecraftVersionSupportRange {
            var version = version.trim()
            // Two-character markers must be tested before their one-character prefixes,
            // or ">=26.1" strips to "=26.1" and parses as major 0 (matching everything).
            val marker = when {
                version.startsWith(">=") -> {
                    version = version.removePrefix(">=")
                    SupportMarker.GreaterThan
                }

                version.startsWith("<=") -> {
                    version = version.removePrefix("<=")
                    SupportMarker.LessThanOrEqual
                }

                version.startsWith("!=") -> {
                    version = version.removePrefix("!=")
                    SupportMarker.NotEqual
                }

                version.startsWith(">") -> {
                    version = version.removePrefix(">")
                    SupportMarker.GreaterThan
                }

                version.startsWith("<") -> {
                    version = version.removePrefix("<")
                    SupportMarker.LessThan
                }

                version.startsWith("=") -> {
                    version = version.removePrefix("=")
                    SupportMarker.Equal
                }

                else -> SupportMarker.Equal
            }
            return MinecraftVersionSupportRange(
                marker,
                version = MinecraftVersionType.parse(version)
            )
        }
    }

    fun supports(minecraftVersion: MinecraftVersionType): Boolean {
        // A pre-release ships the API of the release it leads up to — 26.3-pre1 is the 26.3
        // generation, not "just below 26.3" — so a range written without a pre-release
        // compares against the release identity. Write the pre-release into the range
        // (">26.3-beta.2") to discriminate between them.
        val candidate = if (version.preRelease == null) {
            minecraftVersion.copy(preRelease = null)
        } else {
            minecraftVersion
        }
        return when (marker) {
            // GreaterThan is inclusive (">=") while LessThan is exclusive ("<").
            // This lets a pair like ">26.2" / "<26.2" partition cleanly at 26.2:
            // ">26.2" matches 26.2 and up, "<26.2" matches everything below 26.2.
            SupportMarker.GreaterThan -> candidate >= version
            SupportMarker.LessThan -> candidate < version
            SupportMarker.LessThanOrEqual -> candidate <= version
            SupportMarker.NotEqual -> candidate != version
            SupportMarker.Equal -> candidate == version
        }
    }
}

data class MinecraftVersionType(
    val major: Int,
    val minor: Int,
    val patch: Int? = null,
    val preRelease: Int? = null,
) : Comparable<MinecraftVersionType> {
    companion object {
        /**
         * A version whose major component is not a number — a snapshot such as "26w03a".
         * Snapshots are treated as newer than every release: the jar is compiled against the
         * newest supported Minecraft, so the modern branch is far likelier to be correct on a
         * snapshot than the legacy one.
         */
        private val SNAPSHOT = MinecraftVersionType(Int.MAX_VALUE, Int.MAX_VALUE, Int.MAX_VALUE)

        fun parse(version: String): MinecraftVersionType {
            val trimmed = version.trim()
            // Split the pre-release suffix off first so it can hang off any component:
            // "26.3-pre1", "26.3-rc1" and "1.21.6-beta.4" all have to parse.
            val base = trimmed.substringBefore('-')
            val suffix = trimmed.substringAfter('-', "").lowercase()

            val parts = base.split(".")
            val major = parts.getOrNull(0)?.toIntOrNull() ?: return SNAPSHOT
            val minor = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val patch = parts.getOrNull(2)?.toIntOrNull() ?: 0
            val preRelease = if (suffix.isEmpty()) {
                null
            } else {
                // "beta.4" -> 4, "pre1" -> 1, a bare "beta" -> 0.
                suffix.takeLastWhile { it.isDigit() }.toIntOrNull() ?: 0
            }

            return MinecraftVersionType(major, minor, patch, preRelease)
        }
    }


    override fun compareTo(other: MinecraftVersionType): Int {
        if (major != other.major) return major - other.major
        if (minor != other.minor) return minor - other.minor

        val patchOrder = compareNullable(patch, other.patch)
        if (patchOrder != 0) return patchOrder

        // An absent pre-release is the finished release, which outranks every pre-release of it.
        return when {
            preRelease == other.preRelease -> 0
            preRelease == null -> 1
            other.preRelease == null -> -1
            else -> preRelease - other.preRelease
        }
    }

    private fun compareNullable(a: Int?, b: Int?): Int = when {
        a != null && b != null -> a - b
        a != null -> 1
        b != null -> -1
        else -> 0
    }
}
