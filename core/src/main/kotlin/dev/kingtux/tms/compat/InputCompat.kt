package dev.kingtux.tms.compat

import com.mojang.blaze3d.platform.InputConstants
import com.mojang.blaze3d.platform.Window
import dev.kingtux.tms.TooManyShortcutsCore

/**
 * Cross-version access to Minecraft's input constants.
 *
 * Minecraft 26.3 replaced GLFW with SDL3, which renumbered every key code and mouse button and
 * renamed `InputConstants.Type.KEYSYM` to `KEYBOARD`. Because this mod ships a single jar for
 * several Minecraft versions, it cannot name those values at compile time:
 *
 *  - `InputConstants.KEY_*`, `MOUSE_BUTTON_*` and `MOD_*` are `static final int` with a
 *    ConstantValue attribute, so javac *inlines* them. A jar compiled against 26.3 would carry
 *    SDL numbers and hand them to a 26.1/26.2 game.
 *  - `InputConstants.Type.KEYSYM` compiles to a GETSTATIC, which throws `NoSuchFieldError` on a
 *    version where that constant is gone.
 *
 * Key *names* ("key.keyboard.escape") are stable across the change and are how both vanilla's
 * options.txt and this mod's config already persist bindings, so they are the currency used here:
 * [code] resolves a name against the running game every time it matters.
 *
 * Values are resolved lazily so this object stays loadable in a bare JVM (the `core` unit tests
 * run without Minecraft on the classpath).
 */
object InputCompat {
    /**
     * The keyboard input type: `KEYSYM` on 26.1/26.2, `KEYBOARD` on 26.3+.
     *
     * Matched by name over `values()` rather than referenced directly, since either constant is
     * absent on the other version.
     */
    @JvmStatic
    fun keyboardType(): InputConstants.Type = keyboard

    /** The mouse input type, unchanged across the SDL migration. */
    @JvmStatic
    fun mouseType(): InputConstants.Type = mouse

    /** The value of an unbound key: -1 under GLFW, 0 under SDL. */
    @JvmStatic
    fun unknownValue(): Int = InputConstants.UNKNOWN.value

    /**
     * The key code the running game uses for [name], e.g. "key.keyboard.escape".
     *
     * Falls back to [unknownValue] and logs, rather than throwing, so one unresolvable name
     * cannot take the game down.
     */
    @JvmStatic
    fun code(name: String): Int =
        runCatching { InputConstants.getKey(name).value }
            .onFailure {
                TooManyShortcutsCore.LOGGER.error(
                    "[${TooManyShortcutsCore.MOD_ID}] Could not resolve key name '$name'", it
                )
            }
            .getOrElse { unknownValue() }

    /** Escape, used to clear a pending binding and to synthesise a screen-closing key press. */
    @JvmStatic
    fun escape(): Int = escapeCode

    /** The default binding shipped for the toggle-auto-jump shortcut. */
    @JvmStatic
    fun autoJumpDefault(): Int = autoJumpCode

    /** The `action` value Minecraft passes for a key or button going down. */
    @JvmStatic
    fun pressAction(): Int = press

    /** The `action` value Minecraft passes for an auto-repeat while a key is held. */
    @JvmStatic
    fun repeatAction(): Int = repeat

    /**
     * Whether [keyCode] is currently held.
     *
     * 26.3 dropped the window handle from `InputConstants.isKeyDown` when SDL replaced GLFW, so
     * the legacy two-argument form is reached reflectively. Only the branch that runs is linked,
     * so the direct call never resolves on 26.1/26.2.
     */
    @JvmStatic
    fun isKeyDown(window: Window, keyCode: Int): Boolean {
        val legacy = legacyIsKeyDown
        return if (legacy != null) {
            legacy.invoke(null, window, keyCode) as Boolean
        } else {
            InputConstants.isKeyDown(keyCode)
        }
    }

    private val keyboard: InputConstants.Type by lazy(LazyThreadSafetyMode.PUBLICATION) {
        typeNamed("KEYBOARD", "KEYSYM")
            ?: error("No keyboard InputConstants.Type found on this Minecraft version")
    }

    private val mouse: InputConstants.Type by lazy(LazyThreadSafetyMode.PUBLICATION) {
        typeNamed("MOUSE")
            ?: error("No mouse InputConstants.Type found on this Minecraft version")
    }

    private val escapeCode: Int by lazy(LazyThreadSafetyMode.PUBLICATION) { code("key.keyboard.escape") }

    private val autoJumpCode: Int by lazy(LazyThreadSafetyMode.PUBLICATION) { code("key.keyboard.b") }

    /** Non-null on 26.1/26.2, where `isKeyDown` still takes the GLFW window handle. */
    private val legacyIsKeyDown: java.lang.reflect.Method? by lazy(LazyThreadSafetyMode.PUBLICATION) {
        runCatching {
            InputConstants::class.java.getMethod("isKeyDown", Window::class.java, Int::class.javaPrimitiveType)
        }.getOrNull()
    }

    private val press: Int by lazy(LazyThreadSafetyMode.PUBLICATION) { intConstant("PRESS", 1) }

    private val repeat: Int by lazy(LazyThreadSafetyMode.PUBLICATION) { intConstant("REPEAT", 2) }

    private fun typeNamed(vararg names: String): InputConstants.Type? =
        InputConstants.Type.values().firstOrNull { it.name in names }

    /**
     * Reads a `static final int` off the *running* [InputConstants].
     *
     * Naming one in source would let javac inline the compile-target's value; reflection reads
     * whatever the game actually has.
     */
    private fun intConstant(name: String, fallback: Int): Int =
        runCatching { InputConstants::class.java.getField(name).getInt(null) }
            .onFailure {
                TooManyShortcutsCore.LOGGER.warn(
                    "[${TooManyShortcutsCore.MOD_ID}] InputConstants.$name is missing; using $fallback", it
                )
            }
            .getOrDefault(fallback)
}
