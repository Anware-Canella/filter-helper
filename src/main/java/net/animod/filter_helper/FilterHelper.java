package net.animod.filter_helper;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.GlowItemFrameEntity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FilterHelper {
    public static void init() {
        Main.registerCommand(FILTER_FILLER_COMMAND);
    }

    public static List<Item> itemList = new ArrayList<>();
    public static final List<Direction> XPerps = new ArrayList<>() {{
        add(Direction.UP);
        add(Direction.DOWN);
        add(Direction.NORTH);
        add(Direction.SOUTH);
    }};
    public static final List<Direction> ZPerps = new ArrayList<>() {{
        add(Direction.UP);
        add(Direction.DOWN);
        add(Direction.EAST);
        add(Direction.WEST);
    }};

    public static final LiteralArgumentBuilder<ServerCommandSource> FILTER_FILLER_COMMAND = CommandManager.literal("filters")
        .then(CommandManager.literal("print_list").executes(context -> {
            if (!context.getSource().isExecutedByPlayer()) {
                return -1;
            }
            PlayerEntity player = context.getSource().getPlayer();
            player.sendMessage(Text.literal("filtering Items:\n" + itemList).formatted(Formatting.LIGHT_PURPLE));
            return 1;
        }))
        .then(CommandManager.literal("read_blocks").executes(context -> {
            ServerWorld world = context.getSource().getWorld();
            PlayerEntity player = context.getSource().getPlayer();
            Direction direction = Direction.getEntityFacingOrder(player)[0];
            BlockPos pos = getPlayerHeadPos(player);
            BlockState state = world.getBlockState(pos);
            itemList = new ArrayList<>();
            while (!state.isAir()) {
                itemList.add(state.getBlock().asItem());
                state = world.getBlockState(pos = pos.offset(direction));
            }
            player.sendMessage(Text.literal(itemList.size() + " items in the list.").formatted(Formatting.GREEN));
            return 1;
        }))
        .then(CommandManager.literal("read_item_frames").executes(context -> {
            if (!context.getSource().isExecutedByPlayer()) return -1;
            ServerWorld world = context.getSource().getWorld();
            PlayerEntity player = context.getSource().getPlayer();
            Direction direction = Direction.getEntityFacingOrder(player)[0];
            BlockPos pos = getPlayerHeadPos(player);
            itemList = getFrameItems(world, pos, direction);
            context.getSource().getPlayer().sendMessage(Text.literal(itemList.size() + " items in the list.").formatted(Formatting.GREEN));
            return 1;
        }))
        .then(CommandManager.literal("place_item_frames").executes(context -> {
            if (itemList.isEmpty()) return -1;
            ServerWorld world = context.getSource().getWorld();
            PlayerEntity player = context.getSource().getPlayer();
            Direction direction = Direction.getEntityFacingOrder(player)[0];
            BlockPos pos = getPlayerHeadPos(player);
            Direction placeDirection = null;
            for (Direction testDirection : (direction.getAxis() == Direction.Axis.X ? XPerps : ZPerps)) if (placeFrame(world, pos, testDirection, new ItemStack(itemList.get(0)))) {
                placeDirection = testDirection;
                break;
            }
            if (placeDirection == null) {
                player.sendMessage(Text.literal("can't find a side to place item frames on.").formatted(Formatting.RED));
                return -1;
            }
            for (int i = 1; i < itemList.size(); i++) {
                pos = pos.offset(direction);
                if (!placeFrame(world, pos, placeDirection, new ItemStack(itemList.get(i)))) break;
            }
            player.sendMessage(Text.literal("item frames placed.").formatted(Formatting.GREEN));
            return 1;
        }))
        .then(CommandManager.literal("place_blocks").executes(context -> {
            if (itemList.isEmpty()) return -1;
            ServerWorld world = context.getSource().getWorld();
            PlayerEntity player = context.getSource().getPlayer();
            Direction direction = Direction.getEntityFacingOrder(player)[0];
            BlockPos pos = getPlayerHeadPos(player);
            int offset = 0;
            for (Item item : itemList) {
                if (!(item instanceof BlockItem blockItem)) break;
                world.setBlockState(pos.offset(direction, offset++), blockItem.getBlock().getDefaultState());
            }
            return 1;
        }))
        .then(CommandManager.literal("fill_hoppers").executes(context -> {
            if (itemList.isEmpty()) return -1;
            if (!context.getSource().isExecutedByPlayer()) return -1;
            ServerWorld world = context.getSource().getWorld();
            PlayerEntity player = context.getSource().getPlayer();
            Direction direction = Direction.getEntityFacingOrder(player)[0];
            BlockPos pos = getPlayerHeadPos(player);
            for (int i = 0; i < itemList.size(); i++) {
                if (!(world.getBlockEntity(pos.offset(direction, i)) instanceof HopperBlockEntity hopper)) break;
                hopper.setStack(0, new ItemStack(itemList.get(i)));
            }
            player.sendMessage(Text.literal("set listed items to hoppers.").formatted(Formatting.GREEN));
            return 1;
        }));

    public static BlockPos getPlayerHeadPos(PlayerEntity player) {
        return BlockPos.ofFloored(player.getPos().offset(Direction.UP, player.getEyeHeight(player.getPose())));
    }

    public static List<Item> getFrameItems(ServerWorld world, BlockPos pos, Direction direction) {
        List<ItemFrameEntity> frames = new ArrayList<>();
        for (Entity entity : world.iterateEntities()) {
            if (!(entity instanceof ItemFrameEntity frameEntity)) continue;
            BlockPos framePos = frameEntity.getBlockPos();
            if (direction.getAxis() == Direction.Axis.X) {
                if (direction == Direction.EAST) {
                    if (framePos.getX() < pos.getX()) continue;
                } else if (framePos.getX() > pos.getX()) continue;
                if (framePos.getY() != pos.getY() || framePos.getZ() != pos.getZ()) continue;
            }
            if (direction.getAxis() == Direction.Axis.Z) {
                if (direction == Direction.SOUTH) {
                    if (framePos.getZ() < pos.getZ()) continue;
                } else if (framePos.getZ() > pos.getZ()) continue;
                if (framePos.getY() != pos.getY() || framePos.getX() != pos.getX()) continue;
            }
            frames.add(frameEntity);
        }
        frames.sort(Comparator.comparingInt(frame -> switch (direction) {
            case NORTH -> -frame.getBlockPos().getZ();
            case SOUTH -> frame.getBlockPos().getZ();
            case EAST -> frame.getBlockPos().getX();
            case WEST -> -frame.getBlockPos().getX();
            default -> 0;
        }));
        List<Item> items = new ArrayList<>();
        int count = 0;
        ItemStack stack;
        for (ItemFrameEntity entity : frames) {
            Vec3i v = entity.getBlockPos().subtract(pos);
            int distance = Math.abs(v.getX() * direction.getOffsetX() + v.getZ() * direction.getOffsetZ());
            if (distance != count) break;
            count++;
            stack = entity.getHeldItemStack();
            if (stack == null || stack.isEmpty()) break;
            items.add(stack.getItem());
        }
        return items;
    }

    public static boolean placeFrame(ServerWorld world, BlockPos pos, Direction direction, ItemStack stack) {
        if (!world.getBlockState(pos.offset(direction)).isOf(Blocks.GRAY_CONCRETE)) return false;
        ItemFrameEntity frameEntity = new GlowItemFrameEntity(world, pos, direction.getOpposite());
        frameEntity.setHeldItemStack(stack, true);
        frameEntity.setInvisible(true);
        if (!frameEntity.canStayAttached()) return false;
        return world.spawnEntity(frameEntity);
    }
}
