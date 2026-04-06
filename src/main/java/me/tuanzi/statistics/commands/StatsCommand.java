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
import net.minecraft.server.level.ServerPlayer;

import java.util.Locale;
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
                        StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                            }
                            return 1;
                        })
                        .then(Commands.argument("entityType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        String language = getPreferredLanguage(context.getSource());
                                        return SharedSuggestionProvider.suggest(
                                                stats.getKillsByEntityType().keySet().stream()
                                                        .map(type -> "\"" + translateEntityType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                            }
                            return 1;
                        })
                        .then(Commands.argument("entityType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        String language = getPreferredLanguage(context.getSource());
                                        return SharedSuggestionProvider.suggest(
                                                stats.getDeathsByEntityType().keySet().stream()
                                                        .map(type -> "\"" + translateEntityType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
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
                                        StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("blockType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                String language = getPreferredLanguage(context.getSource());
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getBlocksPlacedByType().keySet().stream()
                                                                .map(type -> "\"" + translateBlockType(type, language) + "\""),
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
                                        StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("blockType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                String language = getPreferredLanguage(context.getSource());
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getBlocksBrokenByType().keySet().stream()
                                                                .map(type -> "\"" + translateBlockType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
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
                                        StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("entityType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                String language = getPreferredLanguage(context.getSource());
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getDamageDealtByEntityType().keySet().stream()
                                                                .map(type -> "\"" + translateEntityType(type, language) + "\""),
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
                                        StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                                    }
                                    return 1;
                                })
                                .then(Commands.argument("entityType", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            ServerPlayer player = context.getSource().getPlayer();
                                            if (player != null) {
                                                String playerName = player.getName().getString();
                                                PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                                String language = getPreferredLanguage(context.getSource());
                                                return SharedSuggestionProvider.suggest(
                                                        stats.getDamageTakenByEntityType().keySet().stream()
                                                                .map(type -> "\"" + translateEntityType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                            }
                            return 1;
                        })
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        String language = getPreferredLanguage(context.getSource());
                                        return SharedSuggestionProvider.suggest(
                                                stats.getFishCaughtByType().keySet().stream()
                                                        .map(type -> "\"" + translateItemType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                            }
                            return 1;
                        })
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        String language = getPreferredLanguage(context.getSource());
                                        return SharedSuggestionProvider.suggest(
                                                stats.getItemsCraftedByType().keySet().stream()
                                                        .map(type -> "\"" + translateItemType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
                            }
                            return 1;
                        })
                        .then(Commands.argument("itemType", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    ServerPlayer player = context.getSource().getPlayer();
                                    if (player != null) {
                                        String playerName = player.getName().getString();
                                        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
                                        String language = getPreferredLanguage(context.getSource());
                                        return SharedSuggestionProvider.suggest(
                                                stats.getItemsDroppedByType().keySet().stream()
                                                        .map(type -> "\"" + translateItemType(type, language) + "\""),
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
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
                                StatsTranslationHelper.sendSuccess(context.getSource(), "stats.command.player_only");
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
        String language = getPreferredLanguage(source);

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
                    String displayName = translateEntityType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.kills.entity_count", displayName, count);
                }
            }
        } else {
            String resolvedType = resolveEntityInput(entityType, stats.getKillsByEntityType(), language);
            int kills = stats.getKillsByEntityType().getOrDefault(resolvedType, 0);
            String displayName = translateEntityType(resolvedType, language);
            StatsTranslationHelper.sendSuccess(source, "stats.header.kills", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.kills.by_entity", displayName, kills);
        }
    }

    private static void showDeathsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

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
                    String displayName = translateEntityType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.deaths.entity_count", displayName, count);
                }
            }
        } else {
            String resolvedType = resolveEntityInput(entityType, stats.getDeathsByEntityType(), language);
            int deaths = stats.getDeathsByEntityType().getOrDefault(resolvedType, 0);
            String displayName = translateEntityType(resolvedType, language);
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
        String language = getPreferredLanguage(source);

        if (blockType == null || blockType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.blocks_placed", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.blocks.total_placed", stats.getBlocksPlaced());
            StatsTranslationHelper.sendSuccess(source, "stats.blocks.by_type_header");

            Map<String, Integer> blocksByType = stats.getBlocksPlacedByType();
            if (blocksByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.blocks.no_placed_records");
            } else {
                for (Map.Entry<String, Integer> entry : blocksByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateBlockType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.blocks.placed_count", displayName, count);
                }
            }
        } else {
            String resolvedType = resolveBlockInput(blockType, stats.getBlocksPlacedByType(), language);
            int placed = stats.getBlocksPlacedByType().getOrDefault(resolvedType, 0);
            String displayName = translateBlockType(resolvedType, language);
            StatsTranslationHelper.sendSuccess(source, "stats.header.blocks_placed", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.blocks.placed_detail", displayName, placed);
        }
    }

    private static void showBlocksBrokenStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

        if (blockType == null || blockType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.blocks_broken", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.blocks.total_broken", stats.getBlocksBroken());
            StatsTranslationHelper.sendSuccess(source, "stats.blocks.by_type_header");

            Map<String, Integer> blocksByType = stats.getBlocksBrokenByType();
            if (blocksByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.blocks.no_broken_records");
            } else {
                for (Map.Entry<String, Integer> entry : blocksByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateBlockType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.blocks.broken_count", displayName, count);
                }
            }
        } else {
            String resolvedType = resolveBlockInput(blockType, stats.getBlocksBrokenByType(), language);
            int broken = stats.getBlocksBrokenByType().getOrDefault(resolvedType, 0);
            String displayName = translateBlockType(resolvedType, language);
            StatsTranslationHelper.sendSuccess(source, "stats.header.blocks_broken", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.blocks.broken_detail", displayName, broken);
        }
    }

    private static void showDamageStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        StatsTranslationHelper.sendSuccess(source, "stats.header.damage", playerName);
        StatsTranslationHelper.sendSuccess(source, "stats.damage.total_dealt", stats.getDamageDealt());
        StatsTranslationHelper.sendSuccess(source, "stats.damage.total_taken", stats.getDamageTaken());
        StatsTranslationHelper.sendSuccess(source, "stats.damage.dealt_hint");
        StatsTranslationHelper.sendSuccess(source, "stats.damage.taken_hint");
    }

    private static void showDamageDealtStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

        if (entityType == null || entityType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.damage_dealt", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.damage.total_dealt", stats.getDamageDealt());
            StatsTranslationHelper.sendSuccess(source, "stats.damage.by_type_header");

            Map<String, Long> damageByType = stats.getDamageDealtByEntityType();
            if (damageByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.damage.no_dealt_records");
            } else {
                for (Map.Entry<String, Long> entry : damageByType.entrySet()) {
                    String type = entry.getKey();
                    long damage = entry.getValue();
                    String displayName = translateEntityType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.damage.dealt_to", displayName, damage);
                }
            }
        } else {
            String resolvedType = resolveEntityInput(entityType, stats.getDamageDealtByEntityType(), language);
            long damage = stats.getDamageDealtByEntityType().getOrDefault(resolvedType, 0L);
            String displayName = translateEntityType(resolvedType, language);
            StatsTranslationHelper.sendSuccess(source, "stats.header.damage_dealt", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.damage.dealt_detail", displayName, damage);
        }
    }

    private static void showDamageTakenStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

        if (entityType == null || entityType.isEmpty()) {
            StatsTranslationHelper.sendSuccess(source, "stats.header.damage_taken", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.damage.total_taken", stats.getDamageTaken());
            StatsTranslationHelper.sendSuccess(source, "stats.damage.by_type_header");

            Map<String, Long> damageByType = stats.getDamageTakenByEntityType();
            if (damageByType.isEmpty()) {
                StatsTranslationHelper.sendSuccess(source, "stats.damage.no_taken_records");
            } else {
                for (Map.Entry<String, Long> entry : damageByType.entrySet()) {
                    String type = entry.getKey();
                    long damage = entry.getValue();
                    String displayName = translateEntityType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.damage.taken_from", displayName, damage);
                }
            }
        } else {
            String resolvedType = resolveEntityInput(entityType, stats.getDamageTakenByEntityType(), language);
            long damage = stats.getDamageTakenByEntityType().getOrDefault(resolvedType, 0L);
            String displayName = translateEntityType(resolvedType, language);
            StatsTranslationHelper.sendSuccess(source, "stats.header.damage_taken", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.damage.taken_detail", displayName, damage);
        }
    }

    private static String getPreferredLanguage(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        if (player == null) {
            return "zh_cn";
        }
        String language = player.clientInformation().language();
        if (language == null || language.isBlank()) {
            return "zh_cn";
        }
        String normalized = language.toLowerCase(Locale.ROOT);
        return normalized.startsWith("zh") ? "zh_cn" : "en_us";
    }

    private static String resolveEntityInput(String input, Map<String, ? extends Number> entityStats, String language) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String normalizedInput = input.trim();
        if (entityStats.containsKey(normalizedInput)) {
            return normalizedInput;
        }

        for (String entityType : entityStats.keySet()) {
            if (matchesEntityInput(normalizedInput, entityType, language)) {
                return entityType;
            }
        }

        return normalizedInput;
    }

    private static boolean matchesEntityInput(String input, String entityType, String language) {
        if (entityType.equalsIgnoreCase(input)) {
            return true;
        }

        String entityId = entityType;
        if (entityId.startsWith("entity.minecraft.")) {
            entityId = entityId.substring("entity.minecraft.".length());
        }
        if (entityId.equalsIgnoreCase(input)) {
            return true;
        }

        String preferredName = translateEntityType(entityType, language);
        if (preferredName.equalsIgnoreCase(input)) {
            return true;
        }

        String zhName = translateEntityType(entityType, "zh_cn");
        if (zhName.equalsIgnoreCase(input)) {
            return true;
        }

        String enName = translateEntityType(entityType, "en_us");
        return enName.equalsIgnoreCase(input);
    }

    private static String translateEntityType(String entityType, String language) {
        if (entityType == null || entityType.isEmpty()) {
            return StatsTranslationHelper.translate("entity.minecraft.unknown", language);
        }

        String key = normalizeEntityTranslationKey(entityType);
        String translated = StatsTranslationHelper.translate(key, language);
        if (!translated.equals(key)) {
            return translated;
        }

        String fallback = entityType;
        if (fallback.startsWith("entity.minecraft.")) {
            fallback = fallback.substring("entity.minecraft.".length());
        }
        return fallback.replace('_', ' ');
    }

    private static String normalizeEntityTranslationKey(String entityType) {
        String normalized = entityType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith("entity.")) {
            return normalized;
        }
        if (normalized.contains(":")) {
            return "entity." + normalized.replace(':', '.');
        }
        return "entity.minecraft." + normalized;
    }

    private static String resolveBlockInput(String input, Map<String, ? extends Number> blockStats, String language) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String normalizedInput = input.trim();
        if (blockStats.containsKey(normalizedInput)) {
            return normalizedInput;
        }

        for (String blockType : blockStats.keySet()) {
            if (matchesBlockInput(normalizedInput, blockType, language)) {
                return blockType;
            }
        }

        return normalizedInput;
    }

    private static String resolveItemInput(String input, Map<String, ? extends Number> itemStats, String language) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        String normalizedInput = input.trim();
        if (itemStats.containsKey(normalizedInput)) {
            return normalizedInput;
        }

        for (String itemType : itemStats.keySet()) {
            if (matchesItemInput(normalizedInput, itemType, language)) {
                return itemType;
            }
        }

        return normalizedInput;
    }

    private static boolean matchesBlockInput(String input, String blockType, String language) {
        if (blockType.equalsIgnoreCase(input)) {
            return true;
        }

        String normalizedInput = normalizeComparableToken(input);
        if (normalizeComparableToken(blockType).equals(normalizedInput)) {
            return true;
        }

        String normalizedKey = normalizeBlockTranslationKey(blockType);
        if (normalizeComparableToken(normalizedKey).equals(normalizedInput)) {
            return true;
        }

        String blockId = stripTranslationPrefix(blockType, "block.minecraft.");
        if (normalizeComparableToken(blockId).equals(normalizedInput)) {
            return true;
        }

        String normalizedBlockId = stripTranslationPrefix(normalizedKey, "block.minecraft.");
        if (normalizeComparableToken(normalizedBlockId).equals(normalizedInput)) {
            return true;
        }

        String preferredName = translateBlockType(blockType, language);
        if (normalizeComparableToken(preferredName).equals(normalizedInput)) {
            return true;
        }

        String zhName = translateBlockType(blockType, "zh_cn");
        if (normalizeComparableToken(zhName).equals(normalizedInput)) {
            return true;
        }

        String enName = translateBlockType(blockType, "en_us");
        return normalizeComparableToken(enName).equals(normalizedInput);
    }

    private static boolean matchesItemInput(String input, String itemType, String language) {
        if (itemType.equalsIgnoreCase(input)) {
            return true;
        }

        String normalizedInput = normalizeComparableToken(input);
        if (normalizeComparableToken(itemType).equals(normalizedInput)) {
            return true;
        }

        String normalizedKey = normalizeItemTranslationKey(itemType);
        if (normalizeComparableToken(normalizedKey).equals(normalizedInput)) {
            return true;
        }

        String itemId = stripTranslationPrefix(itemType, "item.minecraft.");
        if (normalizeComparableToken(itemId).equals(normalizedInput)) {
            return true;
        }

        String normalizedItemId = stripTranslationPrefix(normalizedKey, "item.minecraft.");
        if (normalizeComparableToken(normalizedItemId).equals(normalizedInput)) {
            return true;
        }

        String preferredName = translateItemType(itemType, language);
        if (normalizeComparableToken(preferredName).equals(normalizedInput)) {
            return true;
        }

        String zhName = translateItemType(itemType, "zh_cn");
        if (normalizeComparableToken(zhName).equals(normalizedInput)) {
            return true;
        }

        String enName = translateItemType(itemType, "en_us");
        return normalizeComparableToken(enName).equals(normalizedInput);
    }

    private static String translateBlockType(String blockType, String language) {
        if (blockType == null || blockType.isEmpty()) {
            return StatsTranslationHelper.translate("block.minecraft.unknown", language);
        }

        String key = normalizeBlockTranslationKey(blockType);
        String translated = StatsTranslationHelper.translate(key, language);
        if (!translated.equals(key)) {
            return translated;
        }

        String fallback = TranslationHelper.translateBlockKey(key);
        if (!fallback.equals(key)) {
            return fallback;
        }

        String rawFallback = TranslationHelper.translateBlockKey(blockType);
        if (!rawFallback.equals(blockType)) {
            return rawFallback;
        }

        return stripTranslationPrefix(key, "block.minecraft.").replace('_', ' ');
    }

    private static String translateItemType(String itemType, String language) {
        if (itemType == null || itemType.isEmpty()) {
            return StatsTranslationHelper.translate("item.minecraft.unknown", language);
        }

        String key = normalizeItemTranslationKey(itemType);
        String translated = StatsTranslationHelper.translate(key, language);
        if (!translated.equals(key)) {
            return translated;
        }

        String fallback = TranslationHelper.translateItemKey(key);
        if (!fallback.equals(key)) {
            return fallback;
        }

        String rawFallback = TranslationHelper.translateItemKey(itemType);
        if (!rawFallback.equals(itemType)) {
            return rawFallback;
        }

        return stripTranslationPrefix(key, "item.minecraft.").replace('_', ' ');
    }

    private static String normalizeBlockTranslationKey(String blockType) {
        String normalized = blockType.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("block.")) {
            return normalized;
        }
        if (normalized.startsWith("minecraft:")) {
            return "block.minecraft." + normalized.substring("minecraft:".length());
        }
        if (normalized.startsWith("minecraft.")) {
            return "block." + normalized;
        }
        if (normalized.contains(":")) {
            return "block." + normalized.replace(':', '.');
        }

        String idPath = toSnakeCase(normalized).replace(' ', '_').replace('-', '_');
        return "block.minecraft." + idPath;
    }

    private static String normalizeItemTranslationKey(String itemType) {
        String normalized = itemType.trim().toLowerCase(Locale.ROOT);
        if (normalized.startsWith("item.")) {
            return normalized;
        }
        if (normalized.startsWith("minecraft:")) {
            return "item.minecraft." + normalized.substring("minecraft:".length());
        }
        if (normalized.startsWith("minecraft.")) {
            return "item." + normalized;
        }
        if (normalized.contains(":")) {
            return "item." + normalized.replace(':', '.');
        }

        String idPath = toSnakeCase(normalized).replace(' ', '_').replace('-', '_');
        return "item.minecraft." + idPath;
    }

    private static String stripTranslationPrefix(String key, String prefix) {
        if (key.startsWith(prefix)) {
            return key.substring(prefix.length());
        }
        if (key.startsWith("minecraft:")) {
            return key.substring("minecraft:".length());
        }
        if (key.startsWith("minecraft.")) {
            return key.substring("minecraft.".length());
        }
        if (key.startsWith("item.minecraft.")) {
            return key.substring("item.minecraft.".length());
        }
        if (key.startsWith("block.minecraft.")) {
            return key.substring("block.minecraft.".length());
        }
        if (key.startsWith("entity.minecraft.")) {
            return key.substring("entity.minecraft.".length());
        }
        return key;
    }

    private static String normalizeComparableToken(String value) {
        return toSnakeCase(value.trim())
                .toLowerCase(Locale.ROOT)
                .replace(" ", "")
                .replace("_", "")
                .replace("-", "")
                .replace(":", "")
                .replace(".", "");
    }

    private static String toSnakeCase(String value) {
        StringBuilder out = new StringBuilder(value.length() + 8);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isUpperCase(c)) {
                if (i > 0 && value.charAt(i - 1) != '_' && value.charAt(i - 1) != ':' && value.charAt(i - 1) != '.') {
                    out.append('_');
                }
                out.append(Character.toLowerCase(c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    private static void showFishingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

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
                    StatsTranslationHelper.sendSuccess(source, "stats.fishing.item_count", translateItemType(type, language), count);
                }
            }
        } else {
            String resolvedType = resolveItemInput(itemType, stats.getFishCaughtByType(), language);
            int count = stats.getFishCaughtByType().getOrDefault(resolvedType, 0);
            StatsTranslationHelper.sendSuccess(source, "stats.header.fishing", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.fishing.item_detail", translateItemType(resolvedType, language), count);
        }
    }

    private static void showCraftingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

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
                    String displayName = translateItemType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.crafting.item_count", displayName, count);
                }
            }
        } else {
            String resolvedType = resolveItemInput(itemType, stats.getItemsCraftedByType(), language);
            int count = stats.getItemsCraftedByType().getOrDefault(resolvedType, 0);
            String displayName = translateItemType(resolvedType, language);
            StatsTranslationHelper.sendSuccess(source, "stats.header.crafting", playerName);
            StatsTranslationHelper.sendSuccess(source, "stats.crafting.item_detail", displayName, count);
        }
    }

    private static void showDropsStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);
        String language = getPreferredLanguage(source);

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
                    String displayName = translateItemType(type, language);
                    StatsTranslationHelper.sendSuccess(source, "stats.drops.item_count", displayName, count);
                }
            }
        } else {
            String resolvedType = resolveItemInput(itemType, stats.getItemsDroppedByType(), language);
            int count = stats.getItemsDroppedByType().getOrDefault(resolvedType, 0);
            String displayName = translateItemType(resolvedType, language);
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

