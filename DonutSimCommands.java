package net.donutsim.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.donutsim.config.DonutSimConfig;
import net.donutsim.entity.FakeDropManager;
import net.donutsim.util.ChatHelper;
import net.donutsim.util.NumberFormatter;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

public final class DonutSimCommands {
    private DonutSimCommands() {}

    private static final SuggestionProvider<FabricClientCommandSource> ONLINE_PLAYER_SUGGESTIONS = (ctx, builder) -> {
        Minecraft mc = Minecraft.getInstance();
        List<String> names = new ArrayList<>();
        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                names.add(info.getProfile().name());
            }
        }
        return SharedSuggestionProvider.suggest(names, builder);
    };

    private static final SuggestionProvider<FabricClientCommandSource> ITEM_ID_SUGGESTIONS = (ctx, builder) ->
            SharedSuggestionProvider.suggestResource(BuiltInRegistries.ITEM.keySet(), builder);

    public static void register() {
        ClientCommandRegistrationCallback.EVENT.register(DonutSimCommands::registerCommands);
    }

    private static void registerCommands(CommandDispatcher<FabricClientCommandSource> dispatcher, CommandBuildContext buildContext) {
        // /pay <player> <amount>
        dispatcher.register(ClientCommandManager.literal("pay")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYER_SUGGESTIONS)
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executePay(
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "amount")
                                )))));

        // /payme <player> <amount>
        dispatcher.register(ClientCommandManager.literal("payme")
                .then(ClientCommandManager.argument("player", StringArgumentType.word())
                        .suggests(ONLINE_PLAYER_SUGGESTIONS)
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executePayMe(
                                        StringArgumentType.getString(ctx, "player"),
                                        StringArgumentType.getString(ctx, "amount")
                                )))));

        // /bal and /balance
        dispatcher.register(ClientCommandManager.literal("bal")
                .executes(ctx -> executeBalance()));
        dispatcher.register(ClientCommandManager.literal("balance")
                .executes(ctx -> executeBalance()));

        // /eco <set|give|take> <amount>
        dispatcher.register(ClientCommandManager.literal("eco")
                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeEco("set", StringArgumentType.getString(ctx, "amount")))))
                .then(ClientCommandManager.literal("give")
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeEco("give", StringArgumentType.getString(ctx, "amount")))))
                .then(ClientCommandManager.literal("take")
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeEco("take", StringArgumentType.getString(ctx, "amount"))))));

        // /shards <set|give> <amount>
        dispatcher.register(ClientCommandManager.literal("shards")
                .then(ClientCommandManager.literal("set")
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeShards("set", StringArgumentType.getString(ctx, "amount")))))
                .then(ClientCommandManager.literal("give")
                        .then(ClientCommandManager.argument("amount", StringArgumentType.word())
                                .executes(ctx -> executeShards("give", StringArgumentType.getString(ctx, "amount"))))));

        // /stats <kills|deaths|team> <value>
        dispatcher.register(ClientCommandManager.literal("stats")
                .then(ClientCommandManager.literal("kills")
                        .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx -> executeStatsInt("kills", IntegerArgumentType.getInteger(ctx, "value")))))
                .then(ClientCommandManager.literal("deaths")
                        .then(ClientCommandManager.argument("value", IntegerArgumentType.integer(0))
                                .executes(ctx -> executeStatsInt("deaths", IntegerArgumentType.getInteger(ctx, "value")))))
                .then(ClientCommandManager.literal("team")
                        .then(ClientCommandManager.argument("value", StringArgumentType.greedyString())
                                .executes(ctx -> executeStatsTeam(StringArgumentType.getString(ctx, "value"))))));

        // /drop <item_id> [count]
        dispatcher.register(ClientCommandManager.literal("drop")
                .then(ClientCommandManager.argument("item_id", IdentifierArgument.id())
                        .suggests(ITEM_ID_SUGGESTIONS)
                        .executes(ctx -> executeDrop(IdentifierArgument.getId(ctx, "item_id"), 1))
                        .then(ClientCommandManager.argument("count", IntegerArgumentType.integer(1, 64))
                                .executes(ctx -> executeDrop(
                                        IdentifierArgument.getId(ctx, "item_id"),
                                        IntegerArgumentType.getInteger(ctx, "count")
                                )))));

        // /kit <spawner|pvp|keyall>
        dispatcher.register(ClientCommandManager.literal("kit")
                .then(ClientCommandManager.literal("spawner").executes(ctx -> executeKit("spawner")))
                .then(ClientCommandManager.literal("pvp").executes(ctx -> executeKit("pvp")))
                .then(ClientCommandManager.literal("keyall").executes(ctx -> executeKit("keyall"))));
    }

    private static int executePay(String player, String rawAmount) {
        double amount;
        try {
            amount = NumberFormatter.parseAmount(rawAmount);
        } catch (NumberFormatException e) {
            ChatHelper.sendError("Invalid amount: " + rawAmount);
            return 0;
        }
        if (amount <= 0) {
            ChatHelper.sendError("Amount must be greater than $0.");
            return 0;
        }

        DonutSimConfig cfg = DonutSimConfig.get();
        if (cfg.balance < amount) {
            ChatHelper.sendError("You do not have enough money! Balance: $" + NumberFormatter.formatCompact(cfg.balance));
            return 0;
        }

        cfg.balance -= amount;
        DonutSimConfig.save();

        ChatHelper.playOrbSound();
        ChatHelper.sendLocalMessage(
                Component.literal("You paid ").withStyle(ChatFormatting.WHITE)
                        .append(Component.literal(player).withStyle(ChatFormatting.AQUA))
                        .append(Component.literal(" ").withStyle(ChatFormatting.WHITE))
                        .append(ChatHelper.greenMoney(amount))
        );
        return 1;
    }

    public static int executePayMe(String player, String rawAmount) {
        double amount;
        try {
            amount = NumberFormatter.parseAmount(rawAmount);
        } catch (NumberFormatException e) {
            ChatHelper.sendError("Invalid amount: " + rawAmount);
            return 0;
        }
        if (amount <= 0) {
            ChatHelper.sendError("Amount must be greater than $0.");
            return 0;
        }

        DonutSimConfig cfg = DonutSimConfig.get();
        cfg.balance += amount;
        DonutSimConfig.save();

        ChatHelper.playOrbSound();
        ChatHelper.sendLocalMessage(
                Component.literal(player).withStyle(ChatFormatting.AQUA)
                        .append(Component.literal(" paid you ").withStyle(ChatFormatting.WHITE))
                        .append(ChatHelper.greenMoney(amount))
        );
        return 1;
    }

    private static int executeBalance() {
        DonutSimConfig cfg = DonutSimConfig.get();
        ChatHelper.sendLocalMessage(
                Component.literal("Balance: ").withStyle(ChatFormatting.WHITE)
                        .append(ChatHelper.greenMoney(cfg.balance))
        );
        return 1;
    }

    private static int executeEco(String mode, String rawAmount) {
        double amount;
        try {
            amount = NumberFormatter.parseAmount(rawAmount);
        } catch (NumberFormatException e) {
            ChatHelper.sendError("Invalid amount: " + rawAmount);
            return 0;
        }

        DonutSimConfig cfg = DonutSimConfig.get();
        switch (mode) {
            case "set" -> cfg.balance = Math.max(0, amount);
            case "give" -> cfg.balance += Math.max(0, amount);
            case "take" -> cfg.balance = Math.max(0, cfg.balance - Math.max(0, amount));
        }
        DonutSimConfig.save();

        ChatHelper.sendLocalMessage(
                ChatHelper.prefix()
                        .append(Component.literal("Updated balance to ").withStyle(ChatFormatting.WHITE))
                        .append(ChatHelper.greenMoney(cfg.balance))
        );
        return 1;
    }

    private static int executeShards(String mode, String rawAmount) {
        long amount;
        try {
            amount = Math.round(NumberFormatter.parseAmount(rawAmount));
        } catch (NumberFormatException e) {
            ChatHelper.sendError("Invalid shard amount: " + rawAmount);
            return 0;
        }

        DonutSimConfig cfg = DonutSimConfig.get();
        if ("set".equals(mode)) {
            cfg.shards = Math.max(0L, amount);
        } else {
            cfg.shards += Math.max(0L, amount);
        }
        DonutSimConfig.save();

        ChatHelper.sendLocalMessage(
                ChatHelper.prefix()
                        .append(Component.literal("Shards set to ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(NumberFormatter.formatWhole(cfg.shards)).withStyle(ChatFormatting.LIGHT_PURPLE))
        );
        return 1;
    }

    private static int executeStatsInt(String field, int value) {
        DonutSimConfig cfg = DonutSimConfig.get();
        if ("kills".equals(field)) {
            cfg.kills = value;
            DonutSimConfig.save();
            ChatHelper.sendLocalMessage(
                    ChatHelper.prefix()
                            .append(Component.literal("Kills updated to ").withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.RED))
            );
        } else {
            cfg.deaths = value;
            DonutSimConfig.save();
            ChatHelper.sendLocalMessage(
                    ChatHelper.prefix()
                            .append(Component.literal("Deaths updated to ").withStyle(ChatFormatting.WHITE))
                            .append(Component.literal(String.valueOf(value)).withStyle(ChatFormatting.DARK_RED))
            );
        }
        return 1;
    }

    private static int executeStatsTeam(String teamTag) {
        DonutSimConfig cfg = DonutSimConfig.get();
        cfg.teamName = teamTag.trim();
        DonutSimConfig.save();

        ChatHelper.sendLocalMessage(
                ChatHelper.prefix()
                        .append(Component.literal("Team updated to ").withStyle(ChatFormatting.WHITE))
                        .append(Component.literal(cfg.teamName).withStyle(ChatFormatting.AQUA))
        );
        return 1;
    }

    private static int executeDrop(ResourceLocation itemId, int count) {
        if (!FakeDropManager.spawnItem(itemId, count)) {
            ChatHelper.sendError("Unknown item ID: " + itemId);
            return 0;
        }
        ChatHelper.sendLocalMessage(
                ChatHelper.prefix()
                        .append(Component.literal("Spawned ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(count + "x " + itemId).withStyle(ChatFormatting.AQUA))
        );
        return 1;
    }

    private static int executeKit(String kitName) {
        FakeDropManager.spawnKit(kitName);
        ChatHelper.sendLocalMessage(
                ChatHelper.prefix()
                        .append(Component.literal("Dropped kit: ").withStyle(ChatFormatting.GRAY))
                        .append(Component.literal(kitName.toUpperCase()).withStyle(ChatFormatting.AQUA, ChatFormatting.BOLD))
        );
        return 1;
    }
}
