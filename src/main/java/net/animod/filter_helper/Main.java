package net.animod.filter_helper;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.server.command.ServerCommandSource;

import java.util.logging.Logger;

public class Main implements ModInitializer {
	
	public static final String ID = "modid";
	public static final Logger LOGGER = Logger.getLogger(ID);
	
	@Override
	public void onInitialize() {
		FilterHelper.init();
	}
	
	public static void log(String msg) {
		LOGGER.info(msg);
	}

	public static void registerCommand(final LiteralArgumentBuilder<ServerCommandSource> command) {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(command));
	}
}