package com.essenceextractor.essenceextractormod.client.screen;

import java.util.ArrayList;
import java.util.List;

import com.essenceextractor.essenceextractormod.EssenceExtractor;
import com.essenceextractor.essenceextractormod.menu.EssenceExtractorMenu;
import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.entity.player.Inventory;

/**
 * Custom machine screen with:
 * - top mob-queue summary
 * - machine + player inventories
 * - XP and RF vertical bars
 * - draggable config overlay
 */
public class EssenceExtractorScreen extends AbstractContainerScreen<EssenceExtractorMenu> {
    private static final int COLOR_WHITE = 0xFFFFFFFF;
    private static final int COLOR_ACCENT = 0xFFE8C48C;
    private static final int COLOR_DARK_TEXT = 0xFF2A1A0F;
    private static final int COLOR_CONFIG_TEXT = 0xFF1A1A1A;
    private static final int COLOR_PROGRESS_BG = 0xFF1D1D1D;
    private static final int COLOR_PROGRESS_FILL = 0xFFF2C94C;
    private static final ResourceLocation MAIN_UI_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/ui/essenceextractor_ui_main.png");
    private static final ResourceLocation CONFIG_PANEL_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/ui/essenceextractor_ui_config.png");
    private static final ResourceLocation LEFT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/ui/essenceextractor_ui_btn_left.png");
    private static final ResourceLocation LEFT_BUTTON_PRESSED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/ui/essenceextractor_ui_btn_left_pressed.png");
    private static final ResourceLocation RIGHT_BUTTON_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/ui/essenceextractor_ui_btn_right.png");
    private static final ResourceLocation RIGHT_BUTTON_PRESSED_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/ui/essenceextractor_ui_btn_right_pressed.png");
    private static final ResourceLocation FLOWING_TANK_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/block/experience_flow.png");
    private static final int MAIN_UI_TEXTURE_WIDTH = 256;
    private static final int MAIN_UI_TEXTURE_HEIGHT = 256;
    private static final int CONFIG_PANEL_TEXTURE_WIDTH = 256;
    private static final int CONFIG_PANEL_TEXTURE_HEIGHT = 256;
    private static final int FLOW_TEXTURE_WIDTH = 32;
    private static final int FLOW_TEXTURE_HEIGHT = 1024;
    private static final int MOB_LIST_VISIBLE_ROWS = 2;
    private static final int MOB_LIST_TRACKED_TYPES = 20;
    private static final int PANEL_DEFAULT_LEFT_OFFSET = 88;
    private static final int PANEL_DEFAULT_TOP_OFFSET = 24;
    private static final int PANEL_WIDTH = 70;
    private static final int PANEL_HEIGHT = 155;
    private static final int PANEL_BORDER = 3;
    private static final int PANEL_HEADER_HEIGHT = 11;
    private static final int CONFIG_TOGGLE_WIDTH = 5;
    private static final int CONFIG_TOGGLE_HEIGHT = 5;
    private static final int CONFIG_TOGGLE_X_OFFSET = 170;
    private static final int CONFIG_TOGGLE_Y_OFFSET = 1;
    private static final int ADJUST_BUTTON_WIDTH = 12;
    private static final int ADJUST_BUTTON_HEIGHT = 11;
    private static final int SHOW_BUTTON_WIDTH = PANEL_WIDTH - (PANEL_BORDER * 2);
    private static final int SHOW_BUTTON_HEIGHT = 13;
    private static final int ADJUST_LEFT_BUTTON_X_OFFSET = 15;
    private static final int ADJUST_RIGHT_BUTTON_X_OFFSET = 49;
    private static final int[] ADJUST_ROW_Y_REL_OFFSETS = {14, 29, 44, 68, 83, 98, 122};
    private static final float CONFIG_TEXT_SCALE = 0.75F;
    private static final int CONFIG_LABELS_X_OFFSET = 6;
    private static final int CONFIG_VALUES_CENTER_X_OFFSET = 38;
    private static final int CONFIG_AREA_HEADER_Y_OFFSET = 5;
    private static final int CONFIG_AREA_X_Y_OFFSET = 17;
    private static final int CONFIG_AREA_Y_Y_OFFSET = 32;
    private static final int CONFIG_AREA_Z_Y_OFFSET = 47;
    private static final int CONFIG_POSITION_HEADER_Y_OFFSET = 59;
    private static final int CONFIG_POS_X_Y_OFFSET = 71;
    private static final int CONFIG_POS_Y_Y_OFFSET = 86;
    private static final int CONFIG_POS_Z_Y_OFFSET = 101;
    private static final int CONFIG_TICK_RATE_HEADER_Y_OFFSET = 113;
    private static final int CONFIG_TICK_RATE_VALUE_Y_OFFSET = 125;
    private static final int ENERGY_TANK_X_OFFSET = 134;
    private static final int XP_TANK_X_OFFSET = ENERGY_TANK_X_OFFSET + 17;
    private static final int XP_TANK_Y_OFFSET = 70;
    private static final int XP_TANK_WIDTH = 17;
    private static final int XP_TANK_HEIGHT = 52;
    private static final int ENERGY_TANK_Y_OFFSET = 70;
    private static final int ENERGY_TANK_WIDTH = 17;
    private static final int ENERGY_TANK_HEIGHT = 52;
    private static final int INFO_PROGRESS_X_OFFSET = 8;
    private static final int INFO_PROGRESS_Y_OFFSET = 58;
    private static final int INFO_PROGRESS_WIDTH = 106;
    private static final int INFO_PROGRESS_HEIGHT = 2;
    private static final float INVENTORY_TEXT_SCALE = 0.75F;
    private static final float STATUS_TEXT_SCALE = 0.75F;
    private static final int STATUS_TEXT_X_OFFSET = 8;
    private static final int STATUS_TEXT_START_Y_OFFSET = 12;
    private static final int STATUS_TEXT_LINE_HEIGHT = 10;
    private static final int STATUS_LIST_START_Y_OFFSET = 42;
    private static final int STATUS_LIST_LINE_HEIGHT = 10;
    private static final int MOB_LIST_X_OFFSET = 8;
    private static final int MOB_LIST_Y_OFFSET = 42;
    private static final int MOB_LIST_WIDTH = INFO_PROGRESS_WIDTH;
    private static final int MOB_LIST_HEIGHT = 22;
    private final List<Button> configButtons = new ArrayList<>();
    private Button toggleConfigButton;
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
        this.inventoryLabelY = 124;
    }

    @Override
    protected void init() {
        super.init();

        this.toggleConfigButton = this.addRenderableWidget(Button.builder(Component.empty(), b -> {
            this.configOpen = !this.configOpen;
            updateConfigButtonsVisibility();
        }).bounds(this.leftPos + CONFIG_TOGGLE_X_OFFSET, this.topPos + CONFIG_TOGGLE_Y_OFFSET, CONFIG_TOGGLE_WIDTH, CONFIG_TOGGLE_HEIGHT).build());
        this.toggleConfigButton.setAlpha(0.0F);

        addConfigButtons();
        layoutConfigControls();
        updateConfigButtonsVisibility();
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
        Button minus = this.addRenderableWidget(new TextureButton(
                0,
                0,
                ADJUST_BUTTON_WIDTH,
                ADJUST_BUTTON_HEIGHT,
                LEFT_BUTTON_TEXTURE,
                LEFT_BUTTON_PRESSED_TEXTURE,
                b -> sendMenuButton(decId)));
        Button plus = this.addRenderableWidget(new TextureButton(
                0,
                0,
                ADJUST_BUTTON_WIDTH,
                ADJUST_BUTTON_HEIGHT,
                RIGHT_BUTTON_TEXTURE,
                RIGHT_BUTTON_PRESSED_TEXTURE,
                b -> sendMenuButton(incId)));
        this.configButtons.add(minus);
        this.configButtons.add(plus);
    }

    private void layoutConfigControls() {
        int panelX = getConfigPanelX();
        int panelY = getConfigPanelY();

        if (this.toggleConfigButton != null) {
            this.toggleConfigButton.setPosition(this.leftPos + CONFIG_TOGGLE_X_OFFSET, this.topPos + CONFIG_TOGGLE_Y_OFFSET);
        }

        for (int row = 0; row < ADJUST_ROW_Y_REL_OFFSETS.length; row++) {
            int buttonIndex = row * 2;
            int rowY = panelY + ADJUST_ROW_Y_REL_OFFSETS[row];
            this.configButtons.get(buttonIndex).setPosition(panelX + ADJUST_LEFT_BUTTON_X_OFFSET, rowY);
            this.configButtons.get(buttonIndex + 1).setPosition(panelX + ADJUST_RIGHT_BUTTON_X_OFFSET, rowY);
        }

        if (this.showAreaToggleButton != null) {
            this.showAreaToggleButton.setPosition(panelX + PANEL_BORDER, panelY + PANEL_HEIGHT - SHOW_BUTTON_HEIGHT - PANEL_BORDER);
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

    private static boolean isMouseOverMobList(int mouseX, int mouseY, int leftPos, int topPos) {
        int x1 = leftPos + MOB_LIST_X_OFFSET;
        int y1 = topPos + MOB_LIST_Y_OFFSET;
        int x2 = x1 + MOB_LIST_WIDTH;
        int y2 = y1 + MOB_LIST_HEIGHT;
        return mouseX >= x1 && mouseX < x2 && mouseY >= y1 && mouseY < y2;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (isMouseOverMobList((int) mouseX, (int) mouseY, this.leftPos, this.topPos)) {
            scrollMobQueueList(scrollY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private int getVisibleMobEntryCount() {
        int displayCount = 0;
        for (int i = 0; i < MOB_LIST_TRACKED_TYPES; i++) {
            if (this.menu.getCapturedMobCount(i) > 0 || this.menu.getProcessingMobCount(i) > 0) {
                displayCount++;
            }
        }
        return displayCount;
    }

    private void scrollMobQueueList(int delta) {
        int maxScroll = Math.max(0, getVisibleMobEntryCount() - MOB_LIST_VISIBLE_ROWS);
        this.mobScroll = Math.max(0, Math.min(maxScroll, this.mobScroll + delta));
    }

    private int computeTankFillPixels(int storedAmount, int capacity, int renderHeight) {
        int safeCapacity = Math.max(capacity, 1);
        int safeAmount = Math.max(0, storedAmount);
        int renderedFill = Math.min(renderHeight, safeAmount * renderHeight / safeCapacity);
        if (safeAmount > 0 && renderedFill == 0) {
            return 1;
        }
        return renderedFill;
    }

    private void drawTintedFlowTank(GuiGraphics guiGraphics, int x, int y, int width, int height, int filledPixels, int scrollPixels, float r, float g, float b) {
        if (filledPixels <= 0) {
            return;
        }

        int phase = Math.floorMod(-scrollPixels, FLOW_TEXTURE_HEIGHT);
        int fillTop = y + (height - filledPixels);

        guiGraphics.enableScissor(x, fillTop, x + width, y + height);
        RenderSystem.setShaderColor(r, g, b, 0.98F);

        int drawn = 0;
        while (drawn < filledPixels) {
            int drawY = fillTop + drawn;
            int drawHeight = Math.min(16, filledPixels - drawn);
            int sourceV = Math.floorMod(phase + drawn, FLOW_TEXTURE_HEIGHT);

            if (sourceV + drawHeight <= FLOW_TEXTURE_HEIGHT) {
                guiGraphics.blit(FLOWING_TANK_TEXTURE, x, drawY, 0, sourceV, width, drawHeight, FLOW_TEXTURE_WIDTH, FLOW_TEXTURE_HEIGHT);
            } else {
                int firstHeight = FLOW_TEXTURE_HEIGHT - sourceV;
                guiGraphics.blit(FLOWING_TANK_TEXTURE, x, drawY, 0, sourceV, width, firstHeight, FLOW_TEXTURE_WIDTH, FLOW_TEXTURE_HEIGHT);
                guiGraphics.blit(FLOWING_TANK_TEXTURE, x, drawY + firstHeight, 0, 0, width, drawHeight - firstHeight, FLOW_TEXTURE_WIDTH, FLOW_TEXTURE_HEIGHT);
            }
            drawn += drawHeight;
        }

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        guiGraphics.disableScissor();
    }

    private void drawScaledStatusText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(STATUS_TEXT_SCALE, STATUS_TEXT_SCALE, 1.0F);
        int scaledX = Math.round(x / STATUS_TEXT_SCALE);
        int scaledY = Math.round(y / STATUS_TEXT_SCALE);
        guiGraphics.drawString(this.font, Component.literal(text), scaledX, scaledY, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawScaledConfigText(GuiGraphics guiGraphics, String text, int x, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(CONFIG_TEXT_SCALE, CONFIG_TEXT_SCALE, 1.0F);
        int scaledX = Math.round(x / CONFIG_TEXT_SCALE);
        int scaledY = Math.round(y / CONFIG_TEXT_SCALE);
        guiGraphics.drawString(this.font, Component.literal(text), scaledX, scaledY, color, false);
        guiGraphics.pose().popPose();
    }

    private void drawScaledCenteredConfigText(GuiGraphics guiGraphics, String text, int centerX, int y, int color) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(CONFIG_TEXT_SCALE, CONFIG_TEXT_SCALE, 1.0F);
        int scaledCenterX = Math.round(centerX / CONFIG_TEXT_SCALE);
        int scaledY = Math.round(y / CONFIG_TEXT_SCALE);
        int textX = scaledCenterX - (this.font.width(text) / 2);
        guiGraphics.drawString(this.font, Component.literal(text), textX, scaledY, color, false);
        guiGraphics.pose().popPose();
    }

    private List<Integer> collectQueuedMobSlots() {
        List<Integer> queuedSlots = new ArrayList<>();
        for (int slot = 0; slot < MOB_LIST_TRACKED_TYPES; slot++) {
            if (this.menu.getCapturedMobCount(slot) > 0) {
                queuedSlots.add(slot);
            }
        }
        return queuedSlots;
    }

    private void drawProcessingProgressBar(GuiGraphics guiGraphics, int guiLeft, int guiTop) {
        int progressPercent = Math.max(0, Math.min(100, this.menu.getProcessingProgressPercent()));
        int progressFillWidth = INFO_PROGRESS_WIDTH * progressPercent / 100;
        int progressX = guiLeft + INFO_PROGRESS_X_OFFSET;
        int progressY = guiTop + INFO_PROGRESS_Y_OFFSET;
        guiGraphics.fill(progressX, progressY, progressX + INFO_PROGRESS_WIDTH, progressY + INFO_PROGRESS_HEIGHT, COLOR_PROGRESS_BG);
        if (progressFillWidth > 0) {
            guiGraphics.fill(progressX, progressY, progressX + progressFillWidth, progressY + INFO_PROGRESS_HEIGHT, COLOR_PROGRESS_FILL);
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int guiLeft = this.leftPos;
        int guiTop = this.topPos;
        int flowScrollPixels = (int) (System.currentTimeMillis() / 40L);

        int expFill = computeTankFillPixels(this.menu.getFluidAmount(), this.menu.getFluidCapacity(), XP_TANK_HEIGHT);
        int energyFill = computeTankFillPixels(this.menu.getEnergyStored(), this.menu.getEnergyCapacity(), ENERGY_TANK_HEIGHT);
        drawTintedFlowTank(
                guiGraphics,
                guiLeft + XP_TANK_X_OFFSET,
                guiTop + XP_TANK_Y_OFFSET,
                XP_TANK_WIDTH,
                XP_TANK_HEIGHT,
                expFill,
                flowScrollPixels,
                0.40F,
                1.0F,
                0.55F);
        drawTintedFlowTank(
                guiGraphics,
                guiLeft + ENERGY_TANK_X_OFFSET,
                guiTop + ENERGY_TANK_Y_OFFSET,
                ENERGY_TANK_WIDTH,
                ENERGY_TANK_HEIGHT,
                energyFill,
                flowScrollPixels + 24,
                0.98F,
                0.22F,
                0.10F);

        guiGraphics.blit(
                MAIN_UI_TEXTURE,
                guiLeft,
                guiTop,
                0.0F,
                0.0F,
                this.imageWidth,
                this.imageHeight,
                MAIN_UI_TEXTURE_WIDTH,
                MAIN_UI_TEXTURE_HEIGHT);

        List<Integer> displaySlots = collectQueuedMobSlots();
        int maxScroll = Math.max(0, displaySlots.size() - MOB_LIST_VISIBLE_ROWS);
        this.mobScroll = Math.max(0, Math.min(this.mobScroll, maxScroll));

        int statusTextX = guiLeft + STATUS_TEXT_X_OFFSET;
        drawScaledStatusText(guiGraphics, "Queued: " + this.menu.getTotalQueuedMobs(), statusTextX, guiTop + STATUS_TEXT_START_Y_OFFSET, COLOR_WHITE);
        drawScaledStatusText(guiGraphics, "Processing: " + this.menu.getTotalProcessingMobs(), statusTextX, guiTop + STATUS_TEXT_START_Y_OFFSET + STATUS_TEXT_LINE_HEIGHT, COLOR_WHITE);
        drawScaledStatusText(guiGraphics, "Buffer: " + this.menu.getOutputBufferItemCount() + "/2048", statusTextX, guiTop + STATUS_TEXT_START_Y_OFFSET + (STATUS_TEXT_LINE_HEIGHT * 2), COLOR_ACCENT);
        if (displaySlots.isEmpty()) {
            drawScaledStatusText(guiGraphics, "No mobs queued", statusTextX, guiTop + STATUS_LIST_START_Y_OFFSET, COLOR_WHITE);
        }
        for (int i = 0; i < MOB_LIST_VISIBLE_ROWS; i++) {
            int index = this.mobScroll + i;
            if (index >= displaySlots.size()) {
                break;
            }

            int slot = displaySlots.get(index);
            int captured = this.menu.getCapturedMobCount(slot);
            EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.byId(this.menu.getCapturedMobTypeRawId(slot));
            String name = type == null ? "Unknown" : type.getDescription().getString();
            String text = String.format("%s: %d", name, captured);

            int lineY = guiTop + STATUS_LIST_START_Y_OFFSET + i * STATUS_LIST_LINE_HEIGHT;
            drawScaledStatusText(guiGraphics, text, statusTextX, lineY, COLOR_WHITE);
        }
        drawUpgradeLetter(guiGraphics, this.menu.slots.get(this.menu.getSharpnessMenuSlotIndex()), "S");
        drawUpgradeLetter(guiGraphics, this.menu.slots.get(this.menu.getLootingMenuSlotIndex()), "L");
        drawUpgradeLetter(guiGraphics, this.menu.slots.get(this.menu.getUnbreakingMenuSlotIndex()), "U");

        drawProcessingProgressBar(guiGraphics, guiLeft, guiTop);

        int toggleX1 = guiLeft + CONFIG_TOGGLE_X_OFFSET;
        int toggleY1 = guiTop + CONFIG_TOGGLE_Y_OFFSET;
        int toggleX2 = toggleX1 + CONFIG_TOGGLE_WIDTH;
        int toggleY2 = toggleY1 + CONFIG_TOGGLE_HEIGHT;
        guiGraphics.fill(toggleX1, toggleY1, toggleX2, toggleY2, 0xFFB8B8B8);
        guiGraphics.fill(toggleX1 + 1, toggleY1 + 1, toggleX2 - 1, toggleY2 - 1, this.configOpen ? 0xFFE7E7E7 : 0xFFD2D2D2);

        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
    }

    private void renderConfigOverlay(GuiGraphics guiGraphics) {
        if (!this.configOpen) {
            return;
        }

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(0.0F, 0.0F, 1000.0F);

        int panelX1 = getConfigPanelX();
        int panelY1 = getConfigPanelY();
        guiGraphics.blit(
                CONFIG_PANEL_TEXTURE,
                panelX1,
                panelY1,
                0.0F,
                0.0F,
                PANEL_WIDTH,
                PANEL_HEIGHT,
                CONFIG_PANEL_TEXTURE_WIDTH,
                CONFIG_PANEL_TEXTURE_HEIGHT);

        int labelsX = panelX1 + CONFIG_LABELS_X_OFFSET;
        int valuesCenterX = panelX1 + CONFIG_VALUES_CENTER_X_OFFSET;
        drawScaledConfigText(guiGraphics, "Area", labelsX, panelY1 + CONFIG_AREA_HEADER_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "X:", labelsX, panelY1 + CONFIG_AREA_X_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "Y:", labelsX, panelY1 + CONFIG_AREA_Y_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "Z:", labelsX, panelY1 + CONFIG_AREA_Z_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getAreaX()), valuesCenterX, panelY1 + CONFIG_AREA_X_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getAreaY()), valuesCenterX, panelY1 + CONFIG_AREA_Y_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getAreaZ()), valuesCenterX, panelY1 + CONFIG_AREA_Z_Y_OFFSET, COLOR_CONFIG_TEXT);

        drawScaledConfigText(guiGraphics, "Position", labelsX, panelY1 + CONFIG_POSITION_HEADER_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "X:", labelsX, panelY1 + CONFIG_POS_X_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "Y:", labelsX, panelY1 + CONFIG_POS_Y_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "Z:", labelsX, panelY1 + CONFIG_POS_Z_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getPosX()), valuesCenterX, panelY1 + CONFIG_POS_X_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getPosY()), valuesCenterX, panelY1 + CONFIG_POS_Y_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getPosZ()), valuesCenterX, panelY1 + CONFIG_POS_Z_Y_OFFSET, COLOR_CONFIG_TEXT);

        drawScaledConfigText(guiGraphics, "Tick Rate", labelsX, panelY1 + CONFIG_TICK_RATE_HEADER_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledConfigText(guiGraphics, "T:", labelsX, panelY1 + CONFIG_TICK_RATE_VALUE_Y_OFFSET, COLOR_CONFIG_TEXT);
        drawScaledCenteredConfigText(guiGraphics, String.valueOf(this.menu.getCaptureTickInterval()), valuesCenterX, panelY1 + CONFIG_TICK_RATE_VALUE_Y_OFFSET, COLOR_CONFIG_TEXT);

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
        if (isMouseOverXpTank((int) mouseX, (int) mouseY) && (button == 0 || button == 1)) {
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
        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(INVENTORY_TEXT_SCALE, INVENTORY_TEXT_SCALE, 1.0F);
        int scaledX = Math.round(this.inventoryLabelX / INVENTORY_TEXT_SCALE);
        int scaledY = Math.round(this.inventoryLabelY / INVENTORY_TEXT_SCALE);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, scaledX, scaledY, 0x2A1A0F, false);
        guiGraphics.pose().popPose();
    }

    private boolean isMouseOverXpTank(int mouseX, int mouseY) {
        int tankX1 = this.leftPos + XP_TANK_X_OFFSET;
        int tankY1 = this.topPos + XP_TANK_Y_OFFSET;
        int tankX2 = tankX1 + XP_TANK_WIDTH;
        int tankY2 = tankY1 + XP_TANK_HEIGHT;
        return mouseX >= tankX1 && mouseX < tankX2 && mouseY >= tankY1 && mouseY < tankY2;
    }

    private boolean isMouseOverEnergyTank(int mouseX, int mouseY) {
        int tankX1 = this.leftPos + ENERGY_TANK_X_OFFSET;
        int tankY1 = this.topPos + ENERGY_TANK_Y_OFFSET;
        int tankX2 = tankX1 + ENERGY_TANK_WIDTH;
        int tankY2 = tankY1 + ENERGY_TANK_HEIGHT;
        return mouseX >= tankX1 && mouseX < tankX2 && mouseY >= tankY1 && mouseY < tankY2;
    }

    private void drawUpgradeLetter(GuiGraphics guiGraphics, Slot slot, String letter) {
        if (slot.hasItem()) {
            return;
        }
        int slotX = this.leftPos + slot.x;
        int slotY = this.topPos + slot.y;
        int textWidth = this.font.width(letter);
        int textX = slotX + (16 - textWidth) / 2;
        int textY = slotY + 4;
        guiGraphics.drawString(this.font, letter, textX, textY, COLOR_DARK_TEXT, false);
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
        if (isMouseOverXpTank(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.literal(this.menu.getFluidAmount() + " / " + this.menu.getFluidCapacity() + " mB XP"),
                    mouseX,
                    mouseY);
        } else if (isMouseOverEnergyTank(mouseX, mouseY)) {
            guiGraphics.renderTooltip(
                    this.font,
                    Component.literal(this.menu.getEnergyStored() + " / " + this.menu.getEnergyCapacity() + " FE"),
                    mouseX,
                    mouseY);
        }
        if (!overConfigPanel) {
            this.renderTooltip(guiGraphics, mouseX, mouseY);
        }
    }

    private static class TextureButton extends Button {
        private final ResourceLocation normalTexture;
        private final ResourceLocation pressedTexture;
        private boolean pressedVisual;

        private TextureButton(int x, int y, int width, int height, ResourceLocation normalTexture, ResourceLocation pressedTexture, Button.OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.normalTexture = normalTexture;
            this.pressedTexture = pressedTexture;
        }

        @Override
        protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            ResourceLocation texture = this.pressedVisual ? this.pressedTexture : this.normalTexture;
            guiGraphics.blit(texture, this.getX(), this.getY(), 0.0F, 0.0F, this.getWidth(), this.getHeight(), 256, 256);
        }

        @Override
        public void onClick(double mouseX, double mouseY) {
            this.pressedVisual = true;
            super.onClick(mouseX, mouseY);
        }

        @Override
        public void onRelease(double mouseX, double mouseY) {
            this.pressedVisual = false;
            super.onRelease(mouseX, mouseY);
        }
    }

}
