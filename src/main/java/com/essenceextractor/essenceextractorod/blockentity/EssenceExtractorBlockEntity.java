package com.essenceextractor.essenceextractormod.blockentity;

import java.util.Comparator;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;

import com.essenceextractor.essenceextractormod.EssenceExtractor;
import com.mojang.authlib.GameProfile;
import com.essenceextractor.essenceextractormod.ServerSettings;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.items.ItemStackHandler;

public class EssenceExtractorBlockEntity extends BlockEntity implements net.minecraft.world.MenuProvider {
    public static final int SLOT_COUNT = 24;
    public static final int UPGRADE_SLOT_COUNT = 2;
    public static final int SHARPNESS_SLOT = 0;
    public static final int LOOTING_SLOT = 1;
    public static final int FLUID_CAPACITY = FluidType.BUCKET_VOLUME * 32;
    public static final int CAPTURED_DISPLAY_COUNT = 20;
    public static final int OUTPUT_BUFFER_CAPACITY = 2048;
    private static final int MB_PER_XP = 20;
    private static final int BABY_MOB_XP_MB = MB_PER_XP;
    private static final int SMALL_QUEUE_FULL_PROCESS_THRESHOLD = 10;
    private static final int LARGE_QUEUE_MIN_BATCH = 10;
    private static final GameProfile EXTRACTOR_FAKE_PLAYER_PROFILE =
            new GameProfile(UUID.fromString("48f5b42d-2085-4f10-8300-36bd1fcc4d49"), "[EssenceExtractor]");

    private int captureTicks;
    private int mobProcessTicks;

    // Symmetric area radii.
    private int areaX = 1;
    private int areaY = 1;
    private int areaZ = 1;

    // Relative center position offset from machine block.
    private int posX = 0;
    private int posY = 1;
    private int posZ = 0;
    private int captureTickInterval = 10;
    private int processPercent = 50;

    private boolean showArea;
    private final ArrayDeque<CapturedMobData> capturedMobQueue = new ArrayDeque<>();
    private final ArrayDeque<CapturedMobData> processingMobQueue = new ArrayDeque<>();
    private final Map<EntityType<?>, Integer> processingMobCounts = new HashMap<>();
    private final ArrayList<ItemStack> outputBuffer = new ArrayList<>();

    private final ItemStackHandler itemHandler = new ItemStackHandler(SLOT_COUNT) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    // Separate upgrade inventory so automation/hoppers can only see normal machine slots.
    private final ItemStackHandler upgradeItemHandler = new ItemStackHandler(UPGRADE_SLOT_COUNT) {
        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return isValidUpgradeBook(slot, stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    private final FluidTank tank = new FluidTank(FLUID_CAPACITY, this::isFluidValid) {
        @Override
        protected void onContentsChanged() {
            setChanged();
            if (level != null && !level.isClientSide()) {
                level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    };

    public EssenceExtractorBlockEntity(BlockPos pos, BlockState blockState) {
        super(EssenceExtractor.ESSENCE_EXTRACTOR_BLOCK_ENTITY.get(), pos, blockState);
    }

    public ItemStackHandler getItemHandler() {
        return this.itemHandler;
    }

    public ItemStackHandler getUpgradeItemHandler() {
        return this.upgradeItemHandler;
    }

    public int getFluidAmount() {
        return this.tank.getFluidAmount();
    }

    public int getFluidCapacity() {
        return this.tank.getCapacity();
    }

    public int getAreaX() {
        return this.areaX;
    }

    public int getAreaY() {
        return this.areaY;
    }

    public int getAreaZ() {
        return this.areaZ;
    }

    public int getPosX() {
        return this.posX;
    }

    public int getPosY() {
        return this.posY;
    }

    public int getPosZ() {
        return this.posZ;
    }

    public boolean isShowArea() {
        return this.showArea;
    }

    public int getCaptureTickInterval() {
        return this.captureTickInterval;
    }

    public int getProcessPercent() {
        return this.processPercent;
    }

    public int getSharpnessUpgradeLevel() {
        return getUpgradeEnchantmentLevel(SHARPNESS_SLOT, Enchantments.SHARPNESS);
    }

    public int getLootingUpgradeLevel() {
        return getUpgradeEnchantmentLevel(LOOTING_SLOT, Enchantments.LOOTING);
    }

    public int getOutputBufferItemCount() {
        int count = 0;
        for (ItemStack stack : this.outputBuffer) {
            count += stack.getCount();
        }
        return count;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);

        tag.put("Inventory", this.itemHandler.serializeNBT(registries));
        tag.put("Upgrades", this.upgradeItemHandler.serializeNBT(registries));
        tag.put("Tank", this.tank.writeToNBT(registries, new CompoundTag()));

        tag.putInt("CaptureTicks", this.captureTicks);
        tag.putInt("MobProcessTicks", this.mobProcessTicks);
        tag.putInt("AreaX", this.areaX);
        tag.putInt("AreaY", this.areaY);
        tag.putInt("AreaZ", this.areaZ);
        tag.putInt("PosX", this.posX);
        tag.putInt("PosY", this.posY);
        tag.putInt("PosZ", this.posZ);
        tag.putInt("CaptureTickInterval", this.captureTickInterval);
        tag.putInt("ProcessPercent", this.processPercent);
        tag.putBoolean("ShowArea", this.showArea);

        ListTag queuedList = new ListTag();
        for (CapturedMobData data : this.capturedMobQueue) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(data.type());
            if (id == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("Type", id.toString());
            entry.putBoolean("Baby", data.wasBaby());
            queuedList.add(entry);
        }
        tag.put("CapturedQueue", queuedList);

        ListTag processingList = new ListTag();
        for (CapturedMobData data : this.processingMobQueue) {
            ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(data.type());
            if (id == null) {
                continue;
            }
            CompoundTag entry = new CompoundTag();
            entry.putString("Type", id.toString());
            entry.putBoolean("Baby", data.wasBaby());
            processingList.add(entry);
        }
        tag.put("ProcessingQueue", processingList);

        ListTag outputList = new ListTag();
        for (ItemStack stack : this.outputBuffer) {
            if (stack.isEmpty()) {
                continue;
            }
            outputList.add(stack.save(registries));
        }
        tag.put("OutputBuffer", outputList);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);

        this.itemHandler.deserializeNBT(registries, tag.getCompound("Inventory"));
        this.upgradeItemHandler.deserializeNBT(registries, tag.getCompound("Upgrades"));
        this.tank.readFromNBT(registries, tag.getCompound("Tank"));

        this.captureTicks = tag.getInt("CaptureTicks");
        this.mobProcessTicks = tag.getInt("MobProcessTicks");
        this.areaX = clampArea(tag.contains("AreaX") ? tag.getInt("AreaX") : this.areaX);
        this.areaY = clampArea(tag.contains("AreaY") ? tag.getInt("AreaY") : this.areaY);
        this.areaZ = clampArea(tag.contains("AreaZ") ? tag.getInt("AreaZ") : this.areaZ);
        this.posX = clampPos(tag.contains("PosX") ? tag.getInt("PosX") : this.posX);
        this.posY = clampPos(tag.contains("PosY") ? tag.getInt("PosY") : this.posY);
        this.posZ = clampPos(tag.contains("PosZ") ? tag.getInt("PosZ") : this.posZ);
        this.captureTickInterval = clampCaptureTick(tag.contains("CaptureTickInterval") ? tag.getInt("CaptureTickInterval") : this.captureTickInterval);
        this.processPercent = clampProcessPercent(tag.contains("ProcessPercent") ? tag.getInt("ProcessPercent") : this.processPercent);
        this.showArea = tag.getBoolean("ShowArea");

        this.capturedMobQueue.clear();
        this.processingMobQueue.clear();
        this.processingMobCounts.clear();
        this.outputBuffer.clear();

        ListTag queuedList = tag.getList("CapturedQueue", Tag.TAG_COMPOUND);
        for (int i = 0; i < queuedList.size(); i++) {
            CompoundTag entry = queuedList.getCompound(i);
            EntityType<?> type = parseEntityType(entry.getString("Type"));
            if (type == null) {
                continue;
            }
            this.capturedMobQueue.addLast(new CapturedMobData(type, entry.getBoolean("Baby")));
        }

        ListTag processingList = tag.getList("ProcessingQueue", Tag.TAG_COMPOUND);
        for (int i = 0; i < processingList.size(); i++) {
            CompoundTag entry = processingList.getCompound(i);
            EntityType<?> type = parseEntityType(entry.getString("Type"));
            if (type == null) {
                continue;
            }
            this.processingMobQueue.addLast(new CapturedMobData(type, entry.getBoolean("Baby")));
            this.processingMobCounts.merge(type, 1, Integer::sum);
        }

        ListTag outputList = tag.getList("OutputBuffer", Tag.TAG_COMPOUND);
        for (int i = 0; i < outputList.size(); i++) {
            ItemStack stack = ItemStack.parseOptional(registries, outputList.getCompound(i));
            if (!stack.isEmpty()) {
                this.outputBuffer.add(stack);
            }
        }
    }

    private static EntityType<?> parseEntityType(String id) {
        if (id == null || id.isEmpty()) {
            return null;
        }
        try {
            return BuiltInRegistries.ENTITY_TYPE.getOptional(ResourceLocation.parse(id)).orElse(null);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    public void applyMenuButton(int buttonId) {
        switch (buttonId) {
            case 0 -> this.areaX = clampArea(this.areaX - 1);
            case 1 -> this.areaX = clampArea(this.areaX + 1);
            case 2 -> this.areaY = clampArea(this.areaY - 1);
            case 3 -> this.areaY = clampArea(this.areaY + 1);
            case 4 -> this.areaZ = clampArea(this.areaZ - 1);
            case 5 -> this.areaZ = clampArea(this.areaZ + 1);
            case 6 -> this.posX = clampPos(this.posX - 1);
            case 7 -> this.posX = clampPos(this.posX + 1);
            case 8 -> this.posY = clampPos(this.posY - 1);
            case 9 -> this.posY = clampPos(this.posY + 1);
            case 10 -> this.posZ = clampPos(this.posZ - 1);
            case 11 -> this.posZ = clampPos(this.posZ + 1);
            case 12 -> this.showArea = !this.showArea;
            case 14 -> this.captureTickInterval = clampCaptureTick(this.captureTickInterval - 1);
            case 15 -> this.captureTickInterval = clampCaptureTick(this.captureTickInterval + 1);
            case 16 -> this.processPercent = clampProcessPercent(this.processPercent - 1);
            case 17 -> this.processPercent = clampProcessPercent(this.processPercent + 1);
            default -> {
                return;
            }
        }

        this.setChanged();
    }

    private static int clampArea(int value) {
        return Math.max(0, Math.min(16, value));
    }

    private static int clampPos(int value) {
        return Math.max(-16, Math.min(16, value));
    }

    private static int clampCaptureTick(int value) {
        return Math.max(ServerSettings.getMinCaptureTicks(), Math.min(200, value));
    }

    private static int clampProcessPercent(int value) {
        return Math.max(ServerSettings.getMinProcessPercent(), Math.min(100, value));
    }

    private boolean isFluidValid(FluidStack stack) {
        return stack.getFluid() == EssenceExtractor.EXPERIENCE_FLUID.get();
    }

    private boolean isValidUpgradeBook(int slot, ItemStack stack) {
        if (!stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        // Accept enchanted books in either upgrade slot; exact enchant effect is read during processing.
        return slot == SHARPNESS_SLOT || slot == LOOTING_SLOT;
    }

    private int getUpgradeEnchantmentLevel(int slot, ResourceKey<Enchantment> enchantmentKey) {
        return getEnchantmentLevel(this.upgradeItemHandler.getStackInSlot(slot), enchantmentKey);
    }

    private int getEnchantmentLevel(ItemStack stack, ResourceKey<Enchantment> enchantmentKey) {
        if (stack.isEmpty()) {
            return 0;
        }
        // Client-side slot validation can run before BE level is fully available.
        // Allow the move client-side and let server-side validation enforce exact enchant.
        if (this.level == null) {
            return 1;
        }
        var enchantments = this.level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        var enchantment = enchantments.getOrThrow(enchantmentKey);
        int directLevel = EnchantmentHelper.getTagEnchantmentLevel(enchantment, stack);
        if (directLevel > 0) {
            return directLevel;
        }
        // 1.21 data components can store enchant data differently depending on source.
        return EnchantmentHelper.getEnchantmentsForCrafting(stack).getLevel(enchantment);
    }

    private int getEffectiveMobProcessInterval() {
        int baseInterval = ServerSettings.getMobProcessIntervalTicks();
        int speedBonusPercent = getScaledUpgradeBonus(getSharpnessUpgradeLevel());
        double speedMultiplier = 1.0D + (speedBonusPercent / 100.0D);
        return (int) Math.ceil(baseInterval / speedMultiplier);
    }

    private static int getScaledUpgradeBonus(int level) {
        if (level <= 0) {
            return 0;
        }
        return (level * 4) + 6;
    }

    public boolean handleBucketInteraction(Player player, InteractionHand hand) {
        if (extractBucketFromTank(player, hand)) {
            return true;
        }
        return insertBucketIntoTank(player, hand);
    }

    public boolean extractBucketFromTank(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(Items.BUCKET) || this.tank.getFluidAmount() < FluidType.BUCKET_VOLUME) {
            return false;
        }

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
        }

        ItemStack filled = new ItemStack(EssenceExtractor.EXPERIENCE_BUCKET.get());
        if (held.isEmpty()) {
            player.setItemInHand(hand, filled);
        } else if (!player.addItem(filled)) {
            player.drop(filled, false);
        }

        this.tank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        this.setChanged();
        return true;
    }

    public boolean canExtractBucketFromTank() {
        return this.tank.getFluidAmount() >= FluidType.BUCKET_VOLUME;
    }

    public void extractOneBucketFromTank() {
        this.tank.drain(FluidType.BUCKET_VOLUME, IFluidHandler.FluidAction.EXECUTE);
        this.setChanged();
    }

    public boolean insertBucketIntoTank(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (!held.is(EssenceExtractor.EXPERIENCE_BUCKET.get()) || this.tank.getSpace() < FluidType.BUCKET_VOLUME) {
            return false;
        }

        this.tank.fill(new FluidStack(EssenceExtractor.EXPERIENCE_FLUID.get(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);

        if (!player.getAbilities().instabuild) {
            held.shrink(1);
            ItemStack empty = new ItemStack(Items.BUCKET);
            if (held.isEmpty()) {
                player.setItemInHand(hand, empty);
            } else if (!player.addItem(empty)) {
                player.drop(empty, false);
            }
        }

        this.setChanged();
        return true;
    }

    public boolean canInsertBucketIntoTank() {
        return this.tank.getSpace() >= FluidType.BUCKET_VOLUME;
    }

    public void insertOneBucketIntoTank() {
        this.tank.fill(new FluidStack(EssenceExtractor.EXPERIENCE_FLUID.get(), FluidType.BUCKET_VOLUME), IFluidHandler.FluidAction.EXECUTE);
        this.setChanged();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, EssenceExtractorBlockEntity blockEntity) {
        if (level.isClientSide()) {
            return;
        }

        blockEntity.flushOutputBufferToMachine();

        blockEntity.captureTicks++;
        if (blockEntity.captureTicks >= clampCaptureTick(blockEntity.captureTickInterval)) {
            blockEntity.captureTicks = 0;
            blockEntity.captureMobs(level, pos);

            if (blockEntity.showArea && level instanceof ServerLevel serverLevel) {
                blockEntity.renderAreaOutline(serverLevel, pos);
            }
        }

        blockEntity.mobProcessTicks++;
        if (blockEntity.mobProcessTicks >= blockEntity.getEffectiveMobProcessInterval()) {
            blockEntity.mobProcessTicks = 0;
            blockEntity.processCapturedMobs(level, pos);
        }

        blockEntity.captureItems(level, pos);
        blockEntity.captureExperience(level, pos);
    }

    private void captureItems(Level level, BlockPos pos) {
        AABB box = buildCaptureAABB(pos);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            ItemStack original = itemEntity.getItem();
            if (!canInsertIntoMachine(original)) {
                continue;
            }

            ItemStack remaining = insertIntoMachine(itemEntity.getItem().copy());
            if (remaining.isEmpty()) {
                itemEntity.discard();
            } else {
                itemEntity.setItem(remaining);
            }
        }
    }

    private ItemStack insertIntoMachine(ItemStack stack) {
        ItemStack working = stack;
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            if (working.isEmpty()) {
                break;
            }
            working = this.itemHandler.insertItem(i, working, false);
        }
        return working;
    }

    private boolean canInsertIntoMachine(ItemStack stack) {
        ItemStack working = stack.copy();
        for (int i = 0; i < this.itemHandler.getSlots(); i++) {
            if (working.isEmpty()) {
                return true;
            }
            working = this.itemHandler.insertItem(i, working, true);
        }
        return working.getCount() < stack.getCount();
    }

    private void captureExperience(Level level, BlockPos pos) {
        AABB box = buildCaptureAABB(pos);
        int space = this.tank.getSpace();
        if (space <= 0) {
            return;
        }

        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box)) {
            int orbMb = Math.max(1, orb.getValue() * MB_PER_XP);
            if (orbMb > this.tank.getSpace()) {
                continue;
            }

            int filled = this.tank.fill(new FluidStack(EssenceExtractor.EXPERIENCE_FLUID.get(), orbMb), IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                orb.discard();
            }
        }
    }

    private void captureMobs(Level level, BlockPos pos) {
        AABB box = buildCaptureAABB(pos);
        boolean changed = false;
        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, Mob::isAlive)) {
            if (hasEquipment(mob)) {
                continue;
            }
            this.capturedMobQueue.addLast(new CapturedMobData(mob.getType(), mob.isBaby()));
            mob.discard();
            changed = true;
        }

        if (changed) {
            this.setChanged();
            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    private void processCapturedMobs(Level level, BlockPos pos) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        // Two-step processing:
        // 1) Move 50% from queued -> processing list (UI visible, no outputs yet).
        // 2) On next process cycle, resolve processing list into drops/xp outputs.
        if (!this.processingMobQueue.isEmpty()) {
            finishProcessingBatch(serverLevel, pos);
            return;
        }

        startProcessingBatch();
    }

    private void startProcessingBatch() {
        if (this.capturedMobQueue.isEmpty()) {
            return;
        }

        this.processingMobCounts.clear();
        int totalQueued = this.capturedMobQueue.size();
        int toProcess;
        if (totalQueued <= SMALL_QUEUE_FULL_PROCESS_THRESHOLD) {
            // Small queues are fully drained to avoid getting "stuck" at low counts.
            toProcess = totalQueued;
        } else {
            // Larger queues process 50%, but never fewer than 10 in one batch.
            toProcess = Math.max(LARGE_QUEUE_MIN_BATCH, (int) Math.ceil(totalQueued * 0.5D));
        }
        for (int i = 0; i < toProcess && !this.capturedMobQueue.isEmpty(); i++) {
            CapturedMobData data = this.capturedMobQueue.removeFirst();
            this.processingMobQueue.addLast(data);
            this.processingMobCounts.merge(data.type(), 1, Integer::sum);
        }

        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void finishProcessingBatch(ServerLevel level, BlockPos pos) {
        while (!this.processingMobQueue.isEmpty()) {
            CapturedMobData data = this.processingMobQueue.removeFirst();
            processSingleCapturedMob(level, pos, data);
        }

        this.processingMobCounts.clear();
        this.setChanged();
        if (this.level != null && !this.level.isClientSide()) {
            this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    private void processSingleCapturedMob(ServerLevel level, BlockPos pos, CapturedMobData capturedMob) {
        if (capturedMob.wasBaby()) {
            this.tank.fill(new FluidStack(EssenceExtractor.EXPERIENCE_FLUID.get(), BABY_MOB_XP_MB), IFluidHandler.FluidAction.EXECUTE);
            return;
        }

        if (!(capturedMob.type().create(level) instanceof LivingEntity living)) {
            return;
        }

        living.setPos(pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D);
        Player fakePlayer = FakePlayerFactory.get(level, EXTRACTOR_FAKE_PLAYER_PROFILE);

        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.THIS_ENTITY, living)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                .withParameter(LootContextParams.DAMAGE_SOURCE, level.damageSources().playerAttack(fakePlayer))
                .withParameter(LootContextParams.ATTACKING_ENTITY, fakePlayer)
                .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, fakePlayer)
                .create(LootContextParamSets.ENTITY);

        var lootTableKey = living.getLootTable();
        for (ItemStack stack : level.getServer().reloadableRegistries().getLootTable(lootTableKey).getRandomItems(params)) {
            int looting = getScaledUpgradeBonus(getLootingUpgradeLevel());
            if (looting > 0 && !stack.isEmpty()) {
                stack.grow(level.random.nextInt(looting + 1));
            }
            queueProcessedDrop(stack.copy());
        }

        if (living instanceof Mob mob) {
            int xp = mob.getExperienceReward(level, fakePlayer);
            if (xp > 0) {
                this.tank.fill(new FluidStack(EssenceExtractor.EXPERIENCE_FLUID.get(), xp * MB_PER_XP), IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    private List<Map.Entry<EntityType<?>, Integer>> getTopQueuedMobEntries() {
        return getQueuedMobCounts().entrySet().stream()
                .sorted(Comparator.<Map.Entry<EntityType<?>, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(CAPTURED_DISPLAY_COUNT)
                .toList();
    }

    private List<Map.Entry<EntityType<?>, Integer>> getTopProcessingMobEntries() {
        return this.processingMobCounts.entrySet().stream()
                .sorted(Comparator.<Map.Entry<EntityType<?>, Integer>>comparingInt(Map.Entry::getValue).reversed())
                .limit(CAPTURED_DISPLAY_COUNT)
                .toList();
    }

    public int getCapturedMobTypeRawId(int slot) {
        List<Map.Entry<EntityType<?>, Integer>> entries = getTopQueuedMobEntries();
        if (slot < 0 || slot >= entries.size()) {
            return -1;
        }
        return BuiltInRegistries.ENTITY_TYPE.getId(entries.get(slot).getKey());
    }

    public int getCapturedMobCount(int slot) {
        List<Map.Entry<EntityType<?>, Integer>> entries = getTopQueuedMobEntries();
        if (slot < 0 || slot >= entries.size()) {
            return 0;
        }
        return entries.get(slot).getValue();
    }

    public int getProcessingMobCount(int slot) {
        List<Map.Entry<EntityType<?>, Integer>> entries = getTopProcessingMobEntries();
        if (slot < 0 || slot >= entries.size()) {
            return 0;
        }
        return this.processingMobCounts.getOrDefault(entries.get(slot).getKey(), 0);
    }

    private Map<EntityType<?>, Integer> getQueuedMobCounts() {
        Map<EntityType<?>, Integer> queued = new HashMap<>();
        for (CapturedMobData data : this.capturedMobQueue) {
            queued.merge(data.type(), 1, Integer::sum);
        }
        return queued;
    }

    private static boolean hasEquipment(Mob mob) {
        if (!mob.getItemBySlot(EquipmentSlot.HEAD).isEmpty()
                || !mob.getItemBySlot(EquipmentSlot.CHEST).isEmpty()
                || !mob.getItemBySlot(EquipmentSlot.LEGS).isEmpty()
                || !mob.getItemBySlot(EquipmentSlot.FEET).isEmpty()
                || !mob.getItemBySlot(EquipmentSlot.BODY).isEmpty()) {
            return true;
        }
        return !mob.getMainHandItem().isEmpty() || !mob.getOffhandItem().isEmpty();
    }

    private void queueProcessedDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        ItemStack remaining = insertIntoMachine(stack);
        if (!remaining.isEmpty()) {
            bufferOverflowDrop(remaining);
        }
    }

    private void bufferOverflowDrop(ItemStack stack) {
        if (stack.isEmpty()) {
            return;
        }

        int freeUnits = OUTPUT_BUFFER_CAPACITY - getOutputBufferItemCount();
        if (freeUnits <= 0) {
            return;
        }

        int toStore = Math.min(freeUnits, stack.getCount());
        if (toStore <= 0) {
            return;
        }

        ItemStack pending = stack.copy();
        pending.setCount(toStore);

        for (ItemStack buffered : this.outputBuffer) {
            if (pending.isEmpty()) {
                break;
            }
            if (!ItemStack.isSameItemSameComponents(buffered, pending)) {
                continue;
            }
            int space = buffered.getMaxStackSize() - buffered.getCount();
            if (space <= 0) {
                continue;
            }
            int move = Math.min(space, pending.getCount());
            buffered.grow(move);
            pending.shrink(move);
        }

        while (!pending.isEmpty()) {
            int move = Math.min(pending.getMaxStackSize(), pending.getCount());
            ItemStack split = pending.copy();
            split.setCount(move);
            this.outputBuffer.add(split);
            pending.shrink(move);
        }

        this.setChanged();
    }

    private void flushOutputBufferToMachine() {
        if (this.outputBuffer.isEmpty()) {
            return;
        }

        boolean changed = false;
        ListIterator<ItemStack> iterator = this.outputBuffer.listIterator();
        while (iterator.hasNext()) {
            ItemStack buffered = iterator.next();
            if (buffered.isEmpty()) {
                iterator.remove();
                changed = true;
                continue;
            }

            ItemStack remaining = insertIntoMachine(buffered.copy());
            if (remaining.getCount() != buffered.getCount()) {
                changed = true;
            }
            if (remaining.isEmpty()) {
                iterator.remove();
            } else {
                iterator.set(remaining);
            }
        }

        if (changed) {
            this.setChanged();
            if (this.level != null && !this.level.isClientSide()) {
                this.level.sendBlockUpdated(this.worldPosition, getBlockState(), getBlockState(), 3);
            }
        }
    }

    private record CapturedMobData(EntityType<?> type, boolean wasBaby) {
    }

    private AABB buildCaptureAABB(BlockPos machinePos) {
        int centerX = machinePos.getX() + this.posX;
        int centerY = machinePos.getY() + this.posY;
        int centerZ = machinePos.getZ() + this.posZ;

        double minX = centerX - this.areaX;
        double minY = centerY - this.areaY;
        double minZ = centerZ - this.areaZ;
        double maxX = centerX + this.areaX + 1;
        double maxY = centerY + this.areaY + 1;
        double maxZ = centerZ + this.areaZ + 1;
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private void renderAreaOutline(ServerLevel level, BlockPos machinePos) {
        AABB box = buildCaptureAABB(machinePos);
        double minX = box.minX;
        double minY = box.minY;
        double minZ = box.minZ;
        double maxX = box.maxX;
        double maxY = box.maxY;
        double maxZ = box.maxZ;

        for (int i = 0; i < 8; i++) {
            double x = (i & 1) == 0 ? minX : maxX;
            double y = (i & 2) == 0 ? minY : maxY;
            double z = (i & 4) == 0 ? minZ : maxZ;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER, x, y, z, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.essenceextractor.essence_extractor");
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new com.essenceextractor.essenceextractormod.menu.EssenceExtractorMenu(containerId, playerInventory, this.worldPosition);
    }
}
