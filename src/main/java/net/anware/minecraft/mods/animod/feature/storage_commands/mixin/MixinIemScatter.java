package net.anware.minecraft.mods.animod.feature.storage_commands.mixin;

import net.anware.minecraft.mods.animod.feature.storage_commands.StorageCommand;
import net.minecraft.block.BlockState;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemScatterer.class)
public class MixinIemScatter {

    @Inject(method = "onStateReplaced", at = @At("HEAD"), cancellable = true)
    private static void cancelDroppingItems(BlockState state, BlockState newState, World world, BlockPos pos, CallbackInfo ci) {
        if (!world.getGameRules().getBoolean(StorageCommand.DO_CONTAINER_DROP)) {
            ci.cancel();
        }
    }
}