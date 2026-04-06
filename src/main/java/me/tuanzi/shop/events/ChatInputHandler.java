package me.tuanzi.shop.events;

import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.display.ShopDisplayManager;
import me.tuanzi.shop.logging.TransactionLogger;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopType;
import me.tuanzi.shop.util.DevFlowLogger;
import me.tuanzi.shop.util.SignUpdateHelper;
import me.tuanzi.shop.utils.ItemUtils;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class ChatInputHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopModule/ChatInput");
    private final ShopManager shopManager;
    private final TransactionLogger transactionLogger;

    private final Map<UUID, PendingTransaction> pendingTransactions = new ConcurrentHashMap<>();
    private final Map<UUID, PendingItemChange> pendingItemChanges = new ConcurrentHashMap<>();
    private final Map<UUID, PendingShopCreation> pendingShopCreations = new ConcurrentHashMap<>();

    public ChatInputHandler(ShopManager shopManager, TransactionLogger transactionLogger) {
        this.shopManager = shopManager;
        this.transactionLogger = transactionLogger;
    }

    public void startTransactionInput(ServerPlayer player, ShopInstance shop) {
        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        PendingTransaction pending = new PendingTransaction(shop.getShopId(), expireTime);
        pendingTransactions.put(player.getUUID(), pending);

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.prompt", timeout));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7输入 'cancel' 或 '取消' 可取消交易"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
    }

    public void startItemChangeConfirmation(ServerPlayer player, ShopInstance shop, ItemStack newItem) {
        DevFlowLogger.startFlow("物品修改流程");
        DevFlowLogger.param("物品修改流程", "player", player.getName().getString());
        DevFlowLogger.param("物品修改流程", "shopId", shop.getShopId());
        DevFlowLogger.param("物品修改流程", "oldItem", shop.getTradeItem().getDisplayName().getString());
        DevFlowLogger.param("物品修改流程", "newItem", newItem.getDisplayName().getString());

        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        DevFlowLogger.step("物品修改流程", "初始化待处理状态", "超时时间=", timeout, "秒");
        PendingItemChange pending = new PendingItemChange(shop.getShopId(), newItem.copy(), expireTime);
        pendingItemChanges.put(player.getUUID(), pending);

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.prompt", newItem.getDisplayName().getString()));

        Component confirmBtn = Component.literal(ShopTranslationHelper.getRawTranslation("item.change.confirm"))
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/shopconfirm yes"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§a点击确认更换"))));

        Component cancelBtn = Component.literal(ShopTranslationHelper.getRawTranslation("item.change.cancel"))
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/shopconfirm no"))
                        .withHoverEvent(new HoverEvent.ShowText(Component.literal("§c点击取消更换"))));

        player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.literal(" ")).append(cancelBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7或输入 'yes/是' 确认, 'no/否' 取消"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

        DevFlowLogger.status("物品修改流程", "已向玩家发送确认提示，等待确认");
    }

    public boolean handleChatInput(ServerPlayer player, String message) {
        UUID playerId = player.getUUID();

        if (pendingShopCreations.containsKey(playerId)) {
            return handleShopCreationInput(player, message.trim());
        }

        if (pendingItemChanges.containsKey(playerId)) {
            return handleItemChangeInput(player, message.trim().toLowerCase());
        }

        if (pendingTransactions.containsKey(playerId)) {
            return handleTransactionInput(player, message.trim());
        }

        return false;
    }

    private boolean handleTransactionInput(ServerPlayer player, String input) {
        UUID playerId = player.getUUID();
        PendingTransaction pending = pendingTransactions.get(playerId);

        if (pending == null) {
            return false;
        }

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.timeout"));
            return true;
        }

        String lowerInput = input.toLowerCase();
        if (lowerInput.equals("cancel") || lowerInput.equals("取消")) {
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.cancelled"));
            return true;
        }

        int quantity;
        try {
            quantity = Integer.parseInt(input);
            if (quantity <= 0) {
                player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.invalid"));
                return true;
            }
            int maxQty = ShopModule.getConfig().getMaxTransactionQuantity();
            if (quantity > maxQty) {
                player.sendSystemMessage(ShopTranslationHelper.colored("§c数量超过最大限制: " + maxQty));
                return true;
            }
        } catch (NumberFormatException e) {
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.invalid"));
            return true;
        }

        pendingTransactions.remove(playerId);

        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.not_found"));
            return true;
        }

        ShopInstance shop = shopOpt.get();
        
        if (!shop.isValid()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c此商店已不存在"));
            return true;
        }
        
        executeTransaction(player, shop, quantity);

        return true;
    }

    private boolean handleItemChangeInput(ServerPlayer player, String input) {
        DevFlowLogger.step("物品修改流程", "处理玩家确认输入", "输入=", input);

        UUID playerId = player.getUUID();
        PendingItemChange pending = pendingItemChanges.get(playerId);

        if (pending == null) {
            DevFlowLogger.warning("物品修改流程", "未找到待处理的物品修改请求");
            return false;
        }

        if (System.currentTimeMillis() > pending.expireTime) {
            DevFlowLogger.warning("物品修改流程", "请求已超时");
            pendingItemChanges.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.timeout"));
            DevFlowLogger.endFlow("物品修改流程", false, "请求超时");
            return true;
        }

        boolean confirmed = input.equals("yes") || input.equals("是") || input.equals("confirm");
        boolean cancelled = input.equals("no") || input.equals("否") || input.equals("cancel");

        if (!confirmed && !cancelled) {
            return false;
        }

        pendingItemChanges.remove(playerId);

        if (cancelled) {
            DevFlowLogger.status("物品修改流程", "玩家取消物品修改");
            player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.cancelled"));
            DevFlowLogger.endFlow("物品修改流程", false, "玩家取消");
            return true;
        }

        DevFlowLogger.status("物品修改流程", "玩家确认修改，开始执行");

        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty()) {
            DevFlowLogger.error("物品修改流程", "商店不存在: " + pending.shopId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.not_found"));
            DevFlowLogger.endFlow("物品修改流程", false, "商店不存在");
            return true;
        }

        ShopInstance shop = shopOpt.get();
        
        if (!shop.isValid()) {
            DevFlowLogger.error("物品修改流程", "商店已被删除");
            player.sendSystemMessage(ShopTranslationHelper.colored("§c此商店已不存在"));
            DevFlowLogger.endFlow("物品修改流程", false, "商店已删除");
            return true;
        }
        
        DevFlowLogger.step("物品修改流程", "更新商店物品");
        shop.setTradeItem(pending.newItem.copy());
        shopManager.markDirty();
        DevFlowLogger.param("物品修改流程", "新物品", pending.newItem.getDisplayName().getString());

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ShopModule instance = ShopModule.getInstance(server);
            if (instance != null && instance.getDisplayManager() != null) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    DevFlowLogger.step("物品修改流程", "更新悬浮物品显示");
                    instance.getDisplayManager().updateDisplayItem(shop.getShopId(), pending.newItem, serverLevel);
                    LOGGER.info("[商店调试] 物品修改(聊天确认) - 已更新商店{} 的悬浮物品显示", shop.getShopId());
                    DevFlowLogger.status("物品修改流程", "悬浮物品显示已更新");
                    
                    // 使用 SignUpdateHelper 统一更新告示牌文本
                    SignUpdateHelper.updateSignForShop(shop, serverLevel);
                    DevFlowLogger.status("物品修改流程", "告示牌已通过SignUpdateHelper刷新");
                }
            }
        }

        player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.success", pending.newItem.getDisplayName().getString()));

        DevFlowLogger.endFlow("物品修改流程", true, 
                "物品修改成功 - 新物品: " + pending.newItem.getDisplayName().getString());
        return true;
    }

    public boolean handleConfirmCommand(ServerPlayer player, boolean confirmed) {
        UUID playerId = player.getUUID();
        PendingItemChange pending = pendingItemChanges.get(playerId);

        if (pending == null) {
            return false;
        }

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingItemChanges.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.timeout"));
            return true;
        }

        pendingItemChanges.remove(playerId);

        if (!confirmed) {
            player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.cancelled"));
            return true;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.not_found"));
            return true;
        }

        ShopInstance shop = shopOpt.get();
        
        if (!shop.isValid()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c此商店已不存在"));
            return true;
        }
        
        shop.setTradeItem(pending.newItem.copy());
        shopManager.markDirty();

        DevFlowLogger.step("物品修改流程", "更新商店物品数据");
        DevFlowLogger.param("物品修改流程", "newItem", pending.newItem.getDisplayName().getString());

        MinecraftServer serverCmd = player.level().getServer();
        if (serverCmd != null) {
            ShopModule instanceCmd = ShopModule.getInstance(serverCmd);
            if (instanceCmd != null && instanceCmd.getDisplayManager() != null) {
                if (player.level() instanceof ServerLevel serverLevelCmd) {
                    instanceCmd.getDisplayManager().updateDisplayItem(shop.getShopId(), pending.newItem, serverLevelCmd);
                    LOGGER.info("[商店调试] 物品修改(命令确认) - 已更新商店{} 的悬浮物品显示", shop.getShopId());
                    
                    // 使用 SignUpdateHelper 统一更新告示牌文本
                    SignUpdateHelper.updateSignForShop(shop, serverLevelCmd);
                    DevFlowLogger.status("物品修改流程", "告示牌已通过SignUpdateHelper刷新");
                }
            }
        }

        player.sendSystemMessage(ShopTranslationHelper.translatable("item.change.success", pending.newItem.getDisplayName().getString()));

        DevFlowLogger.endFlow("物品修改流程", true, 
                "物品修改成功 - 新物品: " + pending.newItem.getDisplayName().getString());
        return true;
    }

    public boolean handleConfirmCommand(ServerPlayer player, String action) {
        UUID playerId = player.getUUID();

        if (pendingShopCreations.containsKey(playerId)) {
            return handleShopCreationConfirm(player, action);
        }

        if (pendingItemChanges.containsKey(playerId)) {
            boolean confirmed = action.equals("yes") || action.equals("是");
            return handleConfirmCommand(player, confirmed);
        }

        return false;
    }

    private boolean handleShopCreationConfirm(ServerPlayer player, String action) {
        UUID playerId = player.getUUID();
        PendingShopCreation pending = pendingShopCreations.get(playerId);

        if (pending == null) {
            return false;
        }

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingShopCreations.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.timeout"));
            return true;
        }

        switch (pending.state) {
            case WAITING_CONFIRM:
                boolean confirmed = action.equals("yes") || action.equals("是");
                boolean cancelled = action.equals("no") || action.equals("否");
                
                if (!confirmed && !cancelled) {
                    return false;
                }
                
                if (cancelled) {
                    pendingShopCreations.remove(playerId);
                    player.sendSystemMessage(ShopTranslationHelper.colored("§c已取消创建商店"));
                    return true;
                }
                
                return handleCreationConfirm(player, pending, "yes");
                
            case SELECTING_TYPE:
                return handleCreationType(player, pending, action);
                
            default:
                return false;
        }
    }

    private void updateSignText(ServerPlayer player, ShopInstance shop, ItemStack newItem) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;

        ServerLevel level = player.level();
        if (level == null) return;

        BlockPos signPos = shop.getSignPos();
        if (!(level.getBlockEntity(signPos) instanceof SignBlockEntity signEntity)) return;

        boolean isSellShop = shop.getShopType() == ShopType.SELL;
        String currencyName = shopManager.getCurrencyDisplayName(shop.getWalletTypeId());
        double price = me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop);

        var frontText = signEntity.getFrontText()
                .setMessage(0, Component.literal(isSellShop ? "[出售]" : "[收购]"))
                .setMessage(1, newItem.getDisplayName())
                .setMessage(2, Component.literal(String.format("%.2f %s", price, currencyName)))
                .setMessage(3, Component.literal(""));
        signEntity.setText(frontText, true);
        signEntity.setChanged();
        
        level.sendBlockUpdated(signPos, level.getBlockState(signPos), level.getBlockState(signPos), 3);
        
        LOGGER.info("[商店调试] 告示牌文字已更新 - 商店ID: {}, 位置: {}, 已标记数据为脏数据等待保存", shop.getShopId(), signPos);
    }

    private void executeTransaction(ServerPlayer player, ShopInstance shop, int quantity) {
        boolean isBuy = shop.getShopType() == ShopType.SELL;
        String transactionType = isBuy ? "buy" : "sell";

        MinecraftServer server = player.level().getServer();
        if (server == null) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c服务器错误"));
            return;
        }

        BlockInteractionHandler handler = ShopModule.getInstance(server).getInteractionHandler();
        if (handler == null) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c交易处理器未初始化"));
            return;
        }

        handler.executeTransaction(player, shop.getShopId(), transactionType, quantity);
    }

    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();

        pendingTransactions.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expireTime) {
                return true;
            }
            return false;
        });

        pendingItemChanges.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expireTime) {
                return true;
            }
            return false;
        });
    }

    public void cleanupForShop(UUID shopId) {
        LOGGER.info("清理商店相关的待处理状态 - 商店ID: {}", shopId);
        
        pendingTransactions.entrySet().removeIf(entry -> 
            entry.getValue().shopId.equals(shopId));
        
        pendingItemChanges.entrySet().removeIf(entry -> 
            entry.getValue().shopId.equals(shopId));
        
        LOGGER.info("商店状态清理完成 - 商店ID: {}", shopId);
    }

    public boolean hasPendingTransaction(UUID playerId) {
        PendingTransaction pending = pendingTransactions.get(playerId);
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() > pending.expireTime) {
            pendingTransactions.remove(playerId);
            return false;
        }
        return true;
    }

    public boolean cancelPendingTransaction(UUID playerId) {
        PendingTransaction pending = pendingTransactions.remove(playerId);
        return pending != null;
    }

    public boolean hasPendingItemChange(UUID playerId) {
        PendingItemChange pending = pendingItemChanges.get(playerId);
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() > pending.expireTime) {
            pendingItemChanges.remove(playerId);
            return false;
        }
        return true;
    }

    private static class PendingTransaction {
        final UUID shopId;
        final long expireTime;

        PendingTransaction(UUID shopId, long expireTime) {
            this.shopId = shopId;
            this.expireTime = expireTime;
        }
    }

    private static class PendingItemChange {
        final UUID shopId;
        final ItemStack newItem;
        final long expireTime;

        PendingItemChange(UUID shopId, ItemStack newItem, long expireTime) {
            this.shopId = shopId;
            this.newItem = newItem;
            this.expireTime = expireTime;
        }
    }

    private enum ShopCreationState {
        WAITING_CONFIRM,
        SELECTING_TYPE,
        INPUTTING_PRICE,
        INPUTTING_CURRENCY,
        INPUTTING_NOTE
    }

    private static class PendingShopCreation {
        final BlockPos signPos;
        final BlockPos containerPos;
        final ItemStack item;
        ShopCreationState state;
        double price;
        String currencyId;
        String note;
        long expireTime;

        PendingShopCreation(BlockPos signPos, BlockPos containerPos, ItemStack item, long expireTime) {
            this.signPos = signPos;
            this.containerPos = containerPos;
            this.item = item;
            this.state = ShopCreationState.WAITING_CONFIRM;
            this.expireTime = expireTime;
        }
    }

    public boolean hasPendingShopCreation(UUID playerId) {
        PendingShopCreation pending = pendingShopCreations.get(playerId);
        if (pending == null) {
            return false;
        }
        if (System.currentTimeMillis() > pending.expireTime) {
            pendingShopCreations.remove(playerId);
            return false;
        }
        return true;
    }

    public void startShopCreation(ServerPlayer player, BlockPos signPos, ItemStack item, BlockPos containerPos) {
        DevFlowLogger.startFlow("商店创建流程(聊天)");
        DevFlowLogger.param("商店创建流程(聊天)", "player", player.getName().getString());
        DevFlowLogger.param("商店创建流程(聊天)", "playerUUID", player.getUUID());
        DevFlowLogger.param("商店创建流程(聊天)", "signPos", signPos);
        DevFlowLogger.param("商店创建流程(聊天)", "containerPos", containerPos);
        DevFlowLogger.param("商店创建流程(聊天)", "item", item.getDisplayName().getString());

        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        DevFlowLogger.step("商店创建流程(聊天)", "初始化待处理状态", "超时时间=", timeout, "秒");
        PendingShopCreation pending = new PendingShopCreation(signPos, containerPos, item, expireTime);
        pendingShopCreations.put(player.getUUID(), pending);
        DevFlowLogger.status("商店创建流程(聊天)", "已将玩家添加到待处理列表，等待确认");
        DevFlowLogger.param("商店创建流程(聊天)", "expireTime", expireTime);
        DevFlowLogger.param("商店创建流程(聊天)", "当前状态", "WAITING_CONFIRM");

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("shop.creation.prompt", item.getDisplayName().getString()));
        
        Component confirmBtn = Component.literal(ShopTranslationHelper.getRawTranslation("item.change.confirm"))
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm yes"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§a点击确认创建"))));
        
        Component cancelBtn = Component.literal(ShopTranslationHelper.getRawTranslation("item.change.cancel"))
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm no"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§c点击取消创建"))));
        
        player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.literal(" ")).append(cancelBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7或输入 'yes/是' 确认, 'no/否' 取消"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

        DevFlowLogger.status("商店创建流程(聊天)", "已向玩家发送确认提示消息");
    }

    private boolean handleShopCreationInput(ServerPlayer player, String input) {
        UUID playerId = player.getUUID();
        PendingShopCreation pending = pendingShopCreations.get(playerId);

        if (pending == null) {
            return false;
        }

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingShopCreations.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.input.timeout"));
            return true;
        }

        switch (pending.state) {
            case WAITING_CONFIRM:
                return handleCreationConfirm(player, pending, input);
            case SELECTING_TYPE:
                return handleCreationType(player, pending, input);
            case INPUTTING_PRICE:
                return handleCreationPrice(player, pending, input);
            case INPUTTING_CURRENCY:
                return handleCreationCurrency(player, pending, input);
            case INPUTTING_NOTE:
                return handleCreationNote(player, pending, input);
            default:
                return false;
        }
    }

    private boolean handleCreationConfirm(ServerPlayer player, PendingShopCreation pending, String input) {
        DevFlowLogger.step("商店创建流程(聊天)", "处理确认输入", "玩家输入=", input);

        boolean confirmed = input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("是") || input.equalsIgnoreCase("confirm");
        boolean cancelled = input.equalsIgnoreCase("no") || input.equalsIgnoreCase("否") || input.equalsIgnoreCase("cancel");

        if (!confirmed && !cancelled) {
            DevFlowLogger.status("商店创建流程(聊天)", "无效输入，等待重新输入");
            return false;
        }

        if (cancelled) {
            DevFlowLogger.step("商店创建流程(聊天)", "玩家取消创建");
            pendingShopCreations.remove(player.getUUID());
            player.sendSystemMessage(ShopTranslationHelper.colored("§c已取消创建商店"));
            DevFlowLogger.endFlow("商店创建流程(聊天)", false, "玩家主动取消");
            return true;
        }

        DevFlowLogger.status("商店创建流程(聊天)", "玩家确认创建，进入类型选择阶段");
        
        pending.state = ShopCreationState.SELECTING_TYPE;
        int timeout = 15;
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);
        pending.expireTime = expireTime;
        DevFlowLogger.param("商店创建流程(聊天)", "新状态", "SELECTING_TYPE");
        DevFlowLogger.param("商店创建流程(聊天)", "新超时时间", timeout + "秒");

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b请选择商店类型:"));
        
        Component sellBtn = Component.literal("§a[出售]")
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm sell"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§a点击创建出售商店"))));
        
        Component buyBtn = Component.literal("§a[收购]")
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm buy"))
                .withHoverEvent(new HoverEvent.ShowText(Component.literal("§a点击创建收购商店"))));
        
        player.sendSystemMessage(Component.empty().append(sellBtn).append(Component.literal(" ")).append(buyBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7或输入 'sell/出售' 或 'buy/收购'"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

        DevFlowLogger.status("商店创建流程(聊天)", "已向玩家发送类型选择提示");
        return true;
    }

    private boolean handleCreationType(ServerPlayer player, PendingShopCreation pending, String input) {
        DevFlowLogger.step("商店创建流程(聊天)", "处理类型选择", "玩家输入=", input);

        String lowerInput = input.toLowerCase();
        boolean isSell = lowerInput.equals("sell") || lowerInput.equals("出售");
        boolean isBuy = lowerInput.equals("buy") || lowerInput.equals("收购");

        if (!isSell && !isBuy) {
            DevFlowLogger.warning("商店创建流程(聊天)", "无效的类型输入: " + input);
            player.sendSystemMessage(ShopTranslationHelper.colored("§c无效的类型，请输入 'sell/出售' 或 'buy/收购'"));
            return true;
        }

        DevFlowLogger.status("商店创建流程(聊天)", "类型选择成功: " + (isSell ? "出售" : "收购"));

        pending.state = ShopCreationState.INPUTTING_PRICE;
        int timeout = 15;
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);
        pending.expireTime = expireTime;

        pending.note = isSell ? "出售" : "收购";
        DevFlowLogger.param("商店创建流程(聊天)", "新状态", "INPUTTING_PRICE");
        DevFlowLogger.param("商店创建流程(聊天)", "选择的类型", isSell ? "SELL(出售)" : "BUY(收购)");

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b请输入价格(数字):"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

        DevFlowLogger.status("商店创建流程(聊天)", "已向玩家发送价格输入提示");
        return true;
    }

    private boolean handleCreationPrice(ServerPlayer player, PendingShopCreation pending, String input) {
        DevFlowLogger.step("商店创建流程(聊天)", "处理价格输入", "玩家输入=", input);

        try {
            double price = Double.parseDouble(input);
            if (price <= 0) {
                DevFlowLogger.warning("商店创建流程(聊天)", "价格必须大于0，输入: " + price);
                player.sendSystemMessage(ShopTranslationHelper.colored("§c价格必须大于 0"));
                return true;
            }

            DevFlowLogger.param("商店创建流程(聊天)", "price", price);
            pending.price = price;
            pending.state = ShopCreationState.INPUTTING_CURRENCY;
            int timeout = 15;
            long expireTime = System.currentTimeMillis() + (timeout * 1000L);
            pending.expireTime = expireTime;

            DevFlowLogger.param("商店创建流程(聊天)", "新状态", "INPUTTING_CURRENCY");

            player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
            player.sendSystemMessage(ShopTranslationHelper.colored("§b请选择货币 (输入名称或ID):"));
            
            java.util.Collection<me.tuanzi.economy.currency.WalletType> walletTypes = shopManager.getEconomyAPI().getAllWalletTypes();
            if (walletTypes.isEmpty()) {
                DevFlowLogger.error("商店创建流程(聊天)", "没有可用的货币类型！");
                player.sendSystemMessage(ShopTranslationHelper.colored("§c当前没有可用的货币类型！请联系管理员添加货币。"));
                return true;
            }
            
            StringBuilder currencyList = new StringBuilder("§a");
            int index = 0;
            for (me.tuanzi.economy.currency.WalletType wt : walletTypes) {
                if (index > 0) {
                    currencyList.append("§7、");
                }
                currencyList.append(wt.displayName().getString())
                          .append("(")
                          .append("§e")
                          .append(wt.id())
                          .append("§a")
                          .append(")");
                index++;
            }
            
            player.sendSystemMessage(ShopTranslationHelper.colored(currencyList.toString()));
            player.sendSystemMessage(ShopTranslationHelper.colored("§7示例: 输入 '金币' 或 'gold_coin'"));
            player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

            DevFlowLogger.status("商店创建流程(聊天)", "已向玩家发送货币选择提示");
            DevFlowLogger.param("商店创建流程(聊天)", "可用货币数量", walletTypes.size());
        } catch (NumberFormatException e) {
            DevFlowLogger.warning("商店创建流程(聊天)", "无效的数字格式: " + input);
            player.sendSystemMessage(ShopTranslationHelper.colored("§c请输入有效的数字"));
        }
        return true;
    }

    private boolean handleCreationCurrency(ServerPlayer player, PendingShopCreation pending, String input) {
        DevFlowLogger.step("商店创建流程(聊天)", "处理货币选择", "玩家输入=", input);

        String userInput = input.trim();
        
        if (userInput.isEmpty()) {
            DevFlowLogger.warning("商店创建流程(聊天)", "货币输入为空");
            player.sendSystemMessage(ShopTranslationHelper.colored("§c输入不能为空，请重新输入货币名称或ID"));
            return true;
        }
        
        DevFlowLogger.step("商店创建流程(聊天)", "解析货币类型");
        Optional<me.tuanzi.economy.currency.WalletType> walletType = resolveWalletType(userInput);
        
        if (walletType.isEmpty()) {
            DevFlowLogger.warning("商店创建流程(聊天)", "未找到匹配的货币: " + input);
            player.sendSystemMessage(ShopTranslationHelper.colored("§c未找到匹配的货币: " + input));
            player.sendSystemMessage(ShopTranslationHelper.colored("§7提示: 请输入货币名称（如 '金币'）或ID（如 'gold_coin'）"));
            
            java.util.Collection<me.tuanzi.economy.currency.WalletType> allTypes = shopManager.getEconomyAPI().getAllWalletTypes();
            if (!allTypes.isEmpty()) {
                StringBuilder availableList = new StringBuilder("§a可用货币: ");
                int idx = 0;
                for (me.tuanzi.economy.currency.WalletType wt : allTypes) {
                    if (idx > 0) availableList.append(", ");
                    availableList.append(wt.displayName().getString()).append("(").append(wt.id()).append(")");
                    idx++;
                }
                player.sendSystemMessage(ShopTranslationHelper.colored(availableList.toString()));
            }
            return true;
        }
        
        me.tuanzi.economy.currency.WalletType selectedType = walletType.get();
        pending.currencyId = selectedType.id();
        pending.state = ShopCreationState.INPUTTING_NOTE;
        int timeout = 15;
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);
        pending.expireTime = expireTime;

        DevFlowLogger.param("商店创建流程(聊天)", "selectedCurrency", selectedType.displayName().getString() + "(" + selectedType.id() + ")");
        DevFlowLogger.param("商店创建流程(聊天)", "新状态", "INPUTTING_NOTE");

        player.sendSystemMessage(ShopTranslationHelper.colored("§a已选择货币: " + selectedType.displayName().getString() + " (" + selectedType.id() + ")"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b请输入备注(输入 {None} 表示无备注，15秒内不输入则取消创建):"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

        DevFlowLogger.status("商店创建流程(聊天)", "已向玩家发送备注输入提示");
        return true;
    }

    private Optional<me.tuanzi.economy.currency.WalletType> resolveWalletType(String input) {
        String normalizedInput = input.toLowerCase().trim();
        
        java.util.Collection<me.tuanzi.economy.currency.WalletType> allWalletTypes = shopManager.getEconomyAPI().getAllWalletTypes();
        
        for (me.tuanzi.economy.currency.WalletType wt : allWalletTypes) {
            if (wt.id().equalsIgnoreCase(normalizedInput)) {
                return Optional.of(wt);
            }
        }
        
        for (me.tuanzi.economy.currency.WalletType wt : allWalletTypes) {
            if (wt.displayName().getString().equalsIgnoreCase(input.trim())) {
                return Optional.of(wt);
            }
        }
        
        for (me.tuanzi.economy.currency.WalletType wt : allWalletTypes) {
            if (wt.displayName().getString().toLowerCase().contains(normalizedInput) || 
                normalizedInput.contains(wt.displayName().getString().toLowerCase())) {
                return Optional.of(wt);
            }
        }
        
        for (me.tuanzi.economy.currency.WalletType wt : allWalletTypes) {
            if (wt.id().toLowerCase().contains(normalizedInput) || 
                normalizedInput.contains(wt.id().toLowerCase())) {
                return Optional.of(wt);
            }
        }
        
        return Optional.empty();
    }

    private boolean handleCreationNote(ServerPlayer player, PendingShopCreation pending, String input) {
        DevFlowLogger.step("商店创建流程(聊天)", "处理备注输入", "玩家输入=", input);

        if (input.equalsIgnoreCase("{None}")) {
            DevFlowLogger.status("商店创建流程(聊天)", "玩家选择无备注");
            pending.note = "";
        } else if (input.isEmpty()) {
            DevFlowLogger.warning("商店创建流程(聊天)", "备注输入为空，取消创建");
            pendingShopCreations.remove(player.getUUID());
            player.sendSystemMessage(ShopTranslationHelper.colored("§c已取消创建商店"));
            DevFlowLogger.endFlow("商店创建流程(聊天)", false, "玩家未输入备注，超时取消");
            return true;
        } else {
            pending.note = input.trim();
            DevFlowLogger.param("商店创建流程(聊天)", "note", pending.note);
        }

        completeShopCreation(player, pending);
        return true;
    }

    private void completeShopCreation(ServerPlayer player, PendingShopCreation pending) {
        DevFlowLogger.step("商店创建流程(聊天)", "完成商店创建");

        ShopType shopType = pending.note.equals("出售") ? ShopType.SELL : ShopType.BUY;
        DevFlowLogger.param("商店创建流程(聊天)", "最终shopType", shopType);
        DevFlowLogger.param("商店创建流程(聊天)", "最终price", pending.price);
        DevFlowLogger.param("商店创建流程(聊天)", "最终currencyId", pending.currencyId);
        DevFlowLogger.param("商店创建流程(聊天)", "最终note", pending.note.equals("出售") || pending.note.equals("收购") ? "" : pending.note);

        DevFlowLogger.step("商店创建流程(聊天)", "调用ShopManager.createShop()");
        ShopInstance shop = shopManager.createShop(
                player.getUUID(),
                pending.containerPos,
                pending.signPos,
                shopType,
                pending.item.copy(),
                pending.price,
                pending.currencyId,
                pending.note.equals("出售") || pending.note.equals("收购") ? "" : pending.note
        );

        pendingShopCreations.remove(player.getUUID());

        if (shop != null) {
            DevFlowLogger.status("商店创建流程(聊天)", "商店对象创建成功");

            MinecraftServer server = player.level().getServer();
            if (server != null) {
                ShopModule instance = ShopModule.getInstance(server);
                if (instance != null && instance.getDisplayManager() != null) {
                    if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                        DevFlowLogger.step("商店创建流程(聊天)", "创建悬浮物品显示");
                        instance.getDisplayManager().createDisplayForShop(shop, level);
                        DevFlowLogger.status("商店创建流程(聊天)", "悬浮物品显示已创建");
                    }
                }
            }

            if (player.level() instanceof net.minecraft.server.level.ServerLevel level) {
                SignUpdateHelper.updateSignForShop(shop, level);
            }

            player.sendSystemMessage(ShopTranslationHelper.colored("§a========================================"));
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.creation.success"));
            player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));

            DevFlowLogger.endFlow("商店创建流程(聊天)", true, 
                    "商店创建完成 - ID: " + shop.getShopId().toString().substring(0, 8) + 
                    ", 类型: " + shopType + 
                    ", 价格: " + pending.price +
                    ", 物品: " + pending.item.getDisplayName().getString());
        } else {
            DevFlowLogger.error("商店创建流程(聊天)", "商店对象创建失败（可能货币ID无效）");
            player.sendSystemMessage(ShopTranslationHelper.colored("§c商店创建失败，请检查货币ID是否正确"));
            DevFlowLogger.endFlow("商店创建流程(聊天)", false, "商店创建失败");
        }
    }
}

