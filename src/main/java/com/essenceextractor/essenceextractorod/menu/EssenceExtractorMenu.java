package com.essenceextractor.essenceextractormod.menu;

import com.essenceextractor.essenceextractormod.EssenceExtractor;
import com.essenceextractor.essenceextractormod.blockentity.EssenceExtractorBlockEntity;

import net.minecraft.core.BlockPos;
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
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public class EssenceExtractorMenu extends AbstractContainerMenu {
    private static final int MACHINE_DATA_COUNT = 14 + (EssenceExtractorBlockEntity.CAPTURED_DISPLAY_COUNT * 3);
    private static final int MACHINE_COLUMNS = 8;
    private static final int MACHINE_ROWS = 3;
    private static final int MACHINE_SLOT_COUNT = MACHINE_COLUMNS * MACHINE_ROWS;
    private static final int UPGRADE_SLOT_COUNT = EssenceExtractorBlockEntity.UPGRADE_SLOT_COUNT;
    private static final int MACHINE_START = 0;
    private static final int UPGRADE_START = MACHINE_START + MACHINE_SLOT_COUNT;
    private static final int PLAYER_START = UPGRADE_START + UPGRADE_SLOT_COUNT;
    private static final int HOTBAR_END = PLAYER_START + 27 + 9;

    private final Level level;
    private final BlockPos pos;
    private final ContainerLevelAccess access;
    private final ContainerData machineData;

    public EssenceExtractorMenu(int containerId, Inventory playerInventory, BlockPos pos) {
        this(containerId, playerInventory, pos, getItemHandler(playerInventory, pos), getMachineData(playerInventory, pos));
    }

    private EssenceExtractorMenu(int containerId, Inventory playerInventory, BlockPos pos, IItemHandler machineInventory, ContainerData machineData) {
        super(EssenceExtractor.ESSENCE_EXTRACTOR_MENU.get(), containerId);
        this.level = playerInventory.player.level();
        this.pos = pos;
        this.access = ContainerLevelAccess.create(this.level, this.pos);
        this.machineData = machineData;

        int machineX = 8;
        int machineY = 74;
        for (int row = 0; row < MACHINE_ROWS; row++) {
            for (int col = 0; col < MACHINE_COLUMNS; col++) {
                int index = row * MACHINE_COLUMNS + col;
                this.addSlot(new SlotItemHandler(machineInventory, index, machineX + col * 18, machineY + row * 18));
            }
        }

        var upgradeHandler = getUpgradeItemHandler(playerInventory, pos);
        this.addSlot(new SlotItemHandler(upgradeHandler, EssenceExtractorBlockEntity.SHARPNESS_SLOT, 140, 16));
        this.addSlot(new SlotItemHandler(upgradeHandler, EssenceExtractorBlockEntity.LOOTING_SLOT, 140, 46));

        int playerInvY = 150;
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                int slot = col + row * 9 + 9;
                this.addSlot(new Slot(playerInventory, slot, 8 + col * 18, playerInvY + row * 18));
            }
        }

        int hotbarY = 208;
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
        }

        this.addDataSlots(this.machineData);
    }

    private static IItemHandler getItemHandler(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            return blockEntity.getItemHandler();
        }

        return new ItemStackHandler(EssenceExtractorBlockEntity.SLOT_COUNT);
    }

    private static IItemHandler getUpgradeItemHandler(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().getBlockEntity(pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            return blockEntity.getUpgradeItemHandler();
        }
        return new ItemStackHandler(EssenceExtractorBlockEntity.UPGRADE_SLOT_COUNT);
    }

    private static ContainerData getMachineData(Inventory playerInventory, BlockPos pos) {
        if (playerInventory.player.level().isClientSide()) {
            // Client must rely on menu slot sync values, not direct BE reads.
            return new SimpleContainerData(MACHINE_DATA_COUNT);
        }

        if (playerInventory.player.level().getBlockEntity(pos) instanceof EssenceExtractorBlockEntity blockEntity) {
            return new ContainerData() {
                @Override
                public int get(int index) {
                    if (index < 14) {
                        return switch (index) {
                            case 0 -> blockEntity.getFluidAmount();
                            case 1 -> blockEntity.getFluidCapacity();
                            case 2 -> blockEntity.getAreaX();
                            case 3 -> blockEntity.getAreaY();
                            case 4 -> blockEntity.getAreaZ();
                            case 5 -> blockEntity.getPosX();
                            case 6 -> blockEntity.getPosY();
                            case 7 -> blockEntity.getPosZ();
                            case 8 -> blockEntity.isShowArea() ? 1 : 0;
                            case 9 -> blockEntity.getCaptureTickInterval();
                            case 10 -> blockEntity.getProcessPercent();
                            case 11 -> blockEntity.getSharpnessUpgradeLevel();
                            case 12 -> blockEntity.getLootingUpgradeLevel();
                            case 13 -> blockEntity.getOutputBufferItemCount();
                            default -> 0;
                        };
                    }

                    int offset = index - 14;
                    int slot = offset / 3;
                    int part = offset % 3;
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
        return this.machineData.get(0);
    }

    public int getFluidCapacity() {
        return this.machineData.get(1);
    }

    public int getAreaX() {
        return this.machineData.get(2);
    }

    public int getAreaY() {
        return this.machineData.get(3);
    }

    public int getAreaZ() {
        return this.machineData.get(4);
    }

    public int getPosX() {
        return this.machineData.get(5);
    }

    public int getPosY() {
        return this.machineData.get(6);
    }

    public int getPosZ() {
        return this.machineData.get(7);
    }

    public boolean isShowArea() {
        return this.machineData.get(8) != 0;
    }

    public int getCaptureTickInterval() {
        return this.machineData.get(9);
    }

    public int getProcessPercent() {
        return this.machineData.get(10);
    }

    public int getSharpnessUpgradeLevel() {
        return this.machineData.get(11);
    }

    public int getLootingUpgradeLevel() {
        return this.machineData.get(12);
    }

    public int getOutputBufferItemCount() {
        return this.machineData.get(13);
    }

    public BlockPos getBlockPos() {
        return this.pos;
    }

    public int getCapturedMobTypeRawId(int index) {
        return this.machineData.get(14 + (index * 3));
    }

    public int getCapturedMobCount(int index) {
        return this.machineData.get(15 + (index * 3));
    }

    public int getProcessingMobCount(int index) {
        return this.machineData.get(16 + (index * 3));
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
        Slot sourceSlot = this.slots.get(index);
        if (!sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }

        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack sourceCopy = sourceStack.copy();

        if (index < PLAYER_START) {
            if (!this.moveItemStackTo(sourceStack, PLAYER_START, HOTBAR_END, true)) {
                return ItemStack.EMPTY;
            }
        } else {
            boolean moved = false;
            if (sourceStack.is(net.minecraft.world.item.Items.ENCHANTED_BOOK)) {
                moved = moveBookToMatchingUpgradeSlot(sourceStack);
            }
            if (!moved) {
                moved = this.moveItemStackTo(sourceStack, MACHINE_START, MACHINE_SLOT_COUNT, false);
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
        boolean hasSharpness = getBookEnchantLevel(stack, sharpness) > 0;
        boolean hasLooting = getBookEnchantLevel(stack, looting) > 0;

        if (hasSharpness && this.moveItemStackTo(stack, UPGRADE_START + EssenceExtractorBlockEntity.SHARPNESS_SLOT, UPGRADE_START + EssenceExtractorBlockEntity.SHARPNESS_SLOT + 1, false)) {
            return true;
        }
        if (hasLooting && this.moveItemStackTo(stack, UPGRADE_START + EssenceExtractorBlockEntity.LOOTING_SLOT, UPGRADE_START + EssenceExtractorBlockEntity.LOOTING_SLOT + 1, false)) {
            return true;
        }
        if (hasSharpness && hasLooting) {
            // Fallback: if one slot was occupied, allow the other.
            if (this.moveItemStackTo(stack, UPGRADE_START + EssenceExtractorBlockEntity.SHARPNESS_SLOT, UPGRADE_START + EssenceExtractorBlockEntity.SHARPNESS_SLOT + 1, false)) {
                return true;
            }
            if (this.moveItemStackTo(stack, UPGRADE_START + EssenceExtractorBlockEntity.LOOTING_SLOT, UPGRADE_START + EssenceExtractorBlockEntity.LOOTING_SLOT + 1, false)) {
                return true;
            }
        }
        // Last resort for modded/atypical enchanted books: place into any free upgrade slot.
        if (this.moveItemStackTo(stack, UPGRADE_START, UPGRADE_START + UPGRADE_SLOT_COUNT, false)) {
            return true;
        }
        return false;
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
