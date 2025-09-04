package net.anware.minecraft.mods.animod.util;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.world.GameRules;

public class RegistryUtil {

    public static <O extends GameRules.Rule<O>> GameRules.Key<O> gamerule(String id, GameRules.Category category, GameRules.Type<O> type) {
        return GameRuleRegistry.register(id, category, type);
    }

    public static void registerCommand(final LiteralArgumentBuilder<ServerCommandSource> cmd) {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(cmd)));
    }

    @FunctionalInterface
    public interface CommandRegister {
        LiteralArgumentBuilder<ServerCommandSource> create(CommandRegistryAccess access);
    }

    public static void registerCommand(CommandRegister cmd) {
        CommandRegistrationCallback.EVENT.register(((commandDispatcher, commandRegistryAccess, registrationEnvironment) -> commandDispatcher.register(cmd.create(commandRegistryAccess))));
    }
}
