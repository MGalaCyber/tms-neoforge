import dev.kingtux.tms.mixin.helpers.MinecraftVersionSupportRange
import dev.kingtux.tms.mixin.helpers.MinecraftVersionType
import dev.kingtux.tms.mixin.helpers.SupportMarker
import dev.kingtux.tms.mixin.helpers.supportsAll
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MinecraftVersionTesting {
    @Test
    fun parseMinecraftVersionTest() {
        assertEquals(
            MinecraftVersionType(1, 20, 4),
            MinecraftVersionType.parse("1.20.4")
        )
        assertEquals(
            MinecraftVersionType(1, 20, 5),
            MinecraftVersionType.parse("1.20.5")
        )
        assertEquals(
            MinecraftVersionType(1, 21, 0),
            MinecraftVersionType.parse("1.21.0")
        )
        assertEquals(
            MinecraftVersionType(1, 21, 0, 1),
            MinecraftVersionType.parse("1.21.0-beta.1")
        )
        assertEquals(
            MinecraftVersionType(1, 21, 6, 4),
            MinecraftVersionType.parse("1.21.6-beta.4")
        )
        // The pre-release suffix can hang off the minor component, and is not always "-beta".
        assertEquals(
            MinecraftVersionType(26, 3, 0, 1),
            MinecraftVersionType.parse("26.3-pre1")
        )
        assertEquals(
            MinecraftVersionType(26, 3, 0, 2),
            MinecraftVersionType.parse("26.3-rc2")
        )
        assertEquals(
            MinecraftVersionType(26, 3, 1, 2),
            MinecraftVersionType.parse("26.3.1-beta.2")
        )
    }

    @Test
    fun parseMinecraftVersionRange() {
        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.Equal,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse("=1.20.4")
        )
        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.Equal,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse("1.20.4")
        )
        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.GreaterThan,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse(">1.20.4")
        )
        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.LessThan,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse("<1.20.4")
        )

        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.NotEqual,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse("!=1.20.4")
        )

        // ">" is already inclusive, so ">=" is the same marker -- what matters is that the
        // two-character form is not stripped to "=1.20.4" and parsed as major 0.
        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.GreaterThan,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse(">=1.20.4")
        )
        assertEquals(
            MinecraftVersionSupportRange(
                SupportMarker.LessThanOrEqual,
                MinecraftVersionType(1, 20, 4)
            ),
            MinecraftVersionSupportRange.parse("<=1.20.4")
        )
    }

    @Test
    fun testSupports() {
        val supportsAllOneTwentyOne = MinecraftVersionSupportRange(
            SupportMarker.GreaterThan,
            MinecraftVersionType(1, 21, 4)
        )
        assertEquals(
            true,
            supportsAllOneTwentyOne.supports(MinecraftVersionType(1, 21, 4))
        )
        assertEquals(
            true,
            supportsAllOneTwentyOne.supports(MinecraftVersionType(1, 21, 5))
        )
        assertEquals(
            false,
            supportsAllOneTwentyOne.supports(MinecraftVersionType(1, 21, 3))
        )
    }

    @Test
    fun testDualVersionGate() {
        // The mixin gate partitions cleanly at 26.2: "<26.2" for the legacy (26.1.x)
        // variant and ">26.2" for the modern (26.2+) variant. They must not overlap.
        val legacy = MinecraftVersionSupportRange.parse("<26.2")
        val modern = MinecraftVersionSupportRange.parse(">26.2")

        // Versions are parsed the same way the plugin parses FabricLoader's reported
        // Minecraft version at runtime, so the patch component matches (parse fills an
        // absent patch with 0).

        // 26.1 and its patch releases -> legacy only
        for (v in listOf("26.1", "26.1.2").map { MinecraftVersionType.parse(it) }) {
            assertEquals(true, legacy.supports(v))
            assertEquals(false, modern.supports(v))
        }

        // 26.2 (and later) -> modern only, never legacy
        for (v in listOf("26.2", "26.3").map { MinecraftVersionType.parse(it) }) {
            assertEquals(false, legacy.supports(v))
            assertEquals(true, modern.supports(v))
        }
    }

    @Test
    fun testPreReleaseOrdering() {
        // patch defaults to 0 for every well-formed version, so an ordering that stops at the
        // patch component never reaches the pre-release and reports two betas as equal.
        assertTrue(MinecraftVersionType.parse("26.3-pre1") < MinecraftVersionType.parse("26.3"))
        assertTrue(MinecraftVersionType.parse("26.3-pre1") < MinecraftVersionType.parse("26.3-pre2"))
        assertTrue(
            MinecraftVersionType.parse("1.21.6-beta.4") < MinecraftVersionType.parse("1.21.6-beta.9")
        )
        assertTrue(MinecraftVersionType.parse("26.1.0-beta.1") < MinecraftVersionType.parse("26.1.0"))
    }

    @Test
    fun testPreReleaseSelectsItsOwnGeneration() {
        // 26.3-pre1 ships 26.3's API, so it belongs on the ">26.3" side of the gate even though
        // it orders below the 26.3 release.
        val legacy = MinecraftVersionSupportRange.parse("<26.3")
        val modern = MinecraftVersionSupportRange.parse(">26.3")
        for (v in listOf("26.3-pre1", "26.3-rc1").map { MinecraftVersionType.parse(it) }) {
            assertEquals(false, legacy.supports(v))
            assertEquals(true, modern.supports(v))
        }
    }

    @Test
    fun testSnapshotTreatedAsNewest() {
        // A snapshot has no parseable major, and the jar is compiled against the newest
        // supported Minecraft -- so the modern branch is the safer guess.
        val legacy = MinecraftVersionSupportRange.parse("<26.3")
        val modern = MinecraftVersionSupportRange.parse(">26.3")
        val snapshot = MinecraftVersionType.parse("26w03a")
        assertEquals(false, legacy.supports(snapshot))
        assertEquals(true, modern.supports(snapshot))
    }

    @Test
    fun testTripleVersionGate() {
        // Three API generations need a middle band, which only works if the ranges AND.
        val legacy = listOf("<26.2")
        val modern = listOf(">26.2", "<26.3")
        val sdl = listOf(">26.3")

        val expected = mapOf(
            "26.1" to legacy,
            "26.1.2" to legacy,
            "26.2" to modern,
            "26.2.1" to modern,
            "26.3" to sdl,
            "26.3.1" to sdl,
        )

        for ((version, matching) in expected) {
            val parsed = MinecraftVersionType.parse(version)
            for (gate in listOf(legacy, modern, sdl)) {
                val ranges = gate.map { MinecraftVersionSupportRange.parse(it) }
                assertEquals(
                    gate === matching,
                    supportsAll(ranges, parsed),
                    "$version against $gate"
                )
            }
        }
    }

    @Test
    fun testSupportsAllAndSemantics() {
        val ranges = listOf(">26.2", "<26.3").map { MinecraftVersionSupportRange.parse(it) }
        assertEquals(true, supportsAll(ranges, MinecraftVersionType.parse("26.2")))
        assertEquals(false, supportsAll(ranges, MinecraftVersionType.parse("26.1")))
        assertEquals(false, supportsAll(ranges, MinecraftVersionType.parse("26.3")))

        // No ranges means no constraint, matching how the plugin treats an unannotated mixin.
        assertEquals(true, supportsAll(emptyList(), MinecraftVersionType.parse("26.3")))
    }
}
