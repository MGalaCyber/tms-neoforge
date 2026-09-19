/*
 * Copyright 2020-2023 Siphalor
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.siphalor.amecs.api;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.compat.InputCompat;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;

@SuppressWarnings("WeakerAccess")
@Environment(EnvType.CLIENT)
public enum KeyModifier {
    // the order of the enums makes a difference when generating the shown name in the gui
    // with this order the old text order is preserved. But now the id values do not increment nicely. But changing them would eliminate
    // backward compatibility with the old save format
    NONE("none", -1),
    ALT("alt", 0, "key.keyboard.left.alt", "key.keyboard.right.alt"),
    SHIFT("shift", 2, "key.keyboard.left.shift", "key.keyboard.right.shift"),
    CONTROL("control", 1, "key.keyboard.left.control", "key.keyboard.right.control");

    // using this array for the values because it is faster than calling values() every time
    public static final KeyModifier[] VALUES = KeyModifier.values();

    public final String name;
    public final int id;
    private final String[] keyNames;
    // Resolved from keyNames against the running game rather than hardcoded: 26.3 moved from
    // GLFW key codes to SDL scancodes, so the numbers differ per Minecraft version.
    private int[] keyCodes;

    KeyModifier(String name, int id, String... keyNames) {
        this.name = name;
        this.id = id;
        this.keyNames = keyNames;
    }

    private int[] keyCodes() {
        if (keyCodes == null) {
            int[] resolved = new int[keyNames.length];
            for (int i = 0; i < keyNames.length; i++) {
                resolved[i] = InputCompat.code(keyNames[i]);
            }
            keyCodes = resolved;
        }
        return keyCodes;
    }

    public static KeyModifier fromKeyCode(int keyCode) {
        for (KeyModifier keyModifier : VALUES) {
            if (keyModifier == NONE) {
                continue;
            }
            if (keyModifier.matches(keyCode)) {
                return keyModifier;
            }
        }
        return NONE;
    }

    public static KeyModifier fromKey(InputConstants.Key key) {
        if (key == null || key.getType() != InputCompat.keyboardType()) {
            return NONE;
        }
        return fromKeyCode(key.getValue());
    }

    public static int getModifierCount() {
        return VALUES.length - 1; // remove 1 for NONE
    }

    public boolean matches(int keyCode) {
        if (keyCode == InputCompat.unknownValue()) {
            return false;
        }
        for (int candidate : keyCodes()) {
            if (candidate == keyCode) {
                return true;
            }
        }
        return false;
    }
}
