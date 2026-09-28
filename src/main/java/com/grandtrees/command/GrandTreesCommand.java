package com.grandtrees.command;

import com.grandtrees.template.TreeTemplates;
import com.grandtrees.tree.GrandTreeGrower;
import com.grandtrees.tree.TreeSize;
import com.grandtrees.tree.TreeSpecies;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.LongArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;

/**
 * /grandtrees place &lt;pos&gt; &lt;species&gt; &lt;size&gt; [seed] - grows a tree instantly (ignores obstructions).
 * /grandtrees templates - shows how many .nbt templates are loaded.
 */
public final class GrandTreesCommand {
    private static final SimpleCommandExceptionType BAD_SPECIES = new SimpleCommandExceptionType(Component.literal("Unknown species"));
    private static final SimpleCommandExceptionType BAD_SIZE = new SimpleCommandExceptionType(Component.literal("Unknown size"));

    private GrandTreesCommand() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("grandtrees")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("place")
                        .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .then(Commands.argument("species", StringArgumentType.word())
                                        .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                Arrays.stream(TreeSpecies.values()).map(TreeSpecies::getSerializedName), b))
                                        .then(Commands.argument("size", StringArgumentType.word())
                                                .suggests((c, b) -> SharedSuggestionProvider.suggest(
                                                        Arrays.stream(TreeSize.values()).map(TreeSize::getSerializedName), b))
                                                .executes(c -> place(c, c.getSource().getLevel().getRandom().nextLong()))
                                                .then(Commands.argument("seed", LongArgumentType.longArg())
                                                        .executes(c -> place(c, LongArgumentType.getLong(c, "seed"))))))))
                .then(Commands.literal("templates").executes(c -> {
                    int n = TreeTemplates.count();
                    c.getSource().sendSuccess(() -> Component.literal(n + " grand tree template(s) loaded"), false);
                    return n;
                })));
    }

    private static int place(CommandContext<CommandSourceStack> c, long seed) throws CommandSyntaxException {
        ServerLevel level = c.getSource().getLevel();
        BlockPos pos = BlockPosArgument.getLoadedBlockPos(c, "pos");
        TreeSpecies species = TreeSpecies.byName(StringArgumentType.getString(c, "species"));
        if (species == null) throw BAD_SPECIES.create();
        TreeSize size = TreeSize.byName(StringArgumentType.getString(c, "size"));
        if (size == null) throw BAD_SIZE.create();

        int half = (size.width - 1) / 2;
        BlockPos corner = pos.offset(-half, 0, -half);
        boolean ok = GrandTreeGrower.tryGrow(level, corner, size.width, species, size, seed, null,
                GrandTreeGrower.noConsumables(), true);
        c.getSource().sendSuccess(() -> Component.literal((ok ? "Grew " : "Could not grow ")
                + size.getSerializedName() + " " + species.getSerializedName() + " (seed " + seed + ")"), true);
        return ok ? 1 : 0;
    }
}
