package dev.kingtux.tms.mixin.helpers


/**
 * Restricts a mixin to the Minecraft versions it was written against.
 *
 * Every listed range must match (they are AND-ed), so a middle band is a pair:
 * `{">26.2", "<26.3"}` applies on 26.2 up to but not including 26.3. `>` is inclusive and
 * `<` exclusive, so `<X` / `>X` partitions cleanly at X. An empty list applies everywhere.
 */
// Applied to mixin classes and read at runtime by MCVersionMixinPlugin via
// Class.getAnnotations(), so it must target the class declaration (CLASS), not a
// type-use (TYPE) — the latter is not returned by Class.getAnnotations().
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class MinecraftVersion(
    val minecraftVersions: Array<String> = []
)
