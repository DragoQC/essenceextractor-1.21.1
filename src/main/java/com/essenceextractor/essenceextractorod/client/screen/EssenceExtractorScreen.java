package com.essenceextractor.essenceextractormod.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.essenceextractor.essenceextractormod.menu.EssenceExtractorMenu;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;

public class EssenceExtractorScreen extends AbstractContainerScreen<EssenceExtractorMenu> {
    private static final int MOB_GRID_VISIBLE = 2;
    private static final int MOB_GRID_TOTAL = 20;
    private static final int PANEL_DEFAULT_LEFT_OFFSET = 88;
    private static final int PANEL_DEFAULT_TOP_OFFSET = 24;
    private static final int PANEL_WIDTH = 84;
    private static final int PANEL_HEIGHT = 206;
    private static final int PANEL_HEADER_HEIGHT = 12;
    private static final int TOGGLE_WIDTH = 10;
    private static final int TOGGLE_HEIGHT = 10;
    private static final int TOGGLE_X_OFFSET = 165;
    private static final int TOGGLE_Y_OFFSET = 1;
    private static final int ADJUST_BUTTON_WIDTH = 20;
    private static final int ADJUST_BUTTON_HEIGHT = 16;
    private static final int SHOW_BUTTON_WIDTH = 78;
    private static final int SHOW_BUTTON_HEIGHT = 16;
    private static final int[] ADJUST_ROW_Y_REL_OFFSETS = {16, 36, 56, 88, 108, 128, 160};
    private static final int TANK_X_OFFSET = 156;
    private static final int TANK_Y_OFFSET = 74;
    private static final int TANK_WIDTH = 12;
    private static final int TANK_HEIGHT = 80;
    private static final int TANK_RENDER_FILL_HEIGHT = 46;
    private final List<Button> configButtons = new ArrayList<>();
    private Button toggleConfigButton;
    private Button mobScrollUpButton;
    private Button mobScrollDownButton;
    private Button showAreaToggleButton;
    private boolean configOpen = false;
    private int mobScroll;
    private int panelOffsetX = PANEL_DEFAULT_LEFT_OFFSET;
    private int panelOffsetY = PANEL_DEFAULT_TOP_OFFSET;
    private boolean draggingPanel;
    private int panelDragOffsetX;
    private int panelDragOffsetY;

    public EssenceExtractorScreen(EssenceExtractorMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 232;
        this.inventoryLabelY = 138;
    }

    @Override
    protected void init() {
        super.init();

        this.toggleConfigButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            this.configOpen = !this.configOpen;
            updateConfigButtonsVisibility();
        }).bounds(this.leftPos + TOGGLE_X_OFFSET, this.topPos + TOGGLE_Y_OFFSET, TOGGLE_WIDTH, TOGGLE_HEIGHT).build());
        this.toggleConfigButton.setAlpha(0.0F);

        this.mobScrollUpButton = this.addRenderableWidget(Button.builder(Component.literal("^"), b -> scrollMobList(-1))
                .bounds(this.leftPos + 126, this.topPos + 34, 12, 10)
                .build());
        this.mobScrollDownButton = this.addRenderableWidget(Button.builder(Component.literal("v"), b -> scrollMobList(1))
                .bounds(this.leftPos + 126, this.topPos + 56, 12, 10)
                .build());

        addConfigButtons();
        layoutConfigControls();
        updateConfigButtonsVisibility();
        updateMobScrollButtons(0);
    }

    private void addConfigButtons() {
        this.configButtons.clear();

        addAdjustRow(0, 1);
        addAdjustRow(2, 3);
        addAdjustRow(4, 5);
        addAdjustRow(6, 7);
        addAdjustRow(8, 9);
        addAdjustRow(10, 11);
        addAdjustRow(14, 15);

        this.showAreaToggleButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> sendMenuButton(12))
                .bounds(0, 0, SHOW_BUTTON_WIDTH, SHOW_BUTTON_HEIGHT)
                .build());
        updateShowAreaButtonLabel();
        this.configButtons.add(this.showAreaToggleButton);
    }

    private void addAdjustRow(int decId, int incId) {
        Button minus = this.addRenderableWidget(Button.builder(Component.literal("-"), b -> sendMenuButton(decId)).bounds(0, 0, ADJUST_BUTTON_WIDTH, ADJUST_BUTTON_HEIGHT).build());
        Button plus = this.addRenderableWidget(Button.builder(Component.literal("+"), b -> sendMenuButton(incId)).bounds(0, 0, ADJUST_BUTTON_WIDTH, ADJUST_BUTTON_HEIGHT).build());
        this.configButtons.add(minus);
        this.configButtons.add(plus);
    }

    private void layoutConfigControls() {
        int panelX = getConfigPanelX();
        int panelY = getConfigPanelY();

        if (this.toggleConfigButton != null) {
            this.toggleConfigButton.setPosition(this.leftPos + TOGGLE_X_OFFSET, this.topPos + TOGGLE_Y_OFFSET);
        }

        int baseX = panelX + 12;
        for (int row = 0; row < ADJUST_ROW_Y_REL_OFFSETS.length; row++) {
            int buttonIndex = row * 2;
            int rowY = panelY + ADJUST_ROW_Y_REL_OFFSETS[row];
            this.configButtons.get(buttonIndex).setPosition(baseX, rowY);
            this.configButtons.get(buttonIndex + 1).setPosition(baseX + 46, rowY);
        }

        if (this.showAreaToggleButton != null) {
            this.showAreaToggleButton.setPosition(panelX + 4, panelY + PANEL_HEIGHT - SHOW_BUTTON_HEIGHT - 2);
        }
    }

    private int getPanelMinX() {
        return this.leftPos;
    }

    private int getPanelMaxX() {
        return this.leftPos + this.imageWidth - PANEL_WIDTH;
    }

    private int getPanelMinY() {
        return this.topPos;
    }

    private int getPanelMaxY() {
        return this.topPos + this.imageHeight - PANEL_HEIGHT;
    }

    private int getConfigPanelX() {
        int desired = this.leftPos + this.panelOffsetX;
        return Math.max(getPanelMinX(), Math.min(getPanelMaxX(), desired));
    }

    private int getConfigPanelY() {
        int desired = this.topPos + this.panelOffsetY;
        return Math.max(getPanelMinY(), Math.min(getPanelMaxY(), desired));
    }

    private void setConfigPanelPosition(int x, int y) {
        int clampedX = Math.max(getPanelMinX(), Math.min(getPanelMaxX(), x));
        int clampedY = Math.max(getPanelMinY(), Math.min(getPanelMaxY(), y));
        this.panelOffsetX = clampedX - this.leftPos;
        this.panelOffsetY = clampedY - this.topPos;
    }

    private boolean isMouseOverConfigPanelHeader(double mouseX, double mouseY) {
        if (!this.configOpen) {
            return false;
        }
        int panelX = getConfigPanelX();
        int panelY = getConfigPanelY();
        return mouseX >= panelX && mouseX < panelX + PANEL_WIDTH && mouseY >= panelY && mouseY < panelY + PANEL_HEADER_HEIGHT;
    }

    private boolean isMouseOverConfigPanel(double mouseX, double mouseY) {
        if (!this.configOpen) {
            return false;
        }
        int panelX1 = getConfigPanelX();
        int panelY1 = getConfigPanelY();
        int panelX2 = panelX1 + PANEL_WIDTH;
        int panelY2 = panelY1 + PANEL_HEIGHT;
        return mouseX >= panelX1 && mouseX < panelX2 && mouseY >= panelY1 && mouseY < panelY2;
    }

    private void sendMenuButton(int id) {
        if (this.minecraft == null || this.minecraft.gameMode == null || this.minecraft.player == null) {
            return;
        }

        // Always send to server; client-side BE state can be stale for validation-heavy actions.
        this.minecraft.gameMode.handleInventoryButtonClick(this.menu.containerId, id);
        if (id == 12) {
            updateShowAreaButtonLabel();
        }
    }

    private void updateShowAreaButtonLabel() {
        if (this.showAreaToggleButton != null) {
            this.showAreaToggleButton.setMessage(Component.literal("Box: " + (this.menu.isShowArea() ? "ON" : "OFF")));
        }
    }

    private void updateConfigButtonsVisibility() {
        for (Button button : this.configButtons) {
            button.visible = this.configOpen;
        }
    }

    private static boolean isMouseInMobGrid(int mouseX, int mouseY, int leftPos, int topPos) {
        return mouseX >= leftPos + 8 && mouseX <= leftPos + 137 && mouseY >= topPos + 42 && mouseY <= topPos + 64;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseInMobGrid((int) mouseX, (int) mouseY, this.leftPos, this.topPos)) {
            scrollMobList(scrollY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getMobDisplayCount() {
        int displayCount = 0;
        for (int i = 0; i < MOB_GRID_TOTAL; i++) {
            if (this.menu.getCapturedMobCount(i) > 0 || this.menu.getProcessingMobCount(i) > 0) {
                displayCount++;
            }
        }
        return displayCount;
    }

    private void scrollMobList(int delta) {
        int maxScroll = Math.max(0, getMobDisplayCount() - MOB_GRID_VISIBLE);
        this.mobScroll = Math.max(0, Math.min(maxScroll, this.mobScroll + delta));
        updateMobScrollButtons(maxScroll);
    }

    private void updateMobScrollButtons(int maxScroll) {
        if (this.mobScrollUpButton == null || this.mobScrollDownButton == null) {
            return;
        }
        boolean canScroll = maxScroll > 0;
        this.mobScrollUpButton.visible = canScroll;
        this.mobScrollDownButton.visible = canScroll;
        this.mobScrollUpButton.active = canScroll && this.mobScroll > 0;
        this.mobScrollDownButton.active = canScroll && this.mobScroll < maxScroll;
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        guiGraphics.fill(x, y, x + this.imageWidth, y + this.imageHeight, 0xFF4B4B4B);
        guiGraphics.fill(x + 1, y + 1, x + this.imageWidth - 1, y + this.imageHeight - 1, 0xFF6B6B6B);

        guiGraphics.fill(x + 6, y + 6, x + 138, y + 66, 0xFF1F1F1F);
        guiGraphics.fill(x + 7, y + 7, x + 137, y + 65, 0xFF2A2A2A);

        List<Integer> displaySlots = new ArrayList<>();
        for (int i = 0; i < MOB_GRID_TOTAL; i++) {
            if (this.menu.getCapturedMobCount(i) > 0) {
                displaySlots.add(i);
            }
        }
        int maxScroll = Math.max(0, displaySlots.size() - MOB_GRID_VISIBLE);
        this.mobScroll = Math.max(0, Math.min(this.mobScroll, maxScroll));
        updateMobScrollButtons(maxScroll);

        guiGraphics.drawString(this.font, Component.literal("Queued: " + this.menu.getTotalQueuedMobs()), x + 8, y + 10, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, Component.literal("Processing: " + this.menu.getTotalProcessingMobs()), x + 8, y + 20, 0xFFFFFFFF, false);
        guiGraphics.drawString(this.font, Component.literal("Output Buffer: " + this.menu.getOutputBufferItemCount() + " / 2048"), x + 8, y + 30, 0xFFE8C48C, false);
        if (displaySlots.isEmpty()) {
            guiGraphics.drawString(this.font, Component.literal("No mobs queued"), x + 8, y + 42, 0xFFFFFFFF, false);
        }
        for (int i = 0; i < MOB_GRID_VISIBLE; i++) {
            int index = this.mobScroll + i;
            if (index >= displaySlots.size()) {
                break;
            }

            int slot = displaySlots.get(index);
            int captured = this.menu.getCapturedMobCount(slot);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(this.menu.getCapturedMobTypeRawId(slot));
            String name = type == null ? "Unknown" : type.getDescription().getString();
            String text = String.format("%s: %d", name, captured);

            int lineY = y + 42 + i * 10;
            guiGraphics.drawString(this.font, Component.literal(text), x + 8, lineY, 0xFFFFFFFF, false);
        }
        if (displaySlots.size() > MOB_GRID_VISIBLE) {
            int trackX1 = x + 134;
            int trackY1 = y + 42;
            int trackX2 = x + 137;
            int trackY2 = y + 64;
            int trackHeight = trackY2 - trackY1;
            int handleHeight = Math.max(8, trackHeight * MOB_GRID_VISIBLE / displaySlots.size());
            int scrollRange = Math.max(1, displaySlots.size() - MOB_GRID_VISIBLE);
            int handleTravel = Math.max(0, trackHeight - handleHeight);
            int handleY = trackY1 + (this.mobScroll * handleTravel / scrollRange);

            guiGraphics.fill(trackX1, trackY1, trackX2, trackY2, 0xAA202020);
            guiGraphics.fill(trackX1, handleY, trackX2, handleY + handleHeight, 0xFFD0D0D0);
        }

        guiGraphics.fill(x + 6, y + 70, x + 152, y + 128, 0xFF5B5B5B);
        guiGraphics.fill(x + 154, y + 70, x + 170, y + 128, 0xFF3D3D3D);
        guiGraphics.fill(x + 6, y + 144, x + this.imageWidth - 6, y + this.imageHeight - 6, 0xFF555555);

        int sharpSlotX = x + 140;
        int sharpSlotY = y + 16;
        int lootSlotY = y + 46;
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(0.8F, 0.8F, 1.0F);
        guiGraphics.drawString(this.font, Component.literal("Sharp"), (int) ((x + 140) / 0.8F), (int) ((y + 6) / 0.8F), 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("Loot"), (int) ((x + 140) / 0.8F), (int) ((y + 36) / 0.8F), 0xFFEDEDED, false);
        guiGraphics.pose().popPose();
        guiGraphics.fill(sharpSlotX - 1, sharpSlotY - 1, sharpSlotX + 17, sharpSlotY + 17, 0xFF2B2B2B);
        guiGraphics.fill(sharpSlotX, sharpSlotY, sharpSlotX + 16, sharpSlotY + 16, 0xFF9A9A9A);
        guiGraphics.fill(sharpSlotX - 1, lootSlotY - 1, sharpSlotX + 17, lootSlotY + 17, 0xFF2B2B2B);
        guiGraphics.fill(sharpSlotX, lootSlotY, sharpSlotX + 16, lootSlotY + 16, 0xFF9A9A9A);

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 8; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = y + 73 + row * 18;
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2B2B2B);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF9A9A9A);
            }
        }

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slotX = x + 7 + col * 18;
                int slotY = y + 149 + row * 18;
                guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2B2B2B);
                guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF9A9A9A);
            }
        }

        for (int col = 0; col < 9; col++) {
            int slotX = x + 7 + col * 18;
            int slotY = y + 207;
            guiGraphics.fill(slotX, slotY, slotX + 18, slotY + 18, 0xFF2B2B2B);
            guiGraphics.fill(slotX + 1, slotY + 1, slotX + 17, slotY + 17, 0xFF9A9A9A);
        }

        for (int col = 0; col <= 9; col++) {
            int gx = x + 7 + col * 18;
            guiGraphics.fill(gx, y + 149, gx + 1, y + 225, 0x66444444);
        }
        for (int row = 0; row <= 3; row++) {
            int gy = y + 149 + row * 18;
            guiGraphics.fill(x + 7, gy, x + 169, gy + 1, 0x66444444);
        }
        for (int col = 0; col <= 9; col++) {
            int gx = x + 7 + col * 18;
            guiGraphics.fill(gx, y + 207, gx + 1, y + 225, 0x77444444);
        }
        guiGraphics.fill(x + 7, y + 207, x + 169, y + 208, 0x77444444);
        guiGraphics.fill(x + 7, y + 225, x + 169, y + 226, 0x77444444);

        int tankX1 = x + 156;
        int tankY1 = y + 74;
        int tankX2 = x + 168;
        int tankY2 = y + 126;
        guiGraphics.fill(tankX1, tankY1, tankX2, tankY2, 0xFF1E1E1E);
        guiGraphics.fill(tankX1 + 1, tankY1 + 1, tankX2 - 1, tankY2 - 1, 0xFF3C3C3C);

        int capacity = Math.max(this.menu.getFluidCapacity(), 1);
        int amount = Math.max(0, this.menu.getFluidAmount());
        int height = Math.min(TANK_RENDER_FILL_HEIGHT, amount * TANK_RENDER_FILL_HEIGHT / capacity);
        if (amount > 0 && height == 0) {
            height = 1;
        }
        if (height > 0) {
            guiGraphics.fill(tankX1 + 2, tankY2 - 2 - height, tankX2 - 2, tankY2 - 2, 0xFF6AFF2E);
        }
        // Brighter top strip for "liquid surface" effect.
        if (height > 0) {
            int surfaceY = tankY2 - 2 - height;
            guiGraphics.fill(tankX1 + 2, surfaceY, tankX2 - 2, Math.min(surfaceY + 1, tankY2 - 2), 0xFFB8FF7A);
        }
        // Marks every 4 buckets (32 bucket tank = 8 marks) to make fill easier to read.
        for (int i = 1; i < 8; i++) {
            int markY = tankY2 - 2 - (i * 50 / 8);
            guiGraphics.fill(tankX1 + 2, markY, tankX2 - 2, markY + 1, 0x774D4D4D);
        }

        int toggleX1 = x + TOGGLE_X_OFFSET;
        int toggleY1 = y + TOGGLE_Y_OFFSET;
        int toggleX2 = toggleX1 + TOGGLE_WIDTH;
        int toggleY2 = toggleY1 + TOGGLE_HEIGHT;
        guiGraphics.fill(toggleX1, toggleY1, toggleX2, toggleY2, 0xFFB8B8B8);
        guiGraphics.fill(toggleX1 + 1, toggleY1 + 1, toggleX2 - 1, toggleY2 - 1, this.configOpen ? 0xFFE7E7E7 : 0xFFD2D2D2);
    }

    private void renderConfigOverlay(GuiGraphics guiGraphics) {
        if (!this.configOpen) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);

        int panelX1 = getConfigPanelX();
        int panelY1 = getConfigPanelY();
        int panelX2 = panelX1 + PANEL_WIDTH;
        int panelY2 = panelY1 + PANEL_HEIGHT;
        guiGraphics.fill(panelX1, panelY1, panelX2, panelY2, 0xFF454545);
        guiGraphics.fill(panelX1 + 1, panelY1 + 1, panelX2 - 1, panelY2 - 1, 0xFF686868);
        guiGraphics.fill(panelX1 + 1, panelY1 + 1, panelX2 - 1, panelY1 + PANEL_HEADER_HEIGHT, 0xFF7A7A7A);

        int labelsX = panelX1 + 4;
        int valuesX = panelX1 + 40;
        guiGraphics.drawString(this.font, Component.literal("Area"), labelsX, panelY1 + 4, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("X:"), labelsX, panelY1 + 20, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("Y:"), labelsX, panelY1 + 40, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("Z:"), labelsX, panelY1 + 60, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getAreaX()), valuesX, panelY1 + 20, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getAreaY()), valuesX, panelY1 + 40, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getAreaZ()), valuesX, panelY1 + 60, 0xFFEDEDED, false);

        guiGraphics.drawString(this.font, Component.literal("Position"), labelsX, panelY1 + 76, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("X:"), labelsX, panelY1 + 92, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("Y:"), labelsX, panelY1 + 112, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("Z:"), labelsX, panelY1 + 132, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getPosX()), valuesX, panelY1 + 92, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getPosY()), valuesX, panelY1 + 112, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getPosZ()), valuesX, panelY1 + 132, 0xFFEDEDED, false);

        guiGraphics.drawString(this.font, Component.literal("Tick Rate"), labelsX, panelY1 + 148, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, Component.literal("T:"), labelsX, panelY1 + 164, 0xFFEDEDED, false);
        guiGraphics.drawString(this.font, String.valueOf(this.menu.getCaptureTickInterval()), valuesX, panelY1 + 164, 0xFFEDEDED, false);

        guiGraphics.pose().popPose();
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        layoutConfigControls();
        if (button == 0 && isMouseOverConfigPanelHeader(mouseX, mouseY)) {
            this.draggingPanel = true;
            this.panelDragOffsetX = (int) mouseX - getConfigPanelX();
            this.panelDragOffsetY = (int) mouseY - getConfigPanelY();
            return true;
        }
        if (isMouseOverConfigPanel(mouseX, mouseY)) {
            for (Button configButton : this.configButtons) {
                if (configButton.visible && configButton.isMouseOver(mouseX, mouseY)) {
                    return super.mouseClicked(mouseX, mouseY, button);
                }
            }
            return true;
        }
        if (isMouseOverTank((int) mouseX, (int) mouseY) && (button == 0 || button == 1)) {
            // Left click: extract XP into empty bucket. Right click: insert XP from XP bucket.
            sendMenuButton(button == 0 ? 13 : 18);
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        if (this.draggingPanel && button == 0) {
            setConfigPanelPosition((int) mouseX - this.panelDragOffsetX, (int) mouseY - this.panelDragOffsetY);
            layoutConfigControls();
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) {
            this.draggingPanel = false;
        }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0x2A1A0F, false);
    }

    private boolean isMouseOverTank(int mouseX, int mouseY) {
        int tankX1 = this.leftPos + TANK_X_OFFSET;
        int tankY1 = this.topPos + TANK_Y_OFFSET;
        int tankX2 = tankX1 + TANK_WIDTH;
        int tankY2 = tankY1 + TANK_HEIGHT;
        return mouseX >= tankX1 && mouseX < tankX2 && mouseY >= tankY1 && mouseY < tankY2;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics, mouseX, mouseY, partialTick);
        layoutConfigControls();
        updateShowAreaButtonLabel();
        boolean overConfigPanel = isMouseOverConfigPanel(mouseX, mouseY);
        for (Button button : this.configButtons) {
            button.visible = false;
        }
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderConfigOverlay(guiGraphics);
        if (this.configOpen) {
            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(0.0F, 0.0F, 1100.0F);
            for (Button button : this.configButtons) {
                button.visible = true;
                button.render(guiGraphics, mouseX, mouseY, partialTick);
            }
            guiGraphics.pose().popPose();
        }
        updateConfigButtonsVisibility();
        if (isMouseOverTank(mouseX, mouseY)) {
            guiGraphics.renderTooltip(this.font,
                    Component.literal(this.menu.getFluidAmount() + " / " + this.menu.getFluidCapacity() + " mB"),
                    mouseX,
                    mouseY);
        }
        if (!overConfigPanel) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

}
