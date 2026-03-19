package com.essenceextractor.essenceextractormod.menu;

import com.essenceextractor.essenceextractormod.EssenceExtractor;
import com.essenceextractor.essenceextractormod.blockentity.EssenceExtractorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/**
 * Server/client container wiring for the Essence Extractor screen.
 * Handles slot layout, shift-click routing, and compact machine state sync.
 */
public class EssenceExtractorMenu extends AbstractContainerMenu {
    // Synced machine-data slots (ContainerData indexes).
    private static final int DATA_FLUID_AMOUNT = 0;
    private static final int DATA_FLUID_CAPACITY = 1;
    private static final int DATA_ENERGY_STORED = 2;
    private static final int DATA_ENERGY_CAPACITY = 3;
    private static final int DATA_AREA_X = 4;
    private static final int DATA_AREA_Y = 5;
    private static final int DATA_AREA_Z = 6;
    private static final int DATA_POS_X = 7;
    private static final int DATA_POS_Y = 8;
    private static final int DATA_POS_Z = 9;
    private static final int DATA_SHOW_AREA = 10;
    private static final int DATA_CAPTURE_INTERVAL = 11;
    private static final int DATA_PROCESS_PERCENT = 12;
    private static final int DATA_SHARPNESS_LEVEL = 13;
    private static final int DATA_LOOTING_LEVEL = 14;
    private static final int DATA_UNBREAKING_LEVEL = 15;
    private static final int DATA_OUTPUT_BUFFER_COUNT = 16;
    private static final int DATA_PROCESSING_PROGRESS = 17;
    private static final int MOB_DATA_START_INDEX = DATA_PROCESSING_PROGRESS + 1;
    private static final int MOB_DATA_STRIDE = 3;
    private static final int MACHINE_DATA_COUNT = 18 + (EssenceExtractorBlockEntity.CAPTURED_DISPLAY_COUNT * 3);
    private static final int MACHINE_GRID_COLUMNS = 7;
    private static final int MACHINE_GRID_ROWS = 3;
    private static final int MACHINE_INVENTORY_SLOT_COUNT = MACHINE_GRID_COLUMNS * MACHINE_GRID_ROWS;
    private static final int UPGRADE_SLOT_COUNT = EssenceExtractorBlockEntity.UPGRADE_SLOT_COUNT;
    private static final int MACHINE_SLOT_START = 0;
    private static final int UPGRADE_SLOT_START = MACHINE_SLOT_START + MACHINE_INVENTORY_SLOT_COUNT;
    private static final int PLAYER_SLOT_START = UPGRADE_SLOT_START + UPGRADE_SLOT_COUNT;
    private static final int PLAYER_SLOT_END_EXCLUSIVE = PLAYER_SLOT_START + 27 + 9;

    private final Level level;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData machineData;

    public EssenceExtractorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, resolveMachineItemHandler(playerInventory, pos), createMachineDataSync(playerInventory, pos));
    }

    private EssenceExtractorMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler machineInventory, ContainerData machineData) {
        super(EssenceExtractor.ESSENCE_EXTRACTOR_MENU.get(), containerId);
        this.level = playerInventory.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(this.level, this.pos);
        this.machineData = machineData;

        addMachineSlots(machineInventory);
        addUpgradeSlots(resolveUpgradeItemHandler(playerInventory, pos));
        addPlayerInventorySlots(playerInventory);

        this.addDataSlots(this.machineData);
    }

    private void addMachineSlots(IItemHandler machineInventory) {
        int machineX = 8;
        int machineY = 70;
        for (int row = 0; row < MACHINE_GRID_ROWS; row++) {
            for (int col = 0; col < MACHINE_GRID_COLUMNS; col++) {
                int index = row * MACHINE_GRID_COLUMNS + col;
                this.addSlot(new SlotItemHandler(machineInventory, index, machineX + col * 18, machineY + row * 18));
            }
        }
    }

    private void addUpgradeSlots(IItemHandler upgradeHandler) {
        this.addSlot(new SlotItemHandler(upgradeHandler, EssenceExtractorBlockEntity.SHARPNESS_SLOT, 116, 8));
        this.addSlot(new SlotItemHandler(upgradeHandler, EssenceExtractorBlockEntity.LOOTING_SLOT, 116, 26));
        this.addSlot(new SlotItemHandler(upgradeHandler, EssenceExtractorBlockEntity.UNBREAKING_SLOT, 116, 44));
    }

    private void addPlayerInventorySlots(Inventory playerInventory) {
        int playerInvY = 132;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slot, 8 + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = 190;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }
    }

    private static IItemHandler resolveMachineItemHandler(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            return blockEntity.getItemHandler();
        }

        return new ItemStackHandler(EssenceExtractorBlockEntity.SLOT_COUNT);
    }

    private static IItemHandler resolveUpgradeItemHandler(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            return blockEntity.getUpgradeItemHandler();
        }
        return new ItemStackHandler(EssenceExtractorBlockEntity.UPGRADE_SLOT_COUNT);
    }

    private static ContainerData createMachineDataSync(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().isClientSide()) {
            // Client side does not read BE directly; values are delivered by ContainerData sync.
            return new SimpleContainerData(MACHINE_DATA_COUNT);
        }

        if (playerInventory.player.level().getBlockEntity(pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            return new ContainerData() {
                @Override
                public int get(int index) {
                    if (index < MOB_DATA_START_INDEX) {
                        return switch (index) {
                            case DATA_FLUID_AMOUNT -> blockEntity.getFluidAmount();
                            case DATA_FLUID_CAPACITY -> blockEntity.getFluidCapacity();
                            case DATA_ENERGY_STORED -> blockEntity.getEnergyStored();
                            case DATA_ENERGY_CAPACITY -> blockEntity.getEnergyCapacity();
                            case DATA_AREA_X -> blockEntity.getAreaX();
                            case DATA_AREA_Y -> blockEntity.getAreaY();
                            case DATA_AREA_Z -> blockEntity.getAreaZ();
                            case DATA_POS_X -> blockEntity.getPosX();
                            case DATA_POS_Y -> blockEntity.getPosY();
                            case DATA_POS_Z -> blockEntity.getPosZ();
                            case DATA_SHOW_AREA -> blockEntity.isShowArea() ? 1 : 0;
                            case DATA_CAPTURE_INTERVAL -> blockEntity.getCaptureTickInterval();
                            case DATA_PROCESS_PERCENT -> blockEntity.getProcessPercent();
                            case DATA_SHARPNESS_LEVEL -> blockEntity.getSharpnessUpgradeLevel();
                            case DATA_LOOTING_LEVEL -> blockEntity.getLootingUpgradeLevel();
                            case DATA_UNBREAKING_LEVEL -> blockEntity.getUnbreakingUpgradeLevel();
                            case DATA_OUTPUT_BUFFER_COUNT -> blockEntity.getOutputBufferItemCount();
                            case DATA_PROCESSING_PROGRESS -> blockEntity.getProcessingProgressPercent();
                            default -> 0;
                        };
                    }

                    int offset = index - MOB_DATA_START_INDEX;
                    int slot = offset / MOB_DATA_STRIDE;
                    int part = offset % MOB_DATA_STRIDE;
                    return switch (part) {
                        case 0 -> blockEntity.getCapturedMobTypeRawId(slot);
                        case 1 -> blockEntity.getCapturedMobCount(slot);
                        case 2 -> blockEntity.getProcessingMobCount(slot);
                        default -> 0;
                    };
                }

                @Override
                public void set(int index, int value) {
                    // Server authoritative.
                }

                @Override
                public int getCount() {
                    return MACHINE_DATA_COUNT;
                }
            };
        }

        return new SimpleContainerData(MACHINE_DATA_COUNT);
    }

    public int getFluidAmount() {
        return this.machineData.get(DATA_FLUID_AMOUNT);
    }

    public int getFluidCapacity() {
        return this.machineData.get(DATA_FLUID_CAPACITY);
    }

    public int getAreaX() {
        return this.machineData.get(DATA_AREA_X);
    }

    public int getAreaY() {
        return this.machineData.get(DATA_AREA_Y);
    }

    public int getAreaZ() {
        return this.machineData.get(DATA_AREA_Z);
    }

    public int getPosX() {
        return this.machineData.get(DATA_POS_X);
    }

    public int getPosY() {
        return this.machineData.get(DATA_POS_Y);
    }

    public int getPosZ() {
        return this.machineData.get(DATA_POS_Z);
    }

    public boolean isShowArea() {
        return this.machineData.get(DATA_SHOW_AREA) != 0;
    }

    public int getCaptureTickInterval() {
        return this.machineData.get(DATA_CAPTURE_INTERVAL);
    }

    public int getProcessPercent() {
        return this.machineData.get(DATA_PROCESS_PERCENT);
    }

    public int getSharpnessUpgradeLevel() {
        return this.machineData.get(DATA_SHARPNESS_LEVEL);
    }

    public int getLootingUpgradeLevel() {
        return this.machineData.get(DATA_LOOTING_LEVEL);
    }

    public int getUnbreakingUpgradeLevel() {
        return this.machineData.get(DATA_UNBREAKING_LEVEL);
    }

    public int getEnergyStored() {
        return this.machineData.get(DATA_ENERGY_STORED);
    }

    public int getEnergyCapacity() {
        return this.machineData.get(DATA_ENERGY_CAPACITY);
    }

    public int getOutputBufferItemCount() {
        return this.machineData.get(DATA_OUTPUT_BUFFER_COUNT);
    }

    public int getProcessingProgressPercent() {
        return this.machineData.get(DATA_PROCESSING_PROGRESS);
    }

    public BlockPos getBlockPos() {
        return this.pos;
    }

    private int mobDataIndex(int slot, int partOffset) {
        return MOB_DATA_START_INDEX + (slot * MOB_DATA_STRIDE) + partOffset;
    }

    public int getCapturedMobTypeRawId(int index) {
        return this.machineData.get(mobDataIndex(index, 0));
    }

    public int getCapturedMobCount(int index) {
        return this.machineData.get(mobDataIndex(index, 1));
    }

    public int getProcessingMobCount(int index) {
        return this.machineData.get(mobDataIndex(index, 2));
    }

    public int getUnbreakingMenuSlotIndex() {
        return UPGRADE_SLOT_START + EssenceExtractorBlockEntity.UNBREAKING_SLOT;
    }

    public int getSharpnessMenuSlotIndex() {
        return UPGRADE_SLOT_START + EssenceExtractorBlockEntity.SHARPNESS_SLOT;
    }

    public int getLootingMenuSlotIndex() {
        return UPGRADE_SLOT_START + EssenceExtractorBlockEntity.LOOTING_SLOT;
    }

    public int getTotalQueuedMobs() {
        int total = 0;
        for (int i = 0; i < EssenceExtractorBlockEntity.CAPTURED_DISPLAY_COUNT; i++) {
            total += Math.max(0, getCapturedMobCount(i));
        }
        return total;
    }

    public int getTotalProcessingMobs() {
        int total = 0;
        for (int i = 0; i < EssenceExtractorBlockEntity.CAPTURED_DISPLAY_COUNT; i++) {
            total += Math.max(0, getProcessingMobCount(i));
        }
        return total;
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (this.level.getBlockEntity(this.pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            if (id == 13) {
                if (tryExtractWithCarriedStack(blockEntity)) {
                    return true;
                }
                return blockEntity.extractBucketFromTank(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            if (id == 18) {
                if (tryInsertWithCarriedStack(blockEntity)) {
                    return true;
                }
                return blockEntity.insertBucketIntoTank(player, net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            blockEntity.applyMenuButton(id);
            return true;
        }
        return false;
    }

    private boolean tryExtractWithCarriedStack(EssenceExtractorBlockEntity blockEntity) {
        ItemStack carried = this.getCarried();
        if (!carried.is(Items.BUCKET) || carried.getCount() != 1 || !blockEntity.canExtractBucketFromTank()) {
            return false;
        }
        blockEntity.extractOneBucketFromTank();
        this.setCarried(new ItemStack(EssenceExtractor.EXPERIENCE_BUCKET.get()));
        return true;
    }

    private boolean tryInsertWithCarriedStack(EssenceExtractorBlockEntity blockEntity) {
        ItemStack carried = this.getCarried();
        if (!carried.is(EssenceExtractor.EXPERIENCE_BUCKET.get()) || carried.getCount() != 1 || !blockEntity.canInsertBucketIntoTank()) {
            return false;
        }
        blockEntity.insertOneBucketIntoTank();
        this.setCarried(new ItemStack(Items.BUCKET));
        return true;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        // Standard shift-click transfer rules:
        // - From machine/upgrade slots -> player inventory
        // - From player inventory -> upgrade slots (books) first, then machine storage
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack sourceCopy = sourceStack.copy();

        if (index < PLAYER_SLOT_START) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_SLOT_START, PLAYER_SLOT_END_EXCLUSIVE, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (sourceStack.is(Items.ENCHANTED_BOOK)) {
                moved = moveBookToMatchingUpgradeSlot(sourceStack);
            }
            if (!moved) {
                moved = this.moveItemStackTo(sourceStack, MACHINE_SLOT_START, MACHINE_INVENTORY_SLOT_COUNT, false);
            }
            if (!moved) {
                return ItemStack.EMPTY;
            }
        }

        if (sourceStack.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(player, sourceStack);
        return sourceCopy;
    }

    private boolean moveBookToMatchingUpgradeSlot(ItemStack stack) {
        var enchantments = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var sharpness = enchantments.getOrThrow(Enchantments.SHARPNESS);
        var looting = enchantments.getOrThrow(Enchantments.LOOTING);
        var unbreaking = enchantments.getOrThrow(Enchantments.UNBREAKING);
        boolean hasSharpness = getBookEnchantLevel(stack, sharpness) > 0;
        boolean hasLooting = getBookEnchantLevel(stack, looting) > 0;
        boolean hasUnbreaking = getBookEnchantLevel(stack, unbreaking) > 0;

        if (hasSharpness && moveToUpgradeSlot(stack, EssenceExtractorBlockEntity.SHARPNESS_SLOT)) {
            return true;
        }
        if (hasLooting && moveToUpgradeSlot(stack, EssenceExtractorBlockEntity.LOOTING_SLOT)) {
            return true;
        }
        if (hasUnbreaking && moveToUpgradeSlot(stack, EssenceExtractorBlockEntity.UNBREAKING_SLOT)) {
            return true;
        }
        if (hasSharpness || hasLooting || hasUnbreaking) {
            // Fallbacks: if preferred slot was occupied, allow any matching slot.
            if (moveToUpgradeSlot(stack, EssenceExtractorBlockEntity.SHARPNESS_SLOT)) {
                return true;
            }
            if (moveToUpgradeSlot(stack, EssenceExtractorBlockEntity.LOOTING_SLOT)) {
                return true;
            }
            if (moveToUpgradeSlot(stack, EssenceExtractorBlockEntity.UNBREAKING_SLOT)) {
                return true;
            }
        }
        return false;
    }

    private boolean moveToUpgradeSlot(ItemStack stack, int upgradeSlot) {
        int startIndex = UPGRADE_SLOT_START + upgradeSlot;
        return this.moveItemStackTo(stack, startIndex, startIndex + 1, false);
    }

    private static int getBookEnchantLevel(ItemStack stack, net.minecraft.core.Holder<net.minecraft.world.item.enchantment.Enchantment> enchantment) {
        int directLevel = EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
        if (directLevel > 0) {
            return directLevel;
        }
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(this.access, player, EssenceExtractor.ESSENCE_EXTRACTOR_BLOCK.get());
    }
}
