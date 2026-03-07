package com.essenceextractor.essenceextractormod;

import org.slf4j.Logger;

import com.essenceextractor.essenceextractormod.block.EssenceExtractorBlock;
import com.essenceextractor.essenceextractormod.blockentity.EssenceExtractorBlockEntity;
import com.essenceextractor.essenceextractormod.fluid.ExperienceFluid;
import com.essenceextractor.essenceextractormod.fluid.ExperienceFluidType;
import com.essenceextractor.essenceextractormod.menu.EssenceExtractorMenu;
import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

@Mod(EssenceExtractor.MODID)
public class EssenceExtractor {
    public static final String MODID = "essenceextractor";
    public static final Logger LOGGER = LogUtils.getLogger();

    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = DeferredRegister.create(Registries.MENU, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES = DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, MODID);
    public static final DeferredRegister<FluidType> FLUID_TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, MODID);
    public static final DeferredRegister<net.minecraft.world.level.material.Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, MODID);

    public static final DeferredHolder<FluidType, FluidType> EXPERIENCE_FLUID_TYPE = FLUID_TYPES.register(
            "experience",
            () -> new ExperienceFluidType(FluidType.Properties.create().descriptionId("fluid_type.essenceextractor.experience").density(1200).viscosity(1800)));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> EXPERIENCE_FLUID = FLUIDS.register(
            "experience",
            () -> new ExperienceFluid.Source(createExperienceFluidProperties()));

    public static final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> FLOWING_EXPERIENCE_FLUID = FLUIDS.register(
            "flowing_experience",
            () -> new ExperienceFluid.Flowing(createExperienceFluidProperties()));

    public static final DeferredBlock<LiquidBlock> EXPERIENCE_FLUID_BLOCK = BLOCKS.register(
            "experience",
            id -> new LiquidBlock(
                    EXPERIENCE_FLUID.get(),
                    BlockBehaviour.Properties.ofFullCopy(Blocks.WATER)
                            .replaceable()
                            .liquid()
                            .noLootTable()));

    public static final DeferredItem<BucketItem> EXPERIENCE_BUCKET = ITEMS.register(
            "experience_bucket",
            id -> new BucketItem(
                    EXPERIENCE_FLUID.get(),
                    new Item.Properties()
                            .craftRemainder(Items.BUCKET)
                            .stacksTo(1)));

    public static final DeferredHolder<MenuType<?>, MenuType<EssenceExtractorMenu>> ESSENCE_EXTRACTOR_MENU = MENU_TYPES.register(
            "essence_extractor",
            () -> IMenuTypeExtension.create((containerId, inventory, data) -> new EssenceExtractorMenu(containerId, inventory, data.readBlockPos())));

    public static final DeferredBlock<Block> ESSENCE_EXTRACTOR_BLOCK = BLOCKS.register(
            "essence_extractor",
            id -> new EssenceExtractorBlock(
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_BLACK).strength(4.0F).requiresCorrectToolForDrops()));

    public static final DeferredItem<BlockItem> ESSENCE_EXTRACTOR_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("essence_extractor", ESSENCE_EXTRACTOR_BLOCK);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<EssenceExtractorBlockEntity>> ESSENCE_EXTRACTOR_BLOCK_ENTITY = BLOCK_ENTITY_TYPES.register(
            "essence_extractor",
            () -> BlockEntityType.Builder.of(EssenceExtractorBlockEntity::new, ESSENCE_EXTRACTOR_BLOCK.get()).build(null));

    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> ESSENCE_EXTRACTOR_TAB = CREATIVE_MODE_TABS.register("main_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.essenceextractor"))
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> ESSENCE_EXTRACTOR_BLOCK_ITEM.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(ESSENCE_EXTRACTOR_BLOCK_ITEM.get());
                output.accept(EXPERIENCE_BUCKET.get());
            })
            .build());

    public EssenceExtractor(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::registerCapabilities);

        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
        CREATIVE_MODE_TABS.register(modEventBus);
        MENU_TYPES.register(modEventBus);
        BLOCK_ENTITY_TYPES.register(modEventBus);
        FLUID_TYPES.register(modEventBus);
        FLUIDS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);

        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerCapabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(
                Capabilities.EnergyStorage.BLOCK,
                ESSENCE_EXTRACTOR_BLOCK_ENTITY.get(),
                (blockEntity, side) -> blockEntity.getEnergyStorage());
    }

    private static BaseFlowingFluid.Properties createExperienceFluidProperties() {
        return new BaseFlowingFluid.Properties(EXPERIENCE_FLUID_TYPE, EXPERIENCE_FLUID, FLOWING_EXPERIENCE_FLUID)
                .bucket(EXPERIENCE_BUCKET)
                .block(EXPERIENCE_FLUID_BLOCK);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        LOGGER.info("HELLO FROM COMMON SETUP");

        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());
        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        ServerSettings.load();
        LOGGER.info("Essence Extractor server settings loaded: minCaptureTicks={}, minProcessPercent={}",
                ServerSettings.getMinCaptureTicks(),
                ServerSettings.getMinProcessPercent());
        LOGGER.info("HELLO from server starting");
    }
}
