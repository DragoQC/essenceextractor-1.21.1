package com.essenceextractor.essenceextractormod;

import com.essenceextractor.essenceextractormod.client.screen.EssenceExtractorScreen;

import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.client.gui.ConfigurationScreen;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

@Mod(value = EssenceExtractor.MODID, dist = Dist.CLIENT)
public class EssenceExtractorClient {
    public EssenceExtractorClient(IEventBus modEventBus, ModContainer container) {
        container.registerExtensionPoint(IConfigScreenFactory.class, ConfigurationScreen::new);
        modEventBus.addListener(this::registerScreens);
        modEventBus.addListener(this::registerClientExtensions);
    }

    private void registerScreens(RegisterMenuScreensEvent event) {
        event.register(EssenceExtractor.ESSENCE_EXTRACTOR_MENU.get(), EssenceExtractorScreen::new);
    }

    private void registerClientExtensions(RegisterClientExtensionsEvent event) {
        // Keep fluid textures centralized so tank rendering and world blocks stay visually consistent.
        event.registerFluidType(new IClientFluidTypeExtensions() {
            private static final ResourceLocation STILL = ResourceLocation.fromNamespaceAndPath(EssenceExtractor.MODID, "block/experience_still");
            private static final ResourceLocation FLOWING = ResourceLocation.fromNamespaceAndPath(EssenceExtractor.MODID, "block/experience_flow");

            @Override
            public ResourceLocation getStillTexture() {
                return STILL;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING;
            }

            @Override
            public int getTintColor() {
                return 0xFF6AFF2E;
            }
        }, EssenceExtractor.EXPERIENCE_FLUID_TYPE.get());
    }
}
