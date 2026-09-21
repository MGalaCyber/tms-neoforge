package dev.kingtux.tms.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.alternatives.AlternativeKeyMapping;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.IKeyBinding;
import dev.kingtux.tms.api.KeyModifier;
import dev.kingtux.tms.api.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TMS's own keybinds screen — it fully replaces vanilla's "Key Binds" screen (see
 * {@code ClientSetup#replaceKeyBindsScreen}) and adds per-binding Ctrl/Shift/Alt modifiers,
 * alternative binds, conflict highlighting, and filtering on top of it.
 *
 * <p>The rebind/reset/add/remove controls are real vanilla {@link Button} widgets (not
 * hand-drawn rectangles), positioned fresh every frame in each row's {@code render}, so
 * they pick up resource-pack retextures the same way vanilla's own Key Binds screen does.</p>
 *
 * <p>This file is the least-tested part of the port: {@link ObjectSelectionList} and
 * {@link AbstractSelectionList} constructor/method signatures have shifted between Minecraft
 * versions before. The list constructor call below matches what already compiled and ran
 * successfully in an earlier version of this file — if you change those numbers, that's the
 * part to double check first.</p>
 */
public final class TMSKeyBindsScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int BTN_W = 20;

    private final Screen parent;
    private EditBox searchBox;
    private KeyList list;
    private Button conflictsFilterButton;
    private Button unboundFilterButton;

    /** The binding currently waiting for the next key/mouse press, or null. */
    private KeyMapping listening;

    private boolean filterConflicts = false;
    private boolean filterUnbound = false;

    /** binding -> the other bindings it collides with (same key + same modifiers). Rebuilt on refresh. */
    private final Map<KeyMapping, List<KeyMapping>> conflicts = new IdentityHashMap<>();

    public TMSKeyBindsScreen(Screen parent) {
        super(Component.translatable("too_many_shortcuts.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        searchBox = new EditBox(font, width / 2 - 100, 24, 200, 16, Component.translatable("too_many_shortcuts.screen.search"));
        searchBox.setResponder(text -> refreshList());
        addRenderableWidget(searchBox);

        conflictsFilterButton = Button.builder(filterLabel("too_many_shortcuts.screen.filter.conflicts", filterConflicts),
                        b -> {
                            filterConflicts = !filterConflicts;
                            b.setMessage(filterLabel("too_many_shortcuts.screen.filter.conflicts", filterConflicts));
                            refreshList();
                        })
                .bounds(width / 2 - 154, 44, 150, 16)
                .build();
        addRenderableWidget(conflictsFilterButton);

        unboundFilterButton = Button.builder(filterLabel("too_many_shortcuts.screen.filter.unbound", filterUnbound),
                        b -> {
                            filterUnbound = !filterUnbound;
                            b.setMessage(filterLabel("too_many_shortcuts.screen.filter.unbound", filterUnbound));
                            refreshList();
                        })
                .bounds(width / 2 + 4, 44, 150, 16)
                .build();
        addRenderableWidget(unboundFilterButton);

        list = new KeyList(minecraft, width, height - 34, 64);
        addRenderableWidget(list);
        refreshList();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
                    minecraft.options.save();
                    minecraft.setScreen(parent);
                })
                .bounds(width / 2 - 100, height - 27, 200, 20)
                .build());
    }

    private Component filterLabel(String key, boolean on) {
        return Component.translatable(key, Component.translatable(on ? "too_many_shortcuts.screen.filter.on" : "too_many_shortcuts.screen.filter.off"));
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        super.render(g, mouseX, mouseY, partialTick);
        g.drawCenteredString(font, title, width / 2, 8, 0xFFFFFF);
    }

    @Override
    public void onClose() {
        minecraft.options.save();
        minecraft.setScreen(parent);
    }

    private void refreshList() {
        computeConflicts();
        list.rebuild(searchBox == null ? "" : searchBox.getValue());
    }

    /** Two bindings conflict when they're bound to the exact same key AND the exact same modifiers. */
    private void computeConflicts() {
        conflicts.clear();
        KeyMapping[] all = minecraft.options.keyMappings;
        for (int i = 0; i < all.length; i++) {
            IKeyBinding a = (IKeyBinding) all[i];
            if (a.tms$getBoundKey().equals(InputConstants.UNKNOWN)) continue;
            for (int j = i + 1; j < all.length; j++) {
                IKeyBinding b = (IKeyBinding) all[j];
                if (!b.tms$getBoundKey().equals(a.tms$getBoundKey())) continue;
                if (!b.tms$getKeyModifiers().equals(a.tms$getKeyModifiers())) continue;
                conflicts.computeIfAbsent(all[i], k -> new ArrayList<>()).add(all[j]);
                conflicts.computeIfAbsent(all[j], k -> new ArrayList<>()).add(all[i]);
            }
        }
    }

    private void startListening(KeyMapping binding) {
        this.listening = binding;
    }

    private void assign(KeyMapping binding, InputConstants.Key key) {
        BindingModifiers modifiers = TooManyShortcuts.currentModifiers().copy();
        // If they bound directly to a modifier key, don't also require that modifier.
        KeyModifier self = KeyModifier.fromKeyCode(key.getValue());
        if (self != null) modifiers.set(self, false);

        IKeyBinding tms = (IKeyBinding) binding;
        tms.tms$setBoundKey(key);
        tms.tms$setKeyModifiers(modifiers);
        KeyMapping.resetMapping();
        listening = null;
        refreshList();
    }

    /** "Ctrl + Shift + ..." style live preview shown on the row while listening. */
    private String listeningLabel() {
        BindingModifiers held = TooManyShortcuts.currentModifiers();
        StringBuilder sb = new StringBuilder();
        for (KeyModifier m : KeyModifier.VALUES) {
            if (held.isSet(m)) {
                sb.append(Component.translatable(m.translationKey()).getString()).append(" + ");
            }
        }
        sb.append("...");
        return sb.toString();
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                listening = null;
                return true;
            }
            // Ctrl/Shift/Alt alone don't finish the bind — they're held while a real
            // key is pressed. Without this check, holding Ctrl+Shift+K would finish
            // listening on the very first key (Ctrl) and bind to plain Left Ctrl,
            // which is why combos beyond one modifier never worked before.
            if (KeyModifier.fromKeyCode(keyCode) != null) {
                return true;
            }
            assign(listening, InputConstants.getKey(keyCode, scanCode));
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        boolean handled = super.mouseClicked(mouseX, mouseY, button);
        if (listening != null && !handled) {
            assign(listening, InputConstants.Type.MOUSE.getOrCreate(button));
            return true;
        }
        return handled;
    }

    // ------------------------------------------------------------------------

    private final class KeyList extends ObjectSelectionList<Row> {
        KeyList(Minecraft mc, int width, int height, int top) {
            super(mc, width, height, top, ROW_HEIGHT);
        }

        void rebuild(String filter) {
            clearEntries();
            Map<String, List<KeyMapping>> byCategory = Arrays.stream(minecraft.options.keyMappings)
                    .filter(k -> !((IKeyBinding) k).tms$isAlternative())
                    .collect(Collectors.groupingBy(KeyMapping::getCategory, LinkedHashMap::new, Collectors.toList()));

            for (var entry : byCategory.entrySet()) {
                List<KeyMapping> matching = entry.getValue().stream()
                        .filter(k -> Utils.entryKeyMatches(k, filter.isEmpty() ? null : filter)
                                || Utils.translatedTextEqualsIgnoreCase(k, filter))
                        .filter(k -> !filterConflicts || !conflicts.getOrDefault(k, List.of()).isEmpty())
                        .filter(k -> !filterUnbound || ((IKeyBinding) k).tms$getBoundKey().equals(InputConstants.UNKNOWN))
                        .toList();
                if (matching.isEmpty()) continue;

                addEntry(new CategoryRow(Component.translatable(entry.getKey())));
                for (KeyMapping binding : matching) {
                    addEntry(new KeyRow(binding, false));
                    List<KeyMapping> alts = ((IKeyBinding) binding).tms$getAlternatives();
                    if (alts != null) {
                        for (KeyMapping alt : alts) {
                            if (((IKeyBinding) alt).tms$getBoundKey().equals(InputConstants.UNKNOWN) && alt != listening) {
                                continue; // hide unbound alternatives that aren't mid-rebind
                            }
                            addEntry(new KeyRow(alt, true));
                        }
                    }
                }
            }
        }

        @Override
        public int getRowWidth() {
            return Math.min(400, width - 40);
        }
    }

    private abstract static class Row extends ObjectSelectionList.Entry<Row> {
    }

    private final class CategoryRow extends Row {
        private final Component title;

        CategoryRow(Component title) { this.title = title; }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            g.drawCenteredString(font, title, left + width / 2, top + height - 9 - 1, 0xFFFFFF);
        }

        @Override public Component getNarration() { return title; }
    }

    private final class KeyRow extends Row {
        private final KeyMapping binding;
        private final boolean isAlternative;
        private final Button keyButton;
        private final Button resetButton;
        private final Button addButton;
        private final Button removeButton;

        KeyRow(KeyMapping binding, boolean isAlternative) {
            this.binding = binding;
            this.isAlternative = isAlternative;

            this.keyButton = Button.builder(Component.empty(), b -> startListening(binding))
                    .bounds(0, 0, 60, ROW_HEIGHT - 2)
                    .build();

            if (!isAlternative) {
                this.resetButton = Button.builder(Component.literal("R"), b -> {
                            Utils.resetBinding(binding, false);
                            minecraft.options.save();
                            refreshList();
                        })
                        .bounds(0, 0, BTN_W, ROW_HEIGHT - 2)
                        .build();
                this.addButton = Button.builder(Component.literal("+"), b -> {
                            AlternativeKeyMapping alt = new AlternativeKeyMapping(binding);
                            startListening(alt);
                            refreshList();
                        })
                        .bounds(0, 0, BTN_W, ROW_HEIGHT - 2)
                        .build();
                this.removeButton = null;
            } else {
                this.resetButton = null;
                this.addButton = null;
                this.removeButton = Button.builder(Component.literal("x"), b -> {
                            IKeyBinding tms = (IKeyBinding) binding;
                            tms.tms$setBoundKey(InputConstants.UNKNOWN);
                            tms.tms$getKeyModifiers().unset();
                            if (listening == binding) listening = null;
                            minecraft.options.save();
                            refreshList();
                        })
                        .bounds(0, 0, BTN_W, ROW_HEIGHT - 2)
                        .build();
            }
        }

        private int extraButtonsWidth() { return BTN_W * (isAlternative ? 1 : 2) + 4; }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            // Label area scales with the row so long names (e.g. from big Create addons)
            // get truncated with "..." instead of drawing over the key button.
            int labelWidth = Math.max(90, (int) (width * 0.42));
            String label = isAlternative ? "  \u21B3 alt" : Component.translatable(binding.getName()).getString();
            if (font.width(label) > labelWidth - 4) {
                label = font.plainSubstrByWidth(label, labelWidth - 4 - font.width("...")) + "...";
            }
            List<KeyMapping> conflictList = conflicts.getOrDefault(binding, List.of());
            int labelColor = conflictList.isEmpty() ? 0xFFFFFF : 0xFF5555;
            g.drawString(font, label, left + 2, top + (height - 8) / 2, labelColor);

            int x = left + labelWidth;
            int keyBtnW = Math.max(40, width - labelWidth - extraButtonsWidth() - 6);

            boolean isListening = binding == listening;
            keyButton.setX(x);
            keyButton.setY(top + 1);
            keyButton.setWidth(keyBtnW);
            keyButton.setHeight(height - 2);
            Component keyText = isListening
                    ? Component.literal(listeningLabel())
                    : binding.getTranslatedKeyMessage();
            if (!conflictList.isEmpty()) {
                keyText = keyText.copy().withStyle(ChatFormatting.RED);
            }
            keyButton.setMessage(keyText);
            keyButton.render(g, mouseX, mouseY, partialTick);
            x += keyBtnW + 2;

            if (!isAlternative) {
                resetButton.active = !binding.isDefault();
                resetButton.setX(x); resetButton.setY(top + 1); resetButton.setWidth(BTN_W); resetButton.setHeight(height - 2);
                resetButton.render(g, mouseX, mouseY, partialTick);
                x += BTN_W + 2;

                addButton.setX(x); addButton.setY(top + 1); addButton.setWidth(BTN_W); addButton.setHeight(height - 2);
                addButton.render(g, mouseX, mouseY, partialTick);
            } else {
                removeButton.setX(x); removeButton.setY(top + 1); removeButton.setWidth(BTN_W); removeButton.setHeight(height - 2);
                removeButton.render(g, mouseX, mouseY, partialTick);
            }

            if (!conflictList.isEmpty() && keyButton.isHovered()) {
                List<Component> tooltip = new ArrayList<>();
                tooltip.add(Component.translatable("too_many_shortcuts.screen.conflict_title"));
                for (KeyMapping other : conflictList) {
                    tooltip.add(Component.translatable(other.getName()));
                }
                g.renderTooltip(font, tooltip, Optional.empty(), mouseX, mouseY);
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (keyButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (resetButton != null && resetButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (addButton != null && addButton.mouseClicked(mouseX, mouseY, button)) return true;
            if (removeButton != null && removeButton.mouseClicked(mouseX, mouseY, button)) return true;
            return false;
        }

        @Override
        public Component getNarration() {
            return Component.translatable(binding.getName()).append(": ").append(binding.getTranslatedKeyMessage());
        }
    }
}
