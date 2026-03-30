package me.tuanzi.statistics.commands;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.statistics.data.ServerStatistics;
import me.tuanzi.statistics.listeners.PlayerJoinListener;
import me.tuanzi.statistics.util.StatsTranslationHelper;
import me.tuanzi.statistics.util.TranslationHelper;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.suggestion.Suggestions;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.Map;

public class StatsCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("stats")
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player != null) {
                        String playerName = player.getName().getString();
                        showPlayerStats(context.getSource(), playerName);
                    } else {
                        context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                    }
                    return 1;
                })
                .then(Commands.argument("player", StringArgumentType.string())
                        .suggests((context, builder) -> {
                            ServerPlayer sender = context.getSource().getPlayer();
                            if (sender != null) {
                                return SharedSuggestionProvider.suggest(
                                        context.getSource().getServer().getPlayerList().getPlayers()
                                                .stream()
                                                .map(p -> "\"" + p.getName().getString() + "\""),
                                        builder
                                );
                            }
                            return Suggestions.empty();
                        })
                        .executes(context -> {
                            String playerName = StringArgumentType.getString(context, "player");
                            if (playerName.startsWith("\"") && playerName.endsWith("\"")) {
                                playerName = playerName.substring(1, playerName.length() - 1);
                            }
                            showPlayerStats(context.getSource(), playerName);
                            return 1;
                        })
                )
                .then(Commands.literal("server")
                        .executes(context -> {
                            showServerStats(context.getSource());
                            return 1;
                        })
                )
                .then(Commands.literal("kills")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showKillsStats(context.getSource(), playerName, null);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("entityType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        return SharedSuggestionProvider.suggest(
                                                stats.getKillsByEntityType().keySet().stream()
                                                        .map(type -> "\"" + translateEntityType(type) + "\""),
                                                builder
                                        );
                                    }
                                    return Suggestions.empty();
                                })
                                .executes(context -> {
                                    String playerName = context.getSource().getPlayer().getName().getString();
                                    String entityType = StringArgumentType.getString(context, "entityType");
                                    if (entityType.startsWith("\"") && entityType.endsWith("\"")) {
                                        entityType = entityType.substring(1, entityType.length() - 1);
                                    }
                                    showKillsStats(context.getSource(), playerName, entityType);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("deaths")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showDeathsStats(context.getSource(), playerName, null);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("entityType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        return SharedSuggestionProvider.suggest(
                                                stats.getDeathsByEntityType().keySet().stream()
                                                        .map(type -> "\"" + translateEntityType(type) + "\""),
                                                builder
                                        );
                                    }
                                    return Suggestions.empty();
                                })
                                .executes(context -> {
                                    String playerName = context.getSource().getPlayer().getName().getString();
                                    String entityType = StringArgumentType.getString(context, "entityType");
                                    if (entityType.startsWith("\"") && entityType.endsWith("\"")) {
                                        entityType = entityType.substring(1, entityType.length() - 1);
                                    }
                                    showDeathsStats(context.getSource(), playerName, entityType);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("blocks")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showBlocksStats(context.getSource(), playerName, null);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.literal("placed")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        showBlocksPlacedStats(context.getSource(), playerName, null);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("blockType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getBlocksPlacedByType().keySet().stream()
                                                                .map(type -> "\"" + TranslationHelper.translateBlockKey(type) + "\""),
                                                        builder
                                                );
                                            }
                                            return Suggestions.empty();
                                        })
                                        .executes(context -> {
                                            String playerName = context.getSource().getPlayer().getName().getString();
                                            String blockType = StringArgumentType.getString(context, "blockType");
                                            if (blockType.startsWith("\"") && blockType.endsWith("\"")) {
                                                blockType = blockType.substring(1, blockType.length() - 1);
                                            }
                                            showBlocksPlacedStats(context.getSource(), playerName, blockType);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("broken")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        showBlocksBrokenStats(context.getSource(), playerName, null);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("blockType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getBlocksBrokenByType().keySet().stream()
                                                                .map(type -> "\"" + type + "\""),
                                                        builder
                                                );
                                            }
                                            return Suggestions.empty();
                                        })
                                        .executes(context -> {
                                            String playerName = context.getSource().getPlayer().getName().getString();
                                            String blockType = StringArgumentType.getString(context, "blockType");
                                            if (blockType.startsWith("\"") && blockType.endsWith("\"")) {
                                                blockType = blockType.substring(1, blockType.length() - 1);
                                            }
                                            showBlocksBrokenStats(context.getSource(), playerName, blockType);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("damage")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showDamageStats(context.getSource(), playerName);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.literal("dealt")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        showDamageDealtStats(context.getSource(), playerName, null);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("entityType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getDamageDealtByEntityType().keySet().stream()
                                                                .map(type -> "\"" + translateEntityType(type) + "\""),
                                                        builder
                                                );
                                            }
                                            return Suggestions.empty();
                                        })
                                        .executes(context -> {
                                            String playerName = context.getSource().getPlayer().getName().getString();
                                            String entityType = StringArgumentType.getString(context, "entityType");
                                            if (entityType.startsWith("\"") && entityType.endsWith("\"")) {
                                                entityType = entityType.substring(1, entityType.length() - 1);
                                            }
                                            showDamageDealtStats(context.getSource(), playerName, entityType);
                                            return 1;
                                        })
                                )
                        )
                        .then(Commands.literal("taken")
                                .executes(context -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        showDamageTakenStats(context.getSource(), playerName, null);
                                    } else {
                                        context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("entityType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getDamageTakenByEntityType().keySet().stream()
                                                                .map(type -> "\"" + translateEntityType(type) + "\""),
                                                        builder
                                                );
                                            }
                                            return Suggestions.empty();
                                        })
                                        .executes(context -> {
                                            String playerName = context.getSource().getPlayer().getName().getString();
                                            String entityType = StringArgumentType.getString(context, "entityType");
                                            if (entityType.startsWith("\"") && entityType.endsWith("\"")) {
                                                entityType = entityType.substring(1, entityType.length() - 1);
                                            }
                                            showDamageTakenStats(context.getSource(), playerName, entityType);
                                            return 1;
                                        })
                                )
                        )
                )
                .then(Commands.literal("fishing")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showFishingStats(context.getSource(), playerName, null);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        return SharedSuggestionProvider.suggest(
                                                stats.getFishCaughtByType().keySet().stream()
                                                        .map(type -> "\"" + type + "\""),
                                                builder
                                        );
                                    }
                                    return Suggestions.empty();
                                })
                                .executes(context -> {
                                    String playerName = context.getSource().getPlayer().getName().getString();
                                    String itemType = StringArgumentType.getString(context, "itemType");
                                    if (itemType.startsWith("\"") && itemType.endsWith("\"")) {
                                        itemType = itemType.substring(1, itemType.length() - 1);
                                    }
                                    showFishingStats(context.getSource(), playerName, itemType);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("crafting")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showCraftingStats(context.getSource(), playerName, null);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        return SharedSuggestionProvider.suggest(
                                                stats.getItemsCraftedByType().keySet().stream()
                                                        .map(type -> "\"" + TranslationHelper.translateItemKey(type) + "\""),
                                                builder
                                        );
                                    }
                                    return Suggestions.empty();
                                })
                                .executes(context -> {
                                    String playerName = context.getSource().getPlayer().getName().getString();
                                    String itemType = StringArgumentType.getString(context, "itemType");
                                    if (itemType.startsWith("\"") && itemType.endsWith("\"")) {
                                        itemType = itemType.substring(1, itemType.length() - 1);
                                    }
                                    showCraftingStats(context.getSource(), playerName, itemType);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("drops")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showDropsStats(context.getSource(), playerName, null);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        return SharedSuggestionProvider.suggest(
                                                stats.getItemsDroppedByType().keySet().stream()
                                                        .map(type -> "\"" + TranslationHelper.translateItemKey(type) + "\""),
                                                builder
                                        );
                                    }
                                    return Suggestions.empty();
                                })
                                .executes(context -> {
                                    String playerName = context.getSource().getPlayer().getName().getString();
                                    String itemType = StringArgumentType.getString(context, "itemType");
                                    if (itemType.startsWith("\"") && itemType.endsWith("\"")) {
                                        itemType = itemType.substring(1, itemType.length() - 1);
                                    }
                                    showDropsStats(context.getSource(), playerName, itemType);
                                    return 1;
                                })
                        )
                )
                .then(Commands.literal("info")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showPlayerInfoStats(context.getSource(), playerName);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("activity")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showActivityStats(context.getSource(), playerName);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                )
                .then(Commands.literal("extended")
                        .executes(context -> {
                            ServerPlayer player = context.getSource().getPlayer();
                            if (player != null) {
                                String playerName = player.getName().getString();
                                showExtendedStats(context.getSource(), playerName);
                            } else {
                                context.getSource().sendSuccess(() -> Component.translatable("stats.command.player_only"), false);
                            }
                            return 1;
                        })
                )
        );
    }

    private static void showPlayerStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        long savedPlayTimeSeconds = stats.getPlayTimeSeconds();
        long currentSessionSeconds = 0;

        long loginTime = PlayerJoinListener.getLoginTime(playerName);
        long currentTime = System.currentTimeMillis() / 1000;
        if (currentTime - loginTime < 86400) {
            currentSessionSeconds = currentTime - loginTime;
        }

        long totalPlayTimeSeconds = savedPlayTimeSeconds + currentSessionSeconds;
        long hours = totalPlayTimeSeconds / 3600;
        long minutes = (totalPlayTimeSeconds % 3600) / 60;
        long seconds = totalPlayTimeSeconds % 60;

        StatsTranslationHelper.sendSuccess(source, "stats.header.player", playerName);
        StatsTranslationHelper.sendSuccess(source, "stats.online_time", hours, minutes, seconds);
        StatsTranslationHelper.sendSuccess(source, "stats.distance_traveled", stats.getDistanceTraveled());
        StatsTranslationHelper.sendSuccess(source, "stats.blocks_placed", stats.getBlocksPlaced());
        StatsTranslationHelper.sendSuccess(source, "stats.blocks_broken", stats.getBlocksBroken());
        StatsTranslationHelper.sendSuccess(source, "stats.kills", stats.getKills());
        StatsTranslationHelper.sendSuccess(source, "stats.deaths", stats.getDeaths());
        StatsTranslationHelper.sendSuccess(source, "stats.damage_dealt", stats.getDamageDealt());
        StatsTranslationHelper.sendSuccess(source, "stats.damage_taken", stats.getDamageTaken());
        StatsTranslationHelper.sendSuccess(source, "stats.durability_used", stats.getDurabilityUsed());
    }

    private static void showServerStats(CommandSourceStack source) {
        ServerStatistics stats = StatisticsModule.getInstance().getDataManager().getServerStatistics();

        StatsTranslationHelper.sendSuccess(source, "stats.header.server");
        StatsTranslationHelper.sendSuccess(source, "stats.server.uptime", 
                stats.getTotalUptimeDays(), 
                stats.getTotalUptimeHours() % 24, 
                stats.getTotalUptimeMinutes() % 60, 
                stats.getTotalUptimeSeconds() % 60);
        StatsTranslationHelper.sendSuccess(source, "stats.server.session", 
                stats.getCurrentSessionUptimeSeconds() / 3600, 
                (stats.getCurrentSessionUptimeSeconds() % 3600) / 60, 
                stats.getCurrentSessionUptimeSeconds() % 60);
    }

    private static void showKillsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.kills", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.kills.total", stats.getKills());
            StatsTranslationHelper.sendSuccess(source, "stats.kills.by_type_header");

            Map<String, Integer> killsByType = stats.getKillsByEntityType();
            if (killsByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.kills.no_records");
            } else {
                for (Map.Entry<String, Integer> entry : killsByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateEntityType(type);
                    StatsTranslationHelper.sendSuccess(source, "stats.kills.entity_count", displayName, count);
                }
            }
        } else {
            String originalType = findOriginalEntityType(stats.getKillsByEntityType(), entityType);
            int kills = stats.getKillsByEntityType().getOrDefault(originalType, 0);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            StatsTranslationHelper.sendSuccess(source, "stats.header.kills", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.kills.by_entity", displayName, kills);
        }
    }

    private static void showDeathsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.deaths", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.deaths.total", stats.getDeaths());
            StatsTranslationHelper.sendSuccess(source, "stats.deaths.by_type_header");

            Map<String, Integer> deathsByType = stats.getDeathsByEntityType();
            if (deathsByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.deaths.no_records");
            } else {
                for (Map.Entry<String, Integer> entry : deathsByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateEntityType(type);
                    StatsTranslationHelper.sendSuccess(source, "stats.deaths.entity_count", displayName, count);
                }
            }
        } else {
            String originalType = findOriginalEntityType(stats.getDeathsByEntityType(), entityType);
            int deaths = stats.getDeathsByEntityType().getOrDefault(originalType, 0);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            StatsTranslationHelper.sendSuccess(source, "stats.header.deaths", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.deaths.by_entity", displayName, deaths);
        }
    }

    private static void showBlocksStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        StatsTranslationHelper.sendSuccess(source, "stats.header.blocks", playerName);
        StatsTranslationHelper.sendSuccess(source, "stats.blocks.total_placed", stats.getBlocksPlaced());
        StatsTranslationHelper.sendSuccess(source, "stats.blocks.total_broken", stats.getBlocksBroken());
        StatsTranslationHelper.sendSuccess(source, "stats.blocks.placed_hint");
        StatsTranslationHelper.sendSuccess(source, "stats.blocks.broken_hint");
    }

    private static void showBlocksPlacedStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (blockType == null || blockType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.blocks_placed", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.blocks.total_placed", stats.getBlocksPlaced()), false);
            source.sendSuccess(() -> Component.translatable("stats.blocks.by_type_header"), false);

            Map<String, Integer> blocksByType = stats.getBlocksPlacedByType();
            if (blocksByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.blocks.no_placed_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : blocksByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateBlockKey(type);
                    source.sendSuccess(() -> Component.translatable("stats.blocks.placed_count", displayName, count), false);
                }
            }
        } else {
            int placed = stats.getBlocksPlacedByType().getOrDefault(blockType, 0);
            String displayName = TranslationHelper.translateBlockKey(blockType);
            source.sendSuccess(() -> Component.translatable("stats.header.blocks_placed", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.blocks.placed_detail", displayName, placed), false);
        }
    }

    private static void showBlocksBrokenStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (blockType == null || blockType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.blocks_broken", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.blocks.total_broken", stats.getBlocksBroken()), false);
            source.sendSuccess(() -> Component.translatable("stats.blocks.by_type_header"), false);

            Map<String, Integer> blocksByType = stats.getBlocksBrokenByType();
            if (blocksByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.blocks.no_broken_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : blocksByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateBlockKey(type);
                    source.sendSuccess(() -> Component.translatable("stats.blocks.broken_count", displayName, count), false);
                }
            }
        } else {
            int broken = stats.getBlocksBrokenByType().getOrDefault(blockType, 0);
            String displayName = TranslationHelper.translateBlockKey(blockType);
            source.sendSuccess(() -> Component.translatable("stats.header.blocks_broken", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.blocks.broken_detail", displayName, broken), false);
        }
    }

    private static void showDamageStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.translatable("stats.header.damage", playerName), false);
        source.sendSuccess(() -> Component.translatable("stats.damage.total_dealt", stats.getDamageDealt()), false);
        source.sendSuccess(() -> Component.translatable("stats.damage.total_taken", stats.getDamageTaken()), false);
        source.sendSuccess(() -> Component.translatable("stats.damage.dealt_hint"), false);
        source.sendSuccess(() -> Component.translatable("stats.damage.taken_hint"), false);
    }

    private static void showDamageDealtStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.damage_dealt", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.damage.total_dealt", stats.getDamageDealt()), false);
            source.sendSuccess(() -> Component.translatable("stats.damage.by_type_header"), false);

            Map<String, Long> damageByType = stats.getDamageDealtByEntityType();
            if (damageByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.damage.no_dealt_records"), false);
            } else {
                for (Map.Entry<String, Long> entry : damageByType.entrySet()) {
                    String type = entry.getKey();
                    long damage = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.translatable("stats.damage.dealt_to", displayName, damage), false);
                }
            }
        } else {
            String originalType = findOriginalEntityTypeForDamage(stats.getDamageDealtByEntityType(), entityType);
            long damage = stats.getDamageDealtByEntityType().getOrDefault(originalType, 0L);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.translatable("stats.header.damage_dealt", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.damage.dealt_detail", displayName, damage), false);
        }
    }

    private static void showDamageTakenStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.damage_taken", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.damage.total_taken", stats.getDamageTaken()), false);
            source.sendSuccess(() -> Component.translatable("stats.damage.by_type_header"), false);

            Map<String, Long> damageByType = stats.getDamageTakenByEntityType();
            if (damageByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.damage.no_taken_records"), false);
            } else {
                for (Map.Entry<String, Long> entry : damageByType.entrySet()) {
                    String type = entry.getKey();
                    long damage = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.translatable("stats.damage.taken_from", displayName, damage), false);
                }
            }
        } else {
            String originalType = findOriginalEntityTypeForDamage(stats.getDamageTakenByEntityType(), entityType);
            long damage = stats.getDamageTakenByEntityType().getOrDefault(originalType, 0L);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.translatable("stats.header.damage_taken", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.damage.taken_detail", displayName, damage), false);
        }
    }

    private static String findOriginalEntityTypeForDamage(Map<String, Long> map, String displayName) {
        for (String key : map.keySet()) {
            if (translateEntityType(key).equals(displayName)) {
                return key;
            }
        }
        return displayName;
    }

    private static String findOriginalEntityType(Map<String, Integer> map, String displayName) {
        for (String key : map.keySet()) {
            if (translateEntityType(key).equals(displayName)) {
                return key;
            }
        }
        return displayName;
    }

    private static String translateEntityType(String entityType) {
        if (entityType == null || entityType.isEmpty()) {
            return Component.translatable("entity.minecraft.unknown").getString();
        }

        String lower = entityType.toLowerCase();
        if (lower.contains("zombie")) {
            return Component.translatable("entity.minecraft.zombie").getString();
        } else if (lower.contains("skeleton")) {
            return Component.translatable("entity.minecraft.skeleton").getString();
        } else if (lower.contains("creeper")) {
            return Component.translatable("entity.minecraft.creeper").getString();
        } else if (lower.contains("spider")) {
            return Component.translatable("entity.minecraft.spider").getString();
        } else if (lower.contains("enderman")) {
            return Component.translatable("entity.minecraft.enderman").getString();
        } else if (lower.contains("blaze")) {
            return Component.translatable("entity.minecraft.blaze").getString();
        } else if (lower.contains("ghast")) {
            return Component.translatable("entity.minecraft.ghast").getString();
        } else if (lower.contains("witch")) {
            return Component.translatable("entity.minecraft.witch").getString();
        } else if (lower.contains("slime")) {
            return Component.translatable("entity.minecraft.slime").getString();
        } else if (lower.contains("pig")) {
            return Component.translatable("entity.minecraft.pig").getString();
        } else if (lower.contains("cow")) {
            return Component.translatable("entity.minecraft.cow").getString();
        } else if (lower.contains("sheep")) {
            return Component.translatable("entity.minecraft.sheep").getString();
        } else if (lower.contains("chicken")) {
            return Component.translatable("entity.minecraft.chicken").getString();
        } else if (lower.contains("player")) {
            return Component.translatable("entity.minecraft.player").getString();
        } else if (lower.contains("villager")) {
            return Component.translatable("entity.minecraft.villager").getString();
        } else if (lower.contains("pillager")) {
            return Component.translatable("entity.minecraft.pillager").getString();
        } else if (lower.contains("drowned")) {
            return Component.translatable("entity.minecraft.drowned").getString();
        } else if (lower.contains("phantom")) {
            return Component.translatable("entity.minecraft.phantom").getString();
        } else if (lower.contains("wither")) {
            return Component.translatable("entity.minecraft.wither").getString();
        } else if (lower.contains("warden")) {
            return Component.translatable("entity.minecraft.warden").getString();
        } else if (lower.contains("guardian")) {
            return Component.translatable("entity.minecraft.guardian").getString();
        } else if (lower.contains("elder")) {
            return Component.translatable("entity.minecraft.elder_guardian").getString();
        } else if (lower.contains("dragon")) {
            return Component.translatable("entity.minecraft.ender_dragon").getString();
        } else if (lower.contains("iron_golem") || lower.contains("irongolem")) {
            return Component.translatable("entity.minecraft.iron_golem").getString();
        } else if (lower.contains("snow_golem") || lower.contains("snowgolem")) {
            return Component.translatable("entity.minecraft.snow_golem").getString();
        }

        return entityType;
    }

    private static void showFishingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.fishing", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.fishing.total", stats.getFishingAttempts());
            StatsTranslationHelper.sendSuccess(source, "stats.fishing.success", stats.getFishingSuccess());
            StatsTranslationHelper.sendSuccess(source, "stats.fishing.failures", stats.getFishingFailures());
            StatsTranslationHelper.sendSuccess(source, "stats.fishing.by_type_header");

            Map<String, Integer> fishByType = stats.getFishCaughtByType();
            if (fishByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.fishing.no_records");
            } else {
                for (Map.Entry<String, Integer> entry : fishByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    StatsTranslationHelper.sendSuccess(source, "stats.fishing.item_count", type, count);
                }
            }
        } else {
            int count = stats.getFishCaughtByType().getOrDefault(itemType, 0);
            StatsTranslationHelper.sendSuccess(source, "stats.header.fishing", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.fishing.item_detail", itemType, count);
        }
    }

    private static void showCraftingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.crafting", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.crafting.total", stats.getItemsCrafted());
            StatsTranslationHelper.sendSuccess(source, "stats.crafting.by_type_header");

            Map<String, Integer> craftedByType = stats.getItemsCraftedByType();
            if (craftedByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.crafting.no_records");
            } else {
                for (Map.Entry<String, Integer> entry : craftedByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateItemKey(type);
                    StatsTranslationHelper.sendSuccess(source, "stats.crafting.item_count", displayName, count);
                }
            }
        } else {
            int count = stats.getItemsCraftedByType().getOrDefault(itemType, 0);
            String displayName = TranslationHelper.translateItemKey(itemType);
            StatsTranslationHelper.sendSuccess(source, "stats.header.crafting", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.crafting.item_detail", displayName, count);
        }
    }

    private static void showDropsStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.drops", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.drops.total", stats.getItemsDropped());
            StatsTranslationHelper.sendSuccess(source, "stats.drops.by_type_header");

            Map<String, Integer> droppedByType = stats.getItemsDroppedByType();
            if (droppedByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.drops.no_records");
            } else {
                for (Map.Entry<String, Integer> entry : droppedByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateItemKey(type);
                    StatsTranslationHelper.sendSuccess(source, "stats.drops.item_count", displayName, count);
                }
            }
        } else {
            int count = stats.getItemsDroppedByType().getOrDefault(itemType, 0);
            String displayName = TranslationHelper.translateItemKey(itemType);
            StatsTranslationHelper.sendSuccess(source, "stats.header.drops", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.drops.item_detail", displayName, count);
        }
    }

    private static void showPlayerInfoStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        StatsTranslationHelper.sendSuccess(source, "stats.header.player_info", playerName);

        long firstJoinTime = stats.getFirstJoinTime();
        if (firstJoinTime > 0) {
            java.time.Instant instant = java.time.Instant.ofEpochSecond(firstJoinTime);
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
            String formattedDate = zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            StatsTranslationHelper.sendSuccess(source, "stats.player_info.first_join", formattedDate);
        } else {
            StatsTranslationHelper.sendSuccess(source, "stats.player_info.first_join_unknown");
        }

        StatsTranslationHelper.sendSuccess(source, "stats.player_info.login_days", stats.getLoginDays());
    }

    private static void showActivityStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        StatsTranslationHelper.sendSuccess(source, "stats.header.activity", playerName);
        StatsTranslationHelper.sendSuccess(source, "stats.activity.anvil_uses", stats.getAnvilUses());
        StatsTranslationHelper.sendSuccess(source, "stats.activity.items_enchanted", stats.getItemsEnchanted());
        StatsTranslationHelper.sendSuccess(source, "stats.activity.villager_trades", stats.getVillagerTrades());
        StatsTranslationHelper.sendSuccess(source, "stats.activity.chat_messages", stats.getChatMessagesSent());
    }

    private static void showExtendedStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        StatsTranslationHelper.sendSuccess(source, "stats.header.extended", playerName);
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.exp_header");
        StatsTranslationHelper.sendSuccess(source, "stats.extended.exp_gained", stats.getTotalExperienceGained());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.exp_consumed", stats.getTotalExperienceConsumed());
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.combat_header");
        StatsTranslationHelper.sendSuccess(source, "stats.extended.bow_hits", stats.getBowHits());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.crossbow_hits", stats.getCrossbowHits());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.shield_blocks", stats.getShieldBlocks());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.highest_damage_dealt", stats.getHighestDamageDealt());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.highest_damage_taken", stats.getHighestDamageTaken());
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.pet_header");
        StatsTranslationHelper.sendSuccess(source, "stats.extended.animals_tamed", stats.getAnimalsTamed());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.pet_deaths", stats.getPetDeaths());
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.exploration_header");
        StatsTranslationHelper.sendSuccess(source, "stats.extended.explored_chunks", stats.getExploredChunks());
        StatsTranslationHelper.sendSuccess(source, "stats.extended.total_jumps", stats.getTotalJumps());
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.time_header");
        long longestSession = stats.getLongestSessionSeconds();
        StatsTranslationHelper.sendSuccess(source, "stats.extended.longest_session", 
                longestSession / 3600, (longestSession % 3600) / 60, longestSession % 60);
        
        long sneakTime = stats.getTotalSneakSeconds();
        StatsTranslationHelper.sendSuccess(source, "stats.extended.sneak_time", 
                sneakTime / 3600, (sneakTime % 3600) / 60, sneakTime % 60);
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.life_header");
        long shortestLife = stats.getShortestLifeSeconds();
        StatsTranslationHelper.sendSuccess(source, "stats.extended.shortest_life", 
                shortestLife / 60, shortestLife % 60);
        
        long longestLife = stats.getLongestLifeSeconds();
        StatsTranslationHelper.sendSuccess(source, "stats.extended.longest_life", 
                longestLife / 3600, (longestLife % 3600) / 60, longestLife % 60);
        
        StatsTranslationHelper.sendSuccess(source, "stats.extended.death_header");
        StatsTranslationHelper.sendSuccess(source, "stats.extended.farthest_death_distance", stats.getFarthestDeathDistance());
    }
}
