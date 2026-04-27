package me.tuanzi.cdk.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import me.tuanzi.cdk.*;
import me.tuanzi.economy.utils.ServerTranslationHelper;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class CDKAdminCommand {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH:mm:ss");

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess) {
        dispatcher.register(Commands.literal("cdkadmin")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(Commands.literal("create")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .then(Commands.argument("type", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(Arrays.stream(CDKType.values()).map(CDKType::name), b))
                                        .then(Commands.argument("maxUses", IntegerArgumentType.integer(-1))
                                                .executes(CDKAdminCommand::createCDK)))))
                .then(Commands.literal("generate")
                        .then(Commands.argument("baseCode", StringArgumentType.word())
                                .suggests(CDKAdminCommand::suggestCDKs)
                                .then(Commands.argument("count", IntegerArgumentType.integer(1, 500))
                                        .executes(CDKAdminCommand::generateCDKs))))
                .then(Commands.literal("addcmd")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .suggests(CDKAdminCommand::suggestCDKs)
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(CDKAdminCommand::addCommand))))
                .then(Commands.literal("setmsg")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .suggests(CDKAdminCommand::suggestCDKs)
                                .then(Commands.argument("message", StringArgumentType.greedyString())
                                        .executes(CDKAdminCommand::setMessage))))
                .then(Commands.literal("setexpiry")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .suggests(CDKAdminCommand::suggestCDKs)
                                .then(Commands.argument("date", StringArgumentType.word())
                                        .executes(CDKAdminCommand::setExpiry))))
                .then(Commands.literal("delete")
                        .then(Commands.argument("code", StringArgumentType.word())
                                .suggests(CDKAdminCommand::suggestCDKs)
                                .executes(CDKAdminCommand::deleteCDK)))
                .then(Commands.literal("list")
                        .executes(ctx -> listCDKs(ctx, 1))
                        .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                .executes(ctx -> listCDKs(ctx, IntegerArgumentType.getInteger(ctx, "page")))))
                .then(Commands.literal("history")
                        .then(Commands.argument("player", EntityArgument.player())
                                .executes(ctx -> showHistory(ctx, 1))
                                .then(Commands.argument("page", IntegerArgumentType.integer(1))
                                        .executes(ctx -> showHistory(ctx, IntegerArgumentType.getInteger(ctx, "page"))))))
                .then(Commands.literal("export")
                        .then(Commands.argument("type", StringArgumentType.word())
                                .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("history", "pool"), b))
                                .then(Commands.argument("format", StringArgumentType.word())
                                        .suggests((ctx, b) -> SharedSuggestionProvider.suggest(List.of("csv", "json", "txt"), b))
                                        .executes(CDKAdminCommand::exportData)))));
    }

    private static CompletableFuture<Suggestions> suggestCDKs(CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        return SharedSuggestionProvider.suggest(CDKManager.getInstance().getCDKData().getCdks().keySet(), builder);
    }

    private static int createCDK(CommandContext<CommandSourceStack> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        CDKType type = CDKType.valueOf(StringArgumentType.getString(ctx, "type").toUpperCase());
        int maxUses = IntegerArgumentType.getInteger(ctx, "maxUses");

        String finalCode = CDKManager.getInstance().createCDK(code, type, maxUses);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.create.success", finalCode);
        return 1;
    }

    private static int generateCDKs(CommandContext<CommandSourceStack> ctx) {
        String baseCode = StringArgumentType.getString(ctx, "baseCode");
        int count = IntegerArgumentType.getInteger(ctx, "count");

        int success = CDKManager.getInstance().bulkGenerate(baseCode, count);
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.generate.success", baseCode, success);
        return 1;
    }

    private static int addCommand(CommandContext<CommandSourceStack> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        String cmd = StringArgumentType.getString(ctx, "command");

        CDKEntry entry = CDKManager.getInstance().getCDKData().getCDK(code);
        if (entry != null) {
            entry.getCommands().add(cmd);
            CDKStateSaver.getServerState(ctx.getSource().getServer()).setDirty();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.addcmd.success", code);
        }
        return 1;
    }

    private static int setMessage(CommandContext<CommandSourceStack> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        String msg = StringArgumentType.getString(ctx, "message").replace("&", "§");

        CDKEntry entry = CDKManager.getInstance().getCDKData().getCDK(code);
        if (entry != null) {
            entry.setSuccessMessage(msg);
            CDKStateSaver.getServerState(ctx.getSource().getServer()).setDirty();
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.setmsg.success", msg);
        }
        return 1;
    }

    private static int setExpiry(CommandContext<CommandSourceStack> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        String dateStr = StringArgumentType.getString(ctx, "date");

        try {
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, DATE_FORMATTER);
            long timestamp = dateTime.toInstant(ZoneOffset.ofHours(8)).toEpochMilli();
            
            CDKEntry entry = CDKManager.getInstance().getCDKData().getCDK(code);
            if (entry != null) {
                entry.setExpireTime(timestamp);
                CDKStateSaver.getServerState(ctx.getSource().getServer()).setDirty();
                ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.setexpiry.success", dateStr);
            }
        } catch (Exception e) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "cdk.admin.setexpiry.error");
        }
        return 1;
    }

    private static int deleteCDK(CommandContext<CommandSourceStack> ctx) {
        String code = StringArgumentType.getString(ctx, "code");
        CDKManager.getInstance().getCDKData().removeCDK(code);
        CDKStateSaver.getServerState(ctx.getSource().getServer()).setDirty();
        ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.delete.success", code);
        return 1;
    }

    private static int listCDKs(CommandContext<CommandSourceStack> ctx, int page) {
        List<CDKEntry> all = new ArrayList<>(CDKManager.getInstance().getCDKData().getCdks().values());
        int pageSize = 8;
        int totalPages = (int) Math.ceil((double) all.size() / pageSize);
        if (page > totalPages && totalPages > 0) page = totalPages;

        final int currentPage = page;
        final int finalTotalPages = totalPages;
        String lang = ServerTranslationHelper.getLanguage(ctx.getSource());
        
        ctx.getSource().sendSuccess(() -> ServerTranslationHelper.translateToComponent("cdk.admin.list.header", lang, currentPage, finalTotalPages), false);
        
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        
        String delBtnText = ServerTranslationHelper.translate("cdk.admin.list.delete_btn", lang);
        String delHoverText = ServerTranslationHelper.translate("cdk.admin.list.delete_hover", lang);
        String copyBtnText = ServerTranslationHelper.translate("cdk.admin.list.copy_btn", lang);
        String copyHoverText = ServerTranslationHelper.translate("cdk.admin.list.copy_hover", lang);

        for (int i = start; i < end; i++) {
            CDKEntry e = all.get(i);
            String statusKey = e.isExpired() ? "cdk.admin.list.status.expired" : (e.isFull() ? "cdk.admin.list.status.full" : "cdk.admin.list.status.valid");
            String status = ServerTranslationHelper.translate(statusKey, lang);
            
            Component deleteBtn = Component.literal(delBtnText)
                    .setStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(delHoverText)))
                        .withClickEvent(new ClickEvent.RunCommand("/cdkadmin delete " + e.getCode())));

            Component copyBtn = Component.literal(copyBtnText)
                    .setStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(copyHoverText)))
                        .withClickEvent(new ClickEvent.CopyToClipboard(e.getCode())));

            String cmds = e.getCommands().isEmpty() ? "  (无指令)" : "  - " + String.join("\n  - ", e.getCommands());
            String hoverDetail = ServerTranslationHelper.translate("cdk.admin.list.item_hover", lang, e.getType().toString(), cmds);

            Component line = Component.literal("§e" + e.getCode() + " " + status + " §7(" + e.getCurrentUses() + "/" + (e.getMaxUses() == -1 ? "∞" : e.getMaxUses()) + ")")
                    .setStyle(Style.EMPTY
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverDetail)))
                        .withClickEvent(new ClickEvent.SuggestCommand("/cdk " + e.getCode())))
                    .append(copyBtn)
                    .append(deleteBtn);

            ctx.getSource().sendSuccess(() -> line, false);
        }
        return 1;
    }

    private static int showHistory(CommandContext<CommandSourceStack> ctx, int page) {
        try {
            ServerPlayer target = EntityArgument.getPlayer(ctx, "player");
            List<CDKHistoryEntry> history = CDKManager.getInstance().getHistoryManager().getPlayerHistory(target.getUUID());
            String lang = ServerTranslationHelper.getLanguage(ctx.getSource());

            ctx.getSource().sendSuccess(() -> ServerTranslationHelper.translateToComponent("cdk.admin.history.header", lang, target.getName().getString()), false);
            if (history.isEmpty()) {
                ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.history.empty");
                return 1;
            }

            for (CDKHistoryEntry entry : history) {
                String cmds = (entry.commands() == null || entry.commands().isEmpty()) ? "  (无记录)" : "  - " + String.join("\n  - ", entry.commands());
                String hoverDetail = ServerTranslationHelper.translate("cdk.admin.history.hover", lang, cmds);

                Component line = Component.literal("§7[" + entry.timestamp() + "] §f" + entry.code())
                        .setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.literal(hoverDetail))));
                
                ctx.getSource().sendSuccess(() -> line, false);
            }
        } catch (Exception ignored) {}
        return 1;
    }

    private static int exportData(CommandContext<CommandSourceStack> ctx) {
        String type = StringArgumentType.getString(ctx, "type").toLowerCase();
        String format = StringArgumentType.getString(ctx, "format").toLowerCase();
        try {
            String filename;
            if (type.equals("history")) {
                filename = CDKManager.getInstance().getHistoryManager().exportHistory(format);
            } else {
                filename = CDKManager.getInstance().getHistoryManager().exportCDKs(format, CDKManager.getInstance().getCDKData().getCdks().values());
            }
            
            ServerTranslationHelper.sendSuccess(ctx.getSource(), "cdk.admin.export.success", filename);
        } catch (Exception e) {
            ServerTranslationHelper.sendFailure(ctx.getSource(), "cdk.admin.export.failure", e.getMessage());
        }
        return 1;
    }
}
