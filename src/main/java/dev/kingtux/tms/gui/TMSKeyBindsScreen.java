package dev.kingtux.tms.gui;

import com.mojang.blaze3d.platform.InputConstants;
import dev.kingtux.tms.TooManyShortcuts;
import dev.kingtux.tms.alternatives.AlternativeKeyMapping;
import dev.kingtux.tms.api.BindingModifiers;
import dev.kingtux.tms.api.IKeyBinding;
import dev.kingtux.tms.api.KeyModifier;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSelectionList;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.ObjectSelectionList;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * TMS's own keybinds screen: per-binding Ctrl/Shift/Alt modifiers and "add an alternative
 * bind" support that the vanilla Controls screen has no room for. Reimplemented for NeoForge
 * against upstream's feature set (upstream's {@code TMSKeyBindsScreen} / {@code TMSKeyBindingEntry}
 * / {@code TMSCategoryEntry}, ~700 lines of Yarn-mapped Kotlin) — this version renders rows
 * manually with cached hit-boxes instead of nesting vanilla {@code Button} widgets inside list
 * entries, which is a smaller surface to get wrong when ported across mapping namespaces.
 *
 * <p>This file is the least-tested part of the port: {@link ObjectSelectionList} and
 * {@link AbstractSelectionList} constructor/method signatures have shifted between Minecraft
 * versions before, and this was written from documentation and the 1.21.1 source read alongside
 * this port, not compiled. If the build fails here, that's the first place to look.</p>
 */
public final class TMSKeyBindsScreen extends Screen {
    private static final int ROW_HEIGHT = 20;
    private static final int BTN_W = 16;

    private final Screen parent;
    private EditBox searchBox;
    private KeyList list;

    /** The binding currently waiting for the next key/mouse press, or null. */
    private KeyMapping listening;
    private String lastFilter = "";

    public TMSKeyBindsScreen(Screen parent) {
        super(Component.translatable("too_many_shortcuts.screen.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        searchBox = new EditBox(font, width / 2 - 100, 20, 200, 20, Component.translatable("too_many_shortcuts.screen.search"));
        searchBox.setResponder(text -> refreshList());
        addRenderableWidget(searchBox);

        list = new KeyList(minecraft, width, height - 90, 46);
        addRenderableWidget(list);
        refreshList();

        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> {
                    minecraft.options.save();
                    minecraft.setScreen(parent);
                })
                .bounds(width / 2 - 100, height - 27, 200, 20)
                .build());
    }

    @Override
    public void onClose() {
        minecraft.options.save();
        minecraft.setScreen(parent);
    }

    private void refreshList() {
        list.rebuild(searchBox == null ? "" : searchBox.getValue());
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

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (listening != null) {
            if (keyCode == InputConstants.KEY_ESCAPE) {
                listening = null;
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
            lastFilter = filter;
            Map<String, List<KeyMapping>> byCategory = Arrays.stream(minecraft.options.keyMappings)
                    .filter(k -> !((IKeyBinding) k).tms$isAlternative())
                    .collect(Collectors.groupingBy(KeyMapping::getCategory, LinkedHashMap::new, Collectors.toList()));

            for (var entry : byCategory.entrySet()) {
                List<KeyMapping> matching = entry.getValue().stream()
                        .filter(k -> dev.kingtux.tms.api.Utils.entryKeyMatches(k, filter.isEmpty() ? null : filter)
                                || dev.kingtux.tms.api.Utils.translatedTextEqualsIgnoreCase(k, filter))
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

        @Override public void updateNarration(NarrationElementOutput output) {}
    }

    private final class KeyRow extends Row {
        private final KeyMapping binding;
        private final boolean isAlternative;
        private int rowLeft, rowTop, rowWidth;

        KeyRow(KeyMapping binding, boolean isAlternative) {
            this.binding = binding;
            this.isAlternative = isAlternative;
        }

        @Override
        public void render(GuiGraphics g, int index, int top, int left, int width, int height, int mouseX, int mouseY, boolean hovering, float partialTick) {
            rowLeft = left; rowTop = top; rowWidth = width;

            String label = isAlternative ? "  \u21B3 alt" : Component.translatable(binding.getName()).getString();
            g.drawString(font, label, left + 2, top + (height - 8) / 2, 0xFFFFFF);

            int keyBtnW = width - extraButtonsWidth() - 90;
            int x = left + 90;

            boolean isListening = binding == listening;
            String keyText = isListening ? "> ? <" : binding.getTranslatedKeyMessage().getString();
            int bg = isListening ? 0xFF808000 : (hitKey(mouseX, mouseY) ? 0xFF606060 : 0xFF404040);
            g.fill(x, top + 1, x + keyBtnW, top + height - 1, bg);
            g.drawCenteredString(font, keyText, x + keyBtnW / 2, top + (height - 8) / 2, 0xFFFFFF);
            x += keyBtnW + 2;

            if (!isAlternative) {
                drawSmallButton(g, x, top, height, "R", hitReset(mouseX, mouseY));
                x += BTN_W + 2;
                drawSmallButton(g, x, top, height, "+", hitAdd(mouseX, mouseY));
            } else {
                drawSmallButton(g, x, top, height, "x", hitRemove(mouseX, mouseY));
            }
        }

        private int extraButtonsWidth() { return BTN_W * (isAlternative ? 1 : 2) + 4; }

        private void drawSmallButton(GuiGraphics g, int x, int top, int height, String label, boolean hovered) {
            g.fill(x, top + 1, x + BTN_W, top + height - 1, hovered ? 0xFF707070 : 0xFF505050);
            g.drawCenteredString(font, label, x + BTN_W / 2, top + (height - 8) / 2, 0xFFFFFF);
        }

        private int keyBtnRight() { return rowLeft + rowWidth - extraButtonsWidth(); }

        private boolean hitKey(int mx, int my) {
            return mx >= rowLeft + 90 && mx < keyBtnRight() && my >= rowTop + 1 && my < rowTop + ROW_HEIGHT - 1;
        }

        private boolean hitReset(int mx, int my) {
            int x = keyBtnRight() + 2;
            return !isAlternative && mx >= x && mx < x + BTN_W && my >= rowTop + 1 && my < rowTop + ROW_HEIGHT - 1;
        }

        private boolean hitAdd(int mx, int my) {
            int x = keyBtnRight() + 2 + BTN_W + 2;
            return !isAlternative && mx >= x && mx < x + BTN_W && my >= rowTop + 1 && my < rowTop + ROW_HEIGHT - 1;
        }

        private boolean hitRemove(int mx, int my) {
            int x = keyBtnRight() + 2;
            return isAlternative && mx >= x && mx < x + BTN_W && my >= rowTop + 1 && my < rowTop + ROW_HEIGHT - 1;
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            int mx = (int) mouseX, my = (int) mouseY;
            if (hitKey(mx, my)) {
                startListening(binding);
                return true;
            }
            if (hitReset(mx, my)) {
                dev.kingtux.tms.api.Utils.resetBinding(binding, false);
                minecraft.options.save();
                refreshList();
                return true;
            }
            if (hitAdd(mx, my)) {
                AlternativeKeyMapping alt = new AlternativeKeyMapping(binding);
                startListening(alt);
                refreshList();
                return true;
            }
            if (hitRemove(mx, my)) {
                IKeyBinding tms = (IKeyBinding) binding;
                tms.tms$setBoundKey(InputConstants.UNKNOWN);
                tms.tms$getKeyModifiers().unset();
                if (listening == binding) listening = null;
                minecraft.options.save();
                refreshList();
                return true;
            }
            return false;
        }

        @Override public void updateNarration(NarrationElementOutput output) {}
    }
}
