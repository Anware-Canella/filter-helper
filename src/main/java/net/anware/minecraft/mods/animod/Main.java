package net.anware.minecraft.mods.animod;

import net.anware.minecraft.mods.animod.feature.storage_commands.StorageCommand;
import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Main implements ModInitializer {

	public static final String MOD_ID = "filter_helper";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		StorageCommand.load();
	}
}