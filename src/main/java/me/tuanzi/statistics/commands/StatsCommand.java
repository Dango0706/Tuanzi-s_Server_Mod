package me.tuanzi.statistics.commands;

import me.tuanzi.statistics.StatisticsModule;
import me.tuanzi.statistics.data.PlayerStatistics;
import me.tuanzi.statistics.data.ServerStatistics;
import me.tuanzi.statistics.listeners.PlayerJoinListener;
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

        source.sendSuccess(() -> Component.translatable("stats.header.player", playerName), false);
        source.sendSuccess(() -> Component.translatable("stats.online_time", hours, minutes, seconds), false);
        source.sendSuccess(() -> Component.translatable("stats.distance_traveled", stats.getDistanceTraveled()), false);
        source.sendSuccess(() -> Component.translatable("stats.blocks_placed", stats.getBlocksPlaced()), false);
        source.sendSuccess(() -> Component.translatable("stats.blocks_broken", stats.getBlocksBroken()), false);
        source.sendSuccess(() -> Component.translatable("stats.kills", stats.getKills()), false);
        source.sendSuccess(() -> Component.translatable("stats.deaths", stats.getDeaths()), false);
        source.sendSuccess(() -> Component.translatable("stats.damage_dealt", stats.getDamageDealt()), false);
        source.sendSuccess(() -> Component.translatable("stats.damage_taken", stats.getDamageTaken()), false);
        source.sendSuccess(() -> Component.translatable("stats.durability_used", stats.getDurabilityUsed()), false);
    }

    private static void showServerStats(CommandSourceStack source) {
        ServerStatistics stats = StatisticsModule.getInstance().getDataManager().getServerStatistics();

        source.sendSuccess(() -> Component.translatable("stats.header.server"), false);
        source.sendSuccess(() -> Component.translatable("stats.server.uptime", 
                stats.getTotalUptimeDays(), 
                stats.getTotalUptimeHours() % 24, 
                stats.getTotalUptimeMinutes() % 60, 
                stats.getTotalUptimeSeconds() % 60), false);
        source.sendSuccess(() -> Component.translatable("stats.server.session", 
                stats.getCurrentSessionUptimeSeconds() / 3600, 
                (stats.getCurrentSessionUptimeSeconds() % 3600) / 60, 
                stats.getCurrentSessionUptimeSeconds() % 60), false);
    }

    private static void showKillsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.kills", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.kills.total", stats.getKills()), false);
            source.sendSuccess(() -> Component.translatable("stats.kills.by_type_header"), false);

            Map<String, Integer> killsByType = stats.getKillsByEntityType();
            if (killsByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.kills.no_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : killsByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.translatable("stats.kills.entity_count", displayName, count), false);
                }
            }
        } else {
            String originalType = findOriginalEntityType(stats.getKillsByEntityType(), entityType);
            int kills = stats.getKillsByEntityType().getOrDefault(originalType, 0);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.translatable("stats.header.kills", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.kills.by_entity", displayName, kills), false);
        }
    }

    private static void showDeathsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.deaths", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.deaths.total", stats.getDeaths()), false);
            source.sendSuccess(() -> Component.translatable("stats.deaths.by_type_header"), false);

            Map<String, Integer> deathsByType = stats.getDeathsByEntityType();
            if (deathsByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.deaths.no_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : deathsByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.translatable("stats.deaths.entity_count", displayName, count), false);
                }
            }
        } else {
            String originalType = findOriginalEntityType(stats.getDeathsByEntityType(), entityType);
            int deaths = stats.getDeathsByEntityType().getOrDefault(originalType, 0);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.translatable("stats.header.deaths", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.deaths.by_entity", displayName, deaths), false);
        }
    }

    private static void showBlocksStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.translatable("stats.header.blocks", playerName), false);
        source.sendSuccess(() -> Component.translatable("stats.blocks.total_placed", stats.getBlocksPlaced()), false);
        source.sendSuccess(() -> Component.translatable("stats.blocks.total_broken", stats.getBlocksBroken()), false);
        source.sendSuccess(() -> Component.translatable("stats.blocks.placed_hint"), false);
        source.sendSuccess(() -> Component.translatable("stats.blocks.broken_hint"), false);
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
            source.sendSuccess(() -> Component.translatable("stats.header.fishing", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.fishing.total", stats.getFishingAttempts()), false);
            source.sendSuccess(() -> Component.translatable("stats.fishing.success", stats.getFishingSuccess()), false);
            source.sendSuccess(() -> Component.translatable("stats.fishing.failures", stats.getFishingFailures()), false);
            source.sendSuccess(() -> Component.translatable("stats.fishing.by_type_header"), false);

            Map<String, Integer> fishByType = stats.getFishCaughtByType();
            if (fishByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.fishing.no_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : fishByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    source.sendSuccess(() -> Component.translatable("stats.fishing.item_count", type, count), false);
                }
            }
        } else {
            int count = stats.getFishCaughtByType().getOrDefault(itemType, 0);
            source.sendSuccess(() -> Component.translatable("stats.header.fishing", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.fishing.item_detail", itemType, count), false);
        }
    }

    private static void showCraftingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.crafting", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.crafting.total", stats.getItemsCrafted()), false);
            source.sendSuccess(() -> Component.translatable("stats.crafting.by_type_header"), false);

            Map<String, Integer> craftedByType = stats.getItemsCraftedByType();
            if (craftedByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.crafting.no_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : craftedByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateItemKey(type);
                    source.sendSuccess(() -> Component.translatable("stats.crafting.item_count", displayName, count), false);
                }
            }
        } else {
            int count = stats.getItemsCraftedByType().getOrDefault(itemType, 0);
            String displayName = TranslationHelper.translateItemKey(itemType);
            source.sendSuccess(() -> Component.translatable("stats.header.crafting", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.crafting.item_detail", displayName, count), false);
        }
    }

    private static void showDropsStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            source.sendSuccess(() -> Component.translatable("stats.header.drops", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.drops.total", stats.getItemsDropped()), false);
            source.sendSuccess(() -> Component.translatable("stats.drops.by_type_header"), false);

            Map<String, Integer> droppedByType = stats.getItemsDroppedByType();
            if (droppedByType.isEmpty()) {
                source.sendSuccess(() -> Component.translatable("stats.drops.no_records"), false);
            } else {
                for (Map.Entry<String, Integer> entry : droppedByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateItemKey(type);
                    source.sendSuccess(() -> Component.translatable("stats.drops.item_count", displayName, count), false);
                }
            }
        } else {
            int count = stats.getItemsDroppedByType().getOrDefault(itemType, 0);
            String displayName = TranslationHelper.translateItemKey(itemType);
            source.sendSuccess(() -> Component.translatable("stats.header.drops", playerName), false);
            source.sendSuccess(() -> Component.translatable("stats.drops.item_detail", displayName, count), false);
        }
    }

    private static void showPlayerInfoStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.translatable("stats.header.player_info", playerName), false);

        long firstJoinTime = stats.getFirstJoinTime();
        if (firstJoinTime > 0) {
            java.time.Instant instant = java.time.Instant.ofEpochSecond(firstJoinTime);
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
            String formattedDate = zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            source.sendSuccess(() -> Component.translatable("stats.player_info.first_join", formattedDate), false);
        } else {
            source.sendSuccess(() -> Component.translatable("stats.player_info.first_join_unknown"), false);
        }

        source.sendSuccess(() -> Component.translatable("stats.player_info.login_days", stats.getLoginDays()), false);
    }

    private static void showActivityStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.translatable("stats.header.activity", playerName), false);
        source.sendSuccess(() -> Component.translatable("stats.activity.anvil_uses", stats.getAnvilUses()), false);
        source.sendSuccess(() -> Component.translatable("stats.activity.items_enchanted", stats.getItemsEnchanted()), false);
        source.sendSuccess(() -> Component.translatable("stats.activity.villager_trades", stats.getVillagerTrades()), false);
        source.sendSuccess(() -> Component.translatable("stats.activity.chat_messages", stats.getChatMessagesSent()), false);
    }

    private static void showExtendedStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.translatable("stats.header.extended", playerName), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.exp_header"), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.exp_gained", stats.getTotalExperienceGained()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.exp_consumed", stats.getTotalExperienceConsumed()), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.combat_header"), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.bow_hits", stats.getBowHits()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.crossbow_hits", stats.getCrossbowHits()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.shield_blocks", stats.getShieldBlocks()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.highest_damage_dealt", stats.getHighestDamageDealt()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.highest_damage_taken", stats.getHighestDamageTaken()), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.pet_header"), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.animals_tamed", stats.getAnimalsTamed()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.pet_deaths", stats.getPetDeaths()), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.exploration_header"), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.explored_chunks", stats.getExploredChunks()), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.total_jumps", stats.getTotalJumps()), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.time_header"), false);
        long longestSession = stats.getLongestSessionSeconds();
        source.sendSuccess(() -> Component.translatable("stats.extended.longest_session", 
                longestSession / 3600, (longestSession % 3600) / 60, longestSession % 60), false);
        
        long sneakTime = stats.getTotalSneakSeconds();
        source.sendSuccess(() -> Component.translatable("stats.extended.sneak_time", 
                sneakTime / 3600, (sneakTime % 3600) / 60, sneakTime % 60), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.life_header"), false);
        long shortestLife = stats.getShortestLifeSeconds();
        source.sendSuccess(() -> Component.translatable("stats.extended.shortest_life", 
                shortestLife / 60, shortestLife % 60), false);
        
        long longestLife = stats.getLongestLifeSeconds();
        source.sendSuccess(() -> Component.translatable("stats.extended.longest_life", 
                longestLife / 3600, (longestLife % 3600) / 60, longestLife % 60), false);
        
        source.sendSuccess(() -> Component.translatable("stats.extended.death_header"), false);
        source.sendSuccess(() -> Component.translatable("stats.extended.farthest_death_distance", stats.getFarthestDeathDistance()), false);
    }
}
