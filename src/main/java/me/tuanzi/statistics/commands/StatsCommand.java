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
                        context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                        context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                        context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                        context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                        context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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
                                context.getSource().sendSuccess(() -> Component.literal("§c此命令只能由玩家使用"), false);
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

        source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的统计数据 §e=========="), false);
        source.sendSuccess(() -> Component.literal("§b在线时间: §f" + hours + "小时 " + minutes + "分钟 " + seconds + "秒"), false);
        source.sendSuccess(() -> Component.literal("§b移动距离: §f" + String.format("%.2f", stats.getDistanceTraveled()) + " 米"), false);
        source.sendSuccess(() -> Component.literal("§b放置方块: §f" + stats.getBlocksPlaced()), false);
        source.sendSuccess(() -> Component.literal("§b破坏方块: §f" + stats.getBlocksBroken()), false);
        source.sendSuccess(() -> Component.literal("§b击杀数: §f" + stats.getKills()), false);
        source.sendSuccess(() -> Component.literal("§b死亡数: §f" + stats.getDeaths()), false);
        source.sendSuccess(() -> Component.literal("§b造成伤害: §f" + stats.getDamageDealt()), false);
        source.sendSuccess(() -> Component.literal("§b受到伤害: §f" + stats.getDamageTaken()), false);
        source.sendSuccess(() -> Component.literal("§b耐久消耗: §f" + stats.getDurabilityUsed()), false);
    }

    private static void showServerStats(CommandSourceStack source) {
        ServerStatistics stats = StatisticsModule.getInstance().getDataManager().getServerStatistics();

        source.sendSuccess(() -> Component.literal("§e========== §6服务器统计 §e=========="), false);
        source.sendSuccess(() -> Component.literal("§b总运行时间: §f" + stats.getTotalUptimeDays() + "天 " + (stats.getTotalUptimeHours() % 24) + "小时 " + (stats.getTotalUptimeMinutes() % 60) + "分钟 " + (stats.getTotalUptimeSeconds() % 60) + "秒"), false);
        source.sendSuccess(() -> Component.literal("§b当前会话运行时间: §f" + (stats.getCurrentSessionUptimeSeconds() / 3600) + "小时 " + ((stats.getCurrentSessionUptimeSeconds() % 3600) / 60) + "分钟 " + (stats.getCurrentSessionUptimeSeconds() % 60) + "秒"), false);
    }

    private static void showKillsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的击杀统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总击杀数: §f" + stats.getKills()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按实体类型分类 ---"), false);

            Map<String, Integer> killsByType = stats.getKillsByEntityType();
            if (killsByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无击杀记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : killsByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.literal("§f" + displayName + ": §a" + count), false);
                }
            }
        } else {
            String originalType = findOriginalEntityType(stats.getKillsByEntityType(), entityType);
            int kills = stats.getKillsByEntityType().getOrDefault(originalType, 0);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的击杀统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + displayName + " 击杀数: §f" + kills), false);
        }
    }

    private static void showDeathsStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的死亡统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总死亡数: §f" + stats.getDeaths()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按实体类型分类 ---"), false);

            Map<String, Integer> deathsByType = stats.getDeathsByEntityType();
            if (deathsByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无死亡记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : deathsByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.literal("§f" + displayName + ": §c" + count), false);
                }
            }
        } else {
            String originalType = findOriginalEntityType(stats.getDeathsByEntityType(), entityType);
            int deaths = stats.getDeathsByEntityType().getOrDefault(originalType, 0);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的死亡统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b被 " + displayName + " 击杀数: §f" + deaths), false);
        }
    }

    private static void showBlocksStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的方块统计 §e=========="), false);
        source.sendSuccess(() -> Component.literal("§b总放置方块: §f" + stats.getBlocksPlaced()), false);
        source.sendSuccess(() -> Component.literal("§b总破坏方块: §f" + stats.getBlocksBroken()), false);
        source.sendSuccess(() -> Component.literal("§7使用 /stats blocks placed 查看放置详情"), false);
        source.sendSuccess(() -> Component.literal("§7使用 /stats blocks broken 查看破坏详情"), false);
    }

    private static void showBlocksPlacedStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (blockType == null || blockType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的放置方块统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总放置方块: §f" + stats.getBlocksPlaced()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按方块类型分类 ---"), false);

            Map<String, Integer> blocksByType = stats.getBlocksPlacedByType();
            if (blocksByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无放置记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : blocksByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateBlockKey(type);
                    source.sendSuccess(() -> Component.literal("§f" + displayName + ": §a" + count), false);
                }
            }
        } else {
            int placed = stats.getBlocksPlacedByType().getOrDefault(blockType, 0);
            String displayName = TranslationHelper.translateBlockKey(blockType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的放置方块统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + displayName + " 放置数: §f" + placed), false);
        }
    }

    private static void showBlocksBrokenStats(CommandSourceStack source, String playerName, String blockType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (blockType == null || blockType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的破坏方块统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总破坏方块: §f" + stats.getBlocksBroken()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按方块类型分类 ---"), false);

            Map<String, Integer> blocksByType = stats.getBlocksBrokenByType();
            if (blocksByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无破坏记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : blocksByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateBlockKey(type);
                    source.sendSuccess(() -> Component.literal("§f" + displayName + ": §c" + count), false);
                }
            }
        } else {
            int broken = stats.getBlocksBrokenByType().getOrDefault(blockType, 0);
            String displayName = TranslationHelper.translateBlockKey(blockType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的破坏方块统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + displayName + " 破坏数: §f" + broken), false);
        }
    }

    private static void showDamageStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的伤害统计 §e=========="), false);
        source.sendSuccess(() -> Component.literal("§b总造成伤害: §f" + stats.getDamageDealt() + " 点"), false);
        source.sendSuccess(() -> Component.literal("§b总受到伤害: §f" + stats.getDamageTaken() + " 点"), false);
        source.sendSuccess(() -> Component.literal("§7使用 /stats damage dealt 查看造成伤害详情"), false);
        source.sendSuccess(() -> Component.literal("§7使用 /stats damage taken 查看受到伤害详情"), false);
    }

    private static void showDamageDealtStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的造成伤害统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总造成伤害: §f" + stats.getDamageDealt() + " 点"), false);
            source.sendSuccess(() -> Component.literal("§e--- 按实体类型分类 ---"), false);

            Map<String, Long> damageByType = stats.getDamageDealtByEntityType();
            if (damageByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无造成伤害记录"), false);
            } else {
                for (Map.Entry<String, Long> entry : damageByType.entrySet()) {
                    String type = entry.getKey();
                    long damage = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.literal("§f对 " + displayName + ": §a" + damage + " 点"), false);
                }
            }
        } else {
            String originalType = findOriginalEntityTypeForDamage(stats.getDamageDealtByEntityType(), entityType);
            long damage = stats.getDamageDealtByEntityType().getOrDefault(originalType, 0L);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的造成伤害统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b对 " + displayName + " 造成伤害: §f" + damage + " 点"), false);
        }
    }

    private static void showDamageTakenStats(CommandSourceStack source, String playerName, String entityType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (entityType == null || entityType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的受到伤害统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总受到伤害: §f" + stats.getDamageTaken() + " 点"), false);
            source.sendSuccess(() -> Component.literal("§e--- 按实体类型分类 ---"), false);

            Map<String, Long> damageByType = stats.getDamageTakenByEntityType();
            if (damageByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无受到伤害记录"), false);
            } else {
                for (Map.Entry<String, Long> entry : damageByType.entrySet()) {
                    String type = entry.getKey();
                    long damage = entry.getValue();
                    String displayName = translateEntityType(type);
                    source.sendSuccess(() -> Component.literal("§f来自 " + displayName + ": §c" + damage + " 点"), false);
                }
            }
        } else {
            String originalType = findOriginalEntityTypeForDamage(stats.getDamageTakenByEntityType(), entityType);
            long damage = stats.getDamageTakenByEntityType().getOrDefault(originalType, 0L);
            String displayName = translateEntityType(originalType != null ? originalType : entityType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的受到伤害统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b来自 " + displayName + " 的伤害: §f" + damage + " 点"), false);
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
            return "未知";
        }

        String lower = entityType.toLowerCase();
        if (lower.contains("zombie")) {
            return "僵尸";
        } else if (lower.contains("skeleton")) {
            return "骷髅";
        } else if (lower.contains("creeper")) {
            return "苦力怕";
        } else if (lower.contains("spider")) {
            return "蜘蛛";
        } else if (lower.contains("enderman")) {
            return "末影人";
        } else if (lower.contains("blaze")) {
            return "烈焰人";
        } else if (lower.contains("ghast")) {
            return "恶魂";
        } else if (lower.contains("witch")) {
            return "女巫";
        } else if (lower.contains("slime")) {
            return "史莱姆";
        } else if (lower.contains("pig")) {
            return "猪";
        } else if (lower.contains("cow")) {
            return "牛";
        } else if (lower.contains("sheep")) {
            return "羊";
        } else if (lower.contains("chicken")) {
            return "鸡";
        } else if (lower.contains("player")) {
            return "玩家";
        } else if (lower.contains("villager")) {
            return "村民";
        } else if (lower.contains("pillager")) {
            return "掠夺者";
        } else if (lower.contains("drowned")) {
            return "溺尸";
        } else if (lower.contains("phantom")) {
            return "幻术师";
        } else if (lower.contains("wither")) {
            return "凋灵";
        } else if (lower.contains("warden")) {
            return "监守者";
        } else if (lower.contains("guardian")) {
            return "守卫者";
        } else if (lower.contains("elder")) {
            return "末影龙";
        } else if (lower.contains("dragon")) {
            return "龙";
        } else if (lower.contains("iron_golem") || lower.contains("irongolem")) {
            return "铁傀儡";
        } else if (lower.contains("snow_golem") || lower.contains("snowgolem")) {
            return "雪傀儡";
        }

        return entityType;
    }

    private static void showFishingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的钓鱼统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总钓鱼次数: §f" + stats.getFishingAttempts()), false);
            source.sendSuccess(() -> Component.literal("§b成功次数: §a" + stats.getFishingSuccess()), false);
            source.sendSuccess(() -> Component.literal("§b失败次数: §c" + stats.getFishingFailures()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按物品类型分类 ---"), false);

            Map<String, Integer> fishByType = stats.getFishCaughtByType();
            if (fishByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无钓鱼记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : fishByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    source.sendSuccess(() -> Component.literal("§f" + type + ": §a" + count), false);
                }
            }
        } else {
            int count = stats.getFishCaughtByType().getOrDefault(itemType, 0);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的钓鱼统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + itemType + " 钓获数: §f" + count), false);
        }
    }

    private static void showCraftingStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的合成统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总合成次数: §f" + stats.getItemsCrafted()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按物品类型分类 ---"), false);

            Map<String, Integer> craftedByType = stats.getItemsCraftedByType();
            if (craftedByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无合成记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : craftedByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateItemKey(type);
                    source.sendSuccess(() -> Component.literal("§f" + displayName + ": §a" + count), false);
                }
            }
        } else {
            int count = stats.getItemsCraftedByType().getOrDefault(itemType, 0);
            String displayName = TranslationHelper.translateItemKey(itemType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的合成统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + displayName + " 合成数: §f" + count), false);
        }
    }

    private static void showDropsStats(CommandSourceStack source, String playerName, String itemType) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        if (itemType == null || itemType.isEmpty()) {
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的丢弃物品统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b总丢弃次数: §f" + stats.getItemsDropped()), false);
            source.sendSuccess(() -> Component.literal("§e--- 按物品类型分类 ---"), false);

            Map<String, Integer> droppedByType = stats.getItemsDroppedByType();
            if (droppedByType.isEmpty()) {
                source.sendSuccess(() -> Component.literal("§7暂无丢弃记录"), false);
            } else {
                for (Map.Entry<String, Integer> entry : droppedByType.entrySet()) {
                    String type = entry.getKey();
                    int count = entry.getValue();
                    String displayName = TranslationHelper.translateItemKey(type);
                    source.sendSuccess(() -> Component.literal("§f" + displayName + ": §c" + count), false);
                }
            }
        } else {
            int count = stats.getItemsDroppedByType().getOrDefault(itemType, 0);
            String displayName = TranslationHelper.translateItemKey(itemType);
            source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的丢弃物品统计 §e=========="), false);
            source.sendSuccess(() -> Component.literal("§b" + displayName + " 丢弃数: §f" + count), false);
        }
    }

    private static void showPlayerInfoStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的玩家信息 §e=========="), false);

        long firstJoinTime = stats.getFirstJoinTime();
        if (firstJoinTime > 0) {
            java.time.Instant instant = java.time.Instant.ofEpochSecond(firstJoinTime);
            java.time.ZonedDateTime zdt = instant.atZone(java.time.ZoneId.systemDefault());
            String formattedDate = zdt.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            source.sendSuccess(() -> Component.literal("§b首次加入时间: §f" + formattedDate), false);
        } else {
            source.sendSuccess(() -> Component.literal("§b首次加入时间: §f未知"), false);
        }

        source.sendSuccess(() -> Component.literal("§b登录天数: §f" + stats.getLoginDays() + " 天"), false);
    }

    private static void showActivityStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的活动统计 §e=========="), false);
        source.sendSuccess(() -> Component.literal("§b使用铁砧次数: §f" + stats.getAnvilUses()), false);
        source.sendSuccess(() -> Component.literal("§b附魔物品数: §f" + stats.getItemsEnchanted()), false);
        source.sendSuccess(() -> Component.literal("§b村民交易次数: §f" + stats.getVillagerTrades()), false);
        source.sendSuccess(() -> Component.literal("§b发送聊天消息数: §f" + stats.getChatMessagesSent()), false);
    }

    private static void showExtendedStats(CommandSourceStack source, String playerName) {
        PlayerStatistics stats = StatisticsModule.getInstance().getDataManager().getPlayerStatistics(playerName);

        source.sendSuccess(() -> Component.literal("§e========== §6" + playerName + " 的扩展统计 §e=========="), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 经验统计 ---"), false);
        source.sendSuccess(() -> Component.literal("§f获得经验总量: §a" + stats.getTotalExperienceGained()), false);
        source.sendSuccess(() -> Component.literal("§f消耗经验总量: §c" + stats.getTotalExperienceConsumed()), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 战斗统计 ---"), false);
        source.sendSuccess(() -> Component.literal("§f弓命中次数: §a" + stats.getBowHits()), false);
        source.sendSuccess(() -> Component.literal("§f弩命中次数: §a" + stats.getCrossbowHits()), false);
        source.sendSuccess(() -> Component.literal("§f盾牌格挡次数: §a" + stats.getShieldBlocks()), false);
        source.sendSuccess(() -> Component.literal("§f最高单次造成伤害: §a" + stats.getHighestDamageDealt() + "点"), false);
        source.sendSuccess(() -> Component.literal("§f最高单次受到伤害: §c" + stats.getHighestDamageTaken() + "点"), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 宠物统计 ---"), false);
        source.sendSuccess(() -> Component.literal("§f动物驯服总数: §a" + stats.getAnimalsTamed()), false);
        source.sendSuccess(() -> Component.literal("§f宠物死亡次数: §c" + stats.getPetDeaths()), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 探索统计 ---"), false);
        source.sendSuccess(() -> Component.literal("§f探索区块数: §a" + stats.getExploredChunks()), false);
        source.sendSuccess(() -> Component.literal("§f累计跳跃次数: §a" + stats.getTotalJumps()), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 时间统计 ---"), false);
        long longestSession = stats.getLongestSessionSeconds();
        long longestHours = longestSession / 3600;
        long longestMinutes = (longestSession % 3600) / 60;
        long longestSeconds = longestSession % 60;
        source.sendSuccess(() -> Component.literal("§f最长在线时长: §a" + longestHours + "时" + longestMinutes + "分" + longestSeconds + "秒"), false);
        
        long sneakTime = stats.getTotalSneakSeconds();
        long sneakHours = sneakTime / 3600;
        long sneakMinutes = (sneakTime % 3600) / 60;
        long sneakSeconds = sneakTime % 60;
        source.sendSuccess(() -> Component.literal("§f累计潜行时间: §a" + sneakHours + "时" + sneakMinutes + "分" + sneakSeconds + "秒"), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 寿命统计 ---"), false);
        long shortestLife = stats.getShortestLifeSeconds();
        long shortMinutes = shortestLife / 60;
        long shortSeconds = shortestLife % 60;
        source.sendSuccess(() -> Component.literal("§f最短寿命: §c" + shortMinutes + "分" + shortSeconds + "秒"), false);
        
        long longestLife = stats.getLongestLifeSeconds();
        long lifeHours = longestLife / 3600;
        long lifeMinutes = (longestLife % 3600) / 60;
        long lifeSeconds = longestLife % 60;
        source.sendSuccess(() -> Component.literal("§f最长寿命: §a" + lifeHours + "时" + lifeMinutes + "分" + lifeSeconds + "秒"), false);
        
        source.sendSuccess(() -> Component.literal("§b--- 死亡统计 ---"), false);
        double farthestDistance = stats.getFarthestDeathDistance();
        source.sendSuccess(() -> Component.literal("§f最远死亡掉落距离: §c" + String.format("%.1f", farthestDistance) + "米"), false);
    }
}
