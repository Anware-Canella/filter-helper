package net.anware.minecraft.mods.animod.feature.storage_commands;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.anware.minecraft.mods.animod.util.RegistryUtil;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockArgumentParser;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.ItemStackArgument;
import net.minecraft.command.argument.ItemStackArgumentType;
import net.minecraft.component.ComponentMap;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Pair;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;
import net.minecraft.world.GameRules;

import java.util.*;

public class StorageCommand {

    public static void load() {
        RegistryUtil.registerCommand(createFilterCommand());
        RegistryUtil.registerCommand(createContainerCommand());
        RegistryUtil.registerCommand(StorageCommand::createSumCommand);
    }

    public static final GameRules.Key<GameRules.BooleanRule> DO_CONTAINER_DROP = RegistryUtil.gamerule("doContainerDrop", GameRules.Category.PLAYER, GameRuleFactory.createBooleanRule(true));

    // filter command

    protected static final Map<Integer, List<Item>> ITEM_LISTS = new HashMap<>();
    public static final Block PREFERED_BASE_BLOCK = Blocks.SEA_LANTERN;
    protected static final String INDEX_ID = "index";
    public static final int MAX_SEARCH_DEPTH = 200;

    public static final LiteralArgumentBuilder<ServerCommandSource> createFilterCommand() {
        return CommandManager.literal("filter")
                .then(CommandManager.literal("print")
                        .then(CommandManager.argument(INDEX_ID, IntegerArgumentType.integer(0)).executes(commandContext -> {
                            if (!commandContext.getSource().isExecutedByPlayer()) {
                                return -1;
                            }
                            PlayerEntity player = commandContext.getSource().getPlayer();
                            int index = IntegerArgumentType.getInteger(commandContext, INDEX_ID);
                            printItems(player, index);
                            return 1;
                        }))
                )
                .then(CommandManager.literal("scan")
                        .then(CommandManager.literal("blocks")
                                .then(CommandManager.argument(INDEX_ID, IntegerArgumentType.integer(0)).executes(commandContext -> {
                                    if (!commandContext.getSource().isExecutedByPlayer()) {
                                        return -1;
                                    }
                                    ServerWorld world = commandContext.getSource().getWorld();
                                    PlayerEntity player = commandContext.getSource().getPlayer();
                                    int index = IntegerArgumentType.getInteger(commandContext, INDEX_ID);
                                    scanBlocks(world, player, index);
                                    return 1;
                                }))
                        )
                        .then(CommandManager.literal("frames")
                                .then(CommandManager.argument(INDEX_ID, IntegerArgumentType.integer(0)).executes(commandContext -> {
                                    if (!commandContext.getSource().isExecutedByPlayer()) {
                                        return -1;
                                    }
                                    ServerWorld world = commandContext.getSource().getWorld();
                                    PlayerEntity player = commandContext.getSource().getPlayer();
                                    int index = IntegerArgumentType.getInteger(commandContext, INDEX_ID);
                                    scanFrames(world, player, index);
                                    return 1;
                                }))
                        )
                )
                .then(CommandManager.literal("place")
                        .then(CommandManager.literal("blocks")
                                .then(CommandManager.argument(INDEX_ID, IntegerArgumentType.integer(0)).executes(commandContext -> {
                                    if (!commandContext.getSource().isExecutedByPlayer()) {
                                        return -1;
                                    }
                                    ServerWorld world = commandContext.getSource().getWorld();
                                    PlayerEntity player = commandContext.getSource().getPlayer();
                                    int index = IntegerArgumentType.getInteger(commandContext, INDEX_ID);
                                    placeBlocks(world, player, index);
                                    return 1;
                                }))
                        )
                        .then(CommandManager.literal("frames")
                                .then(CommandManager.argument(INDEX_ID, IntegerArgumentType.integer(0)).executes(commandContext -> {
                                    if (!commandContext.getSource().isExecutedByPlayer()) {
                                        return -1;
                                    }
                                    ServerWorld world = commandContext.getSource().getWorld();
                                    PlayerEntity player = commandContext.getSource().getPlayer();
                                    int index = IntegerArgumentType.getInteger(commandContext, INDEX_ID);
                                    placeFrames(world, player, index);
                                    return 1;
                                }))
                        )
                        .then(CommandManager.literal("hoppers")
                                .then(CommandManager.argument(INDEX_ID, IntegerArgumentType.integer(0)).executes(commandContext -> {
                                    if (!commandContext.getSource().isExecutedByPlayer()) {
                                        return -1;
                                    }
                                    ServerWorld world = commandContext.getSource().getWorld();
                                    PlayerEntity player = commandContext.getSource().getPlayer();
                                    int index = IntegerArgumentType.getInteger(commandContext, INDEX_ID);
                                    placeHoppers(world, player, index);
                                    return 1;
                                }))
                        )
                );
    }

    public static BlockPos getPlayerHeadPos(PlayerEntity player) {
        return BlockPos.ofFloored(player.getPos().offset(Direction.UP, player.getEyeHeight(player.getPose())));
    }

    public static Direction getHorizontalDirection(Vec3i vec) {
        // don't use this on vec = (0,0,0) :/
        if (vec.getY() != 0) {
            return null;
        }
        if (vec.getX() == 0) {
            return vec.getZ() > 0 ? Direction.SOUTH : Direction.NORTH;
        }
        else if (vec.getZ() == 0) {
            return vec.getX() > 0 ? Direction.EAST : Direction.WEST;
        }
        return null;
    }

    public static boolean isZero(Vec3i vec) {
        return vec.getX() == 0 && vec.getY() == 0 && vec.getZ() == 0;
    }

    public static int dot(Vec3i vec1, Vec3i vec2) {
        return vec1.getX() * vec2.getX() + vec1.getY() * vec2.getY() + vec1.getZ() * vec2.getZ();
    }

    public static boolean placeFrame(ServerWorld world, BlockPos pos, Direction direction, Item item) {
        // direction is the direction of the offset, opposite to the direction the frame will be facing
        if (!world.getBlockState(pos.offset(direction)).isOf(PREFERED_BASE_BLOCK)) {
            return false;
        }
        ItemFrameEntity frameEntity = new GlowItemFrameEntity(world, pos, direction.getOpposite());
        frameEntity.setHeldItemStack(new ItemStack(item), true);
        frameEntity.setInvisible(true);
        if (!frameEntity.canStayAttached()) return false;
        return world.spawnEntity(frameEntity);
    }
    
    public static void printItems(PlayerEntity player, int index) {
        StringBuilder str = new StringBuilder("items for [ " + index + " ]:\n[");
        List<Item> items = ITEM_LISTS.get(index);
        if (items == null || items.isEmpty()) {
            player.sendMessage(Text.literal("no items in list [ " + index + " ]").formatted(Formatting.RED));
            return;
        }
        str.append(items.getFirst().toString());
        for (int i = 1; i < items.size(); i++) {
            str.append(", ").append(items.get(i).toString());
        }
        str.append("]");
        player.sendMessage(Text.literal(str.toString()).formatted(Formatting.LIGHT_PURPLE));
    }

    public static void scanBlocks(ServerWorld world, PlayerEntity player, int index) {
        Direction direction = Direction.getEntityFacingOrder(player)[0];
        BlockPos pos = getPlayerHeadPos(player);
        BlockState state = world.getBlockState(pos);
        List<Item> list = new ArrayList<>();
        int offset = 1;
        while (!state.isAir()) {
            list.add(state.getBlock().asItem());
            state = world.getBlockState(pos.offset(direction, offset++));
            if (offset >= MAX_SEARCH_DEPTH) {
                break;
            }
        }
        if (list.isEmpty()) {
            player.sendMessage(Text.literal("no item frames found").formatted(Formatting.RED));
        }
        else {
            ITEM_LISTS.put(index, list);
            player.sendMessage(Text.literal(list.size() + " items in list [ " + index + " ]").formatted(Formatting.GREEN));
        }
    }

    public static void scanFrames(ServerWorld world, PlayerEntity player, int index) {
        Direction direction = Direction.getEntityFacingOrder(player)[0];
        BlockPos pos = getPlayerHeadPos(player);
        List<Pair<ItemFrameEntity, Integer>> entities = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ItemFrameEntity itemFrameEntity)) {
                continue;
            }
            Vec3i vec = itemFrameEntity.getBlockPos().subtract(pos);
            if (!isZero(vec) && getHorizontalDirection(vec) != direction) {
                continue;
            }
            entities.add(new Pair<>(itemFrameEntity, Math.abs(vec.getComponentAlongAxis(direction.getAxis()))));
        }
        entities.sort(Comparator.comparingInt(Pair::getRight));
        List<Item> list = new ArrayList<>();
        int offset = 0;
        for (Pair<ItemFrameEntity, Integer> p : entities) {
            if (p.getRight() != offset) {
                break;
            }
            offset++;
            ItemStack stack = p.getLeft().getHeldItemStack();
            if (stack == null || stack.isEmpty()) {
                break;
            }
            list.add(stack.getItem());
        }
        if (list.isEmpty()) {
            player.sendMessage(Text.literal("no item frames found").formatted(Formatting.RED));
        }
        else {
            player.sendMessage(Text.literal(list.size() + " items in list [ " + index + " ]").formatted(Formatting.GREEN));
            ITEM_LISTS.put(index, list);
        }
    }

    public static void placeBlocks(ServerWorld world, PlayerEntity player, int index) {
        List<Item> list = ITEM_LISTS.get(index);
        if (list == null || list.isEmpty()) {
            player.sendMessage(Text.literal("no items in list [ " + index + " ]").formatted(Formatting.RED));
            return;
        }
        BlockPos pos = getPlayerHeadPos(player);
        Direction direction = Direction.getEntityFacingOrder(player)[0];
        int offset = 0;
        int cnt = 0;
        for (Item item : list) {
            if (item instanceof BlockItem blockItem) {
                world.setBlockState(pos.offset(direction, offset), blockItem.getBlock().getDefaultState());
                cnt++;
            }
            offset++;
        }
        player.sendMessage(Text.literal(cnt + " blocks placed.").formatted(Formatting.GREEN));
    }

    public static void placeFrames(ServerWorld world, PlayerEntity player, int index) {
        List<Item> list = ITEM_LISTS.get(index);
        if (list == null || list.isEmpty()) {
            player.sendMessage(Text.literal("no items in list [ " + index + " ]").formatted(Formatting.RED));
            return;
        }
        BlockPos pos = getPlayerHeadPos(player);
        Direction direction = Direction.getEntityFacingOrder(player)[0];
        Direction placeDirection = null;
        for (Direction testDirection : Direction.values()) {
            if (dot(testDirection.getVector(), direction.getVector()) != 0) {
                continue;
            }
            if (world.getBlockState(pos.offset(testDirection)).isOf(PREFERED_BASE_BLOCK)) {
                placeDirection = testDirection;
                break;
            }
        }
        if (placeDirection == null) {
            player.sendMessage(Text.literal("can't find a side to place item frames on.").formatted(Formatting.RED));
            return;
        }
        int offset = 0;
        int cnt = 0;
        for (Item item : list) {
            if (placeFrame(world, pos.offset(direction, offset), placeDirection, item)) {
                cnt++;
            }
            offset++;
        }
        player.sendMessage(Text.literal(cnt + "frames placed.").formatted(Formatting.GREEN));
    }

    public static void placeHoppers(ServerWorld world, PlayerEntity player, int index) {
        List<Item> list = ITEM_LISTS.get(index);
        if (list == null || list.isEmpty()) {
            player.sendMessage(Text.literal("no items in list [ " + index + " ]").formatted(Formatting.RED));
            return;
        }
        BlockPos pos = getPlayerHeadPos(player);
        Direction direction = Direction.getEntityFacingOrder(player)[0];
        int offset = 0;
        int cnt = 0;
        for (Item item : list) {
            if (world.getBlockEntity(pos.offset(direction, offset)) instanceof HopperBlockEntity hopperBlockEntity) {
                hopperBlockEntity.setStack(0, new ItemStack(item, 1));
                cnt++;
            }
            offset++;
        }
        player.sendMessage(Text.literal(cnt + " hoppers set.").formatted(Formatting.GREEN));
    }

    // container command

    public static LiteralArgumentBuilder<ServerCommandSource> createContainerCommand() {
        return CommandManager.literal("container")
                .then(CommandManager.literal("clear")
                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("to", BlockPosArgumentType.blockPos()).executes(commandContext -> {
                                    if (commandContext.getSource().isExecutedByPlayer()) {
                                        return -1;
                                    }
                                    ServerWorld world = commandContext.getSource().getWorld();
                                    ServerPlayerEntity player = commandContext.getSource().getPlayer();
                                    BlockPos pos1 = BlockPosArgumentType.getBlockPos(commandContext, "from");
                                    BlockPos pos2 = BlockPosArgumentType.getBlockPos(commandContext, "to");
                                    int count = 0;
                                    for (BlockPos pos : BlockPos.iterate(pos1, pos2)) {
                                        if (world.getBlockEntity(pos) instanceof Inventory inventory) {
                                            inventory.clear();
                                            count++;
                                        }
                                    }
                                    player.sendMessage(Text.literal(count + " containers cleared."));
                                    return 1;
                                }))
                        )
                );
    }

    // sum command

    public static LiteralArgumentBuilder<ServerCommandSource> createSumCommand(CommandRegistryAccess access) {
        return CommandManager.literal("sum")
                .then(CommandManager.literal("count")
                        .then(CommandManager.argument("from", BlockPosArgumentType.blockPos())
                                .then(CommandManager.argument("to", BlockPosArgumentType.blockPos()).executes(commandContext -> {
                                    return 1;
                                }))
                        )
                )
                .then(CommandManager.literal("give")
                        .then(CommandManager.argument("stack", ItemStackArgumentType.itemStack(access))
                                .then(CommandManager.argument("count", IntegerArgumentType.integer(0))
                                        .then(CommandManager.argument("combine", BoolArgumentType.bool()).executes(commandContext -> {
                                            if (!commandContext.getSource().isExecutedByPlayer()) {
                                                return -1;
                                            }
                                            ServerPlayerEntity player = commandContext.getSource().getPlayer();
                                            ItemStackArgument stackArg = ItemStackArgumentType.getItemStackArgument(commandContext, "stack");
                                            int count = IntegerArgumentType.getInteger(commandContext, "count");
                                            boolean combine = BoolArgumentType.getBool(commandContext, "combine");
                                            giveSum(player, stackArg.getItem(), count, combine);
                                            return 1;
                                        }))
                                )
                        )
                );
    }

    public static void giveSum(ServerPlayerEntity player, Item item, int count, boolean combine) {
        List<ItemStack> givenStacks = new ArrayList<>();
        if (combine) {
            int shulkerMax = 27;
            int stackMax = item.getMaxCount();
            int stacks = count / stackMax;
            int single = count % stackMax;
            int shulkers = stacks / shulkerMax;
            stacks = stacks % shulkerMax;
            ItemStack shulkerStackProt = createShulkerStack(item);
            for (int i = 0; i < shulkers; i++) {
                givenStacks.add(shulkerStackProt.copy());
            }
            for (int i = 0; i < stacks; i++) {
                givenStacks.add(new ItemStack(item, stackMax));
            }
            givenStacks.add(new ItemStack(item, single));
        } else {
            int stackMax = item.getMaxCount();
            int stacks = count / stackMax;
            int single = stacks % stackMax;
            for (int i = 0; i < stacks; i++) {
                givenStacks.add(new ItemStack(item, stackMax));
            }
            givenStacks.add(new ItemStack(item, single));
        }
        for (ItemStack stack : givenStacks) {
            givePlayer(player, stack);
        }
    }

    public static void givePlayer(ServerPlayerEntity player, ItemStack stack) {
        boolean bl = player.getInventory().insertStack(stack);
        if (bl && stack.isEmpty()) {
            ItemEntity itemEntity = player.dropItem(stack, false);
            if (itemEntity != null) {
                itemEntity.setDespawnImmediately();
            }
            player.getWorld().playSound(
                    null,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    SoundEvents.ENTITY_ITEM_PICKUP,
                    SoundCategory.PLAYERS,
                    0.2F,
                    ((player.getRandom().nextFloat() - player.getRandom().nextFloat()) * 0.7F + 1.0F) * 2.0F
            );
            player.currentScreenHandler.sendContentUpdates();
        } else {
            ItemEntity itemEntity = player.dropItem(stack, false);
            if (itemEntity != null) {
                itemEntity.resetPickupDelay();
                itemEntity.setOwner(player.getUuid());
            }
        }
    }

    public static ItemStack createShulkerStack(Item item) {
        ItemStack stack = new ItemStack(Items.WHITE_SHULKER_BOX);
        List<ItemStack> stacks = new ArrayList<>(27);
        for (int i = 0; i < 27; i++) {
            stacks.add(new ItemStack(item, item.getMaxCount()));
        }
        ComponentMap.Builder builder = ComponentMap.builder();
        builder.add(DataComponentTypes.CONTAINER, ContainerComponent.fromStacks(stacks));
        stack.applyComponentsFrom(builder.build());
        return stack;
    }
}
