package com.essenceextractor.essenceextractormod.client.render;

import com.essenceextractor.essenceextractormod.EssenceExtractor;
import com.essenceextractor.essenceextractormod.blockentity.EssenceExtractorBlockEntity;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;

import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

public final class CaptureAreaForcefieldRenderer {
    private static final ResourceLocation FORCEFIELD_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            EssenceExtractor.MODID,
            "textures/environnement/forcefield.png");
    private static final int FLOW_CYCLE_MS = 3_000;
    private static final int HUE_CYCLE_MS = 10_000;
    private static final int CHUNK_SCAN_PADDING = 1;
    private static final double FRUSTUM_PADDING = 0.05D;
    private static final float COLOR_ALPHA = 0.34F;

    private CaptureAreaForcefieldRenderer() {
    }

    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_PARTICLES) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        renderVisibleExtractorAreas(minecraft, event, event.getCamera().getPosition());
    }

    private static void renderVisibleExtractorAreas(Minecraft minecraft, RenderLevelStageEvent event, Vec3 cameraPos) {
        ClientChunkCache chunkSource = minecraft.level.getChunkSource();
        int renderDistance = minecraft.options.getEffectiveRenderDistance() + CHUNK_SCAN_PADDING;
        int centerChunkX = SectionPos.blockToSectionCoord(Mth.floor(cameraPos.x));
        int centerChunkZ = SectionPos.blockToSectionCoord(Mth.floor(cameraPos.z));

        for (int chunkX = centerChunkX - renderDistance; chunkX <= centerChunkX + renderDistance; chunkX++) {
            for (int chunkZ = centerChunkZ - renderDistance; chunkZ <= centerChunkZ + renderDistance; chunkZ++) {
                LevelChunk chunk = chunkSource.getChunk(chunkX, chunkZ, ChunkStatus.FULL, false);
                if (chunk == null) {
                    continue;
                }

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (!(blockEntity instanceof EssenceExtractorBlockEntity extractor) || !extractor.isShowArea()) {
                        continue;
                    }

                    AABB captureAABB = extractor.getCaptureAABB();
                    if (!event.getFrustum().isVisible(captureAABB.inflate(FRUSTUM_PADDING))) {
                        continue;
                    }

                    renderForcefieldBox(cameraPos, captureAABB);
                }
            }
        }
    }

    private static void renderForcefieldBox(Vec3 cameraPos, AABB box) {
        float minX = (float) (box.minX - cameraPos.x);
        float minY = (float) (box.minY - cameraPos.y);
        float minZ = (float) (box.minZ - cameraPos.z);
        float maxX = (float) (box.maxX - cameraPos.x);
        float maxY = (float) (box.maxY - cameraPos.y);
        float maxZ = (float) (box.maxZ - cameraPos.z);

        float flowTime = (float) (Util.getMillis() % FLOW_CYCLE_MS) / FLOW_CYCLE_MS;
        float vTop = (float) (-Mth.frac(cameraPos.y * 0.5D));
        float vBottom = vTop + ((maxY - minY) * 0.5F);
        float hue = (float) (Util.getMillis() % HUE_CYCLE_MS) / HUE_CYCLE_MS;
        int pastelRgb = Mth.hsvToRgb(hue, 0.28F, 1.00F);
        float colorR = (float) ((pastelRgb >> 16) & 0xFF) / 255.0F;
        float colorG = (float) ((pastelRgb >> 8) & 0xFF) / 255.0F;
        float colorB = (float) (pastelRgb & 0xFF) / 255.0F;

        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderTexture(0, FORCEFIELD_TEXTURE);
        RenderSystem.depthMask(Minecraft.useShaderTransparency());
        RenderSystem.setShaderColor(colorR, colorG, colorB, COLOR_ALPHA);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.polygonOffset(-3.0F, -3.0F);
        RenderSystem.enablePolygonOffset();
        RenderSystem.disableCull();

        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        addWallAlongZ(buffer, minX, minY, maxY, minZ, maxZ, flowTime, vTop, vBottom, false);
        addWallAlongZ(buffer, maxX, minY, maxY, minZ, maxZ, flowTime, vTop, vBottom, true);
        addWallAlongX(buffer, minZ, minY, maxY, minX, maxX, flowTime, vTop, vBottom, true);
        addWallAlongX(buffer, maxZ, minY, maxY, minX, maxX, flowTime, vTop, vBottom, false);
        addHorizontalFace(buffer, minY, minX, maxX, minZ, maxZ, flowTime, false);
        addHorizontalFace(buffer, maxY, minX, maxX, minZ, maxZ, flowTime, true);

        MeshData meshData = buffer.build();
        if (meshData != null) {
            BufferUploader.drawWithShader(meshData);
        }

        RenderSystem.enableCull();
        RenderSystem.polygonOffset(0.0F, 0.0F);
        RenderSystem.disablePolygonOffset();
        RenderSystem.disableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.depthMask(true);
    }

    private static void addWallAlongZ(
            BufferBuilder buffer,
            float x,
            float minY,
            float maxY,
            float minZ,
            float maxZ,
            float flowTime,
            float vTop,
            float vBottom,
            boolean positiveU) {
        float z = minZ;
        float uScroll = 0.0F;
        while (z < maxZ) {
            float segment = Math.min(1.0F, maxZ - z);
            float u1 = flowTime + (positiveU ? uScroll : -uScroll);
            float u2 = flowTime + (positiveU ? (uScroll + segment * 0.5F) : -(uScroll + segment * 0.5F));
            float z2 = z + segment;

            buffer.addVertex(x, minY, z).setUv(u1, vBottom);
            buffer.addVertex(x, minY, z2).setUv(u2, vBottom);
            buffer.addVertex(x, maxY, z2).setUv(u2, vTop);
            buffer.addVertex(x, maxY, z).setUv(u1, vTop);

            z = z2;
            uScroll += segment * 0.5F;
        }
    }

    private static void addWallAlongX(
            BufferBuilder buffer,
            float z,
            float minY,
            float maxY,
            float minX,
            float maxX,
            float flowTime,
            float vTop,
            float vBottom,
            boolean positiveU) {
        float x = minX;
        float uScroll = 0.0F;
        while (x < maxX) {
            float segment = Math.min(1.0F, maxX - x);
            float u1 = flowTime + (positiveU ? uScroll : -uScroll);
            float u2 = flowTime + (positiveU ? (uScroll + segment * 0.5F) : -(uScroll + segment * 0.5F));
            float x2 = x + segment;

            buffer.addVertex(x, minY, z).setUv(u1, vBottom);
            buffer.addVertex(x2, minY, z).setUv(u2, vBottom);
            buffer.addVertex(x2, maxY, z).setUv(u2, vTop);
            buffer.addVertex(x, maxY, z).setUv(u1, vTop);

            x = x2;
            uScroll += segment * 0.5F;
        }
    }

    private static void addHorizontalFace(
            BufferBuilder buffer,
            float y,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float flowTime,
            boolean topFace) {
        float u1 = flowTime + (minX * 0.5F);
        float u2 = flowTime + (maxX * 0.5F);
        float v1 = flowTime + (minZ * 0.5F);
        float v2 = flowTime + (maxZ * 0.5F);

        if (topFace) {
            buffer.addVertex(minX, y, minZ).setUv(u1, v1);
            buffer.addVertex(maxX, y, minZ).setUv(u2, v1);
            buffer.addVertex(maxX, y, maxZ).setUv(u2, v2);
            buffer.addVertex(minX, y, maxZ).setUv(u1, v2);
            return;
        }

        buffer.addVertex(minX, y, maxZ).setUv(u1, v2);
        buffer.addVertex(maxX, y, maxZ).setUv(u2, v2);
        buffer.addVertex(maxX, y, minZ).setUv(u2, v1);
        buffer.addVertex(minX, y, minZ).setUv(u1, v1);
    }
}
