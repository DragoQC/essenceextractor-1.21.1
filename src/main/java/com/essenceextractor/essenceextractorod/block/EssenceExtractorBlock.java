package com.essenceextractor.essenceextractormod.block;

import com.essenceextractor.essenceextractormod.EssenceExtractor;
import com.essenceextractor.essenceextractormod.blockentity.EssenceExtractorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class EssenceExtractorBlock extends Block implements EntityBlock {
    public EssenceExtractorBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EssenceExtractorBlockEntity essenceExtractorBlockEntity) {
                if (essenceExtractorBlockEntity.handleBucketInteraction(player, hand)) {
                    return ItemInteractionResult.CONSUME;
                }

                player.openMenu(essenceExtractorBlockEntity, pos);
                return ItemInteractionResult.CONSUME;
            }
        }

        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide()) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof EssenceExtractorBlockEntity essenceExtractorBlockEntity) {
                player.openMenu(essenceExtractorBlockEntity, pos);
            }
            return InteractionResult.CONSUME;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new EssenceExtractorBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
        if (level.isClientSide() || blockEntityType != EssenceExtractor.ESSENCE_EXTRACTOR_BLOCK_ENTITY.get()) {
            return null;
        }

        return (tickLevel, tickPos, tickState, blockEntity) -> EssenceExtractorBlockEntity.serverTick(tickLevel, tickPos, tickState, (EssenceExtractorBlockEntity) blockEntity);
    }
}
