package dev.kingtux.tms.api.modifiers

import dev.kingtux.tms.api.ModifierPrefixTextProvider
import dev.kingtux.tms.compat.InputCompat
import net.fabricmc.api.EnvType
import net.fabricmc.api.Environment
import com.mojang.blaze3d.platform.InputConstants
import net.minecraft.client.input.InputWithModifiers

/**
* the order of the enums makes a difference when generating the shown name in the gui
* with this order the old text order is preserved. But now the id values do not increment nicely. But changing them would eliminate
* backward compatibility with the old save format
*/
@Environment(EnvType.CLIENT)
enum class KeyModifier(
    val id: Int,
    private val leftKeyName: String,
    private val rightKeyName: String,
) {
    /**
     * Alt key modifier.
     */
    ALT(0, "key.keyboard.left.alt", "key.keyboard.right.alt"),

    /**
     * Shift key modifier.
     */
    SHIFT(2, "key.keyboard.left.shift", "key.keyboard.right.shift"),

    /**
     * Control key modifier.
     */
    CONTROL(1, "key.keyboard.left.control", "key.keyboard.right.control");

    /**
     * The name of the modifier, used for translation keys and display names.
     * This is the same as the enum name but in lowercase.
     */
    val textProvider: ModifierPrefixTextProvider =
        ModifierPrefixTextProvider(this)

    /**
     * The left and right key codes for this modifier, as the running Minecraft numbers them.
     *
     * Resolved from key names rather than written as literals: 26.3 moved from GLFW key codes to
     * SDL scancodes, so left shift is 340 on 26.1/26.2 and 225 on 26.3.
     */
    val keyCodes: IntArray by lazy(LazyThreadSafetyMode.PUBLICATION) {
        intArrayOf(InputCompat.code(leftKeyName), InputCompat.code(rightKeyName))
    }

    /**
     * Checks if the given keyCode matches this KeyModifier.
     */
    fun matches(keyCode: Int): Boolean {
        return keyCode != InputCompat.unknownValue() && keyCodes.contains(keyCode)
    }

    /**
     * Returns the translation key for this KeyModifier.
     * The translation key is in the format "tms.modifier.{name}" where {name} is the lowercase name of the modifier.
     */
    val translationKey: String
        get() = "tms.modifier." + name.lowercase()

    companion object {
        /**
         * Returns the modifiers held down during [input].
         *
         * Delegates the bitmask arithmetic to Minecraft's own [InputWithModifiers] defaults, so
         * the running game applies its own modifier bits — GLFW's on 26.1/26.2, SDL's on 26.3.
         */
        fun fromInput(input: InputWithModifiers): List<KeyModifier> {
            val result: MutableList<KeyModifier> = ArrayList()
            if (input.hasAltDown()) result.add(ALT)
            if (input.hasShiftDown()) result.add(SHIFT)
            if (input.hasControlDown()) result.add(CONTROL)
            return result
        }

        /**
         * Returns the KeyModifier for a given keyCode.
         * If no KeyModifier matches the keyCode, returns null.
         *
         * @param keyCode The key code to check.
         *
         * @return The KeyModifier that matches the keyCode, or null if none match.
         */
        fun fromKeyCode(keyCode: Int): KeyModifier? {
            for (keyModifier in entries) {
                if (keyModifier.matches(keyCode)) {
                    return keyModifier
                }
            }
            return null
        }

        /**
         * Checks if the given key is a key modifier (ALT, SHIFT, CONTROL).
         *
         * @param key The InputConstants.Key to check.
         *
         * @return True if the key is a key modifier, false otherwise.
         */
        fun isKeyModifier(key: InputConstants.Key?): Boolean {
            if (key == null || key.type != InputCompat.keyboardType()) {
                return false
            }
            for (keyModifier in entries) {
                if (keyModifier.matches(key.value)) {
                    return true
                }
            }
            return false
        }

        /**
         * Returns the KeyModifier for a given InputConstants.Key.
         *
         * If the key is null or not a key symbol, returns null.
         *
         * @param key The InputConstants.Key to check.
         * @return The KeyModifier that matches the key, or null if none match.
         */
        fun fromKey(key: InputConstants.Key?): KeyModifier? {
            if (key == null || key.type != InputCompat.keyboardType()) {
                return null
            }
            return fromKeyCode(key.value)
        }

        val modifierCount: Int
            get() = entries.size - 1 // remove 1 for NONE
    }
}
