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
    private final Map<UUID, PendingDynamicSetup> pendingDynamicSetups = new ConcurrentHashMap<>();

    public ChatInputHandler(ShopManager shopManager, TransactionLogger transactionLogger) {
        this.shopManager = shopManager;
        this.transactionLogger = transactionLogger;
    }

    public void startTransactionInput(ServerPlayer player, ShopInstance shop) {
        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        PendingTransaction pending = new PendingTransaction(shop.getShopId(), expireTime);
        pendingTransactions.put(player.getUUID(), pending);

        player.sendSystemMessage(ShopTranslationHelper.header(player, "transaction.header"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.prompt", timeout));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "transaction.confirm_hint_cancel")));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "transaction.header"));
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

        player.sendSystemMessage(ShopTranslationHelper.header(player, "item.change.header"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.prompt", newItem.getDisplayName().getString()));

        Component confirmBtn = Component.literal("[" + ShopTranslationHelper.getRawTranslation(player, "item.change.confirm_btn") + "]")
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/shopconfirm yes"))
                        .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "item.change.click_confirm"))));

        Component cancelBtn = Component.literal("[" + ShopTranslationHelper.getRawTranslation(player, "item.change.cancel_btn") + "]")
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/shopconfirm no"))
                        .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "item.change.click_cancel"))));

        player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.literal(" ")).append(cancelBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "transaction.confirm_hint")));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "item.change.header"));

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

        if (pendingDynamicSetups.containsKey(playerId)) {
            return handleDynamicSetupInput(player, message.trim());
        }

        if (pendingTransactions.containsKey(playerId)) {
            return handleTransactionInput(player, message.trim());
        }

        return false;
    }

    public void startDynamicSetup(ServerPlayer player, ShopInstance shop) {
        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        PendingDynamicSetup pending = new PendingDynamicSetup(shop.getShopId(), expireTime);
        pendingDynamicSetups.put(player.getUUID(), pending);

        player.sendSystemMessage(ShopTranslationHelper.header(player, "common.dynamic_pricing"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "admin.shop.dynamic.setup_header"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.base_price_fmt") + shop.getBasePrice()));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.min_price_prompt")));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "common.dynamic_pricing"));
    }

    private boolean handleDynamicSetupInput(ServerPlayer player, String input) {
        UUID playerId = player.getUUID();
        PendingDynamicSetup pending = pendingDynamicSetups.get(playerId);

        if (pending == null) return false;

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingDynamicSetups.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
            return true;
        }

        if (input.equalsIgnoreCase("cancel") || input.equalsIgnoreCase("取消")) {
            pendingDynamicSetups.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "admin.shop.dynamic.cancelled"));
            return true;
        }

        try {
            double value = Double.parseDouble(input);
            if (value <= 0) {
                player.sendSystemMessage(ShopTranslationHelper.translatable(player, "admin.shop.dynamic.invalid_value"));
                return true;
            }

            switch (pending.state) {
                case INPUTTING_MIN_PRICE:
                    pending.minPrice = value;
                    pending.state = ShopDynamicSetupState.INPUTTING_MAX_PRICE;
                    player.sendSystemMessage(ShopTranslationHelper.colored("§a" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.min_price_set") + value));
                    player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.max_price_prompt")));
                    break;
                case INPUTTING_MAX_PRICE:
                    if (value <= pending.minPrice) {
                        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "admin.shop.dynamic.max_less_than_min"));
                        return true;
                    }
                    pending.maxPrice = value;
                    pending.state = ShopDynamicSetupState.INPUTTING_HALF_LIFE;
                    player.sendSystemMessage(ShopTranslationHelper.colored("§a" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.max_price_set") + value));
                    player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.halflife_prompt")));
                    break;
                case INPUTTING_HALF_LIFE:
                    pendingDynamicSetups.remove(playerId);
                    completeDynamicSetup(player, pending, value);
                    break;
            }
            // 每次输入成功后重置超时
            pending.expireTime = System.currentTimeMillis() + (ShopModule.getConfig().getInputTimeoutSeconds() * 1000L);
        } catch (NumberFormatException e) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c" + ShopTranslationHelper.getRawTranslation(player, "common.invalid_number")));
        }

        return true;
    }

    private void completeDynamicSetup(ServerPlayer player, PendingDynamicSetup pending, double halfLife) {
        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.not_found"));
            return;
        }

        ShopInstance shop = shopOpt.get();
        shop.setMinPrice(pending.minPrice);
        shop.setMaxPrice(pending.maxPrice);
        shop.setHalfLifeConstant(halfLife);
        shop.setDynamicPricing(true);
        
        // 实时更新当前价格
        double newPrice = me.tuanzi.shop.pricing.DynamicPricing.calculatePrice(shop);
        shop.setCurrentPrice(newPrice);
        shopManager.markDirty();

        if (player.level() instanceof ServerLevel level) {
            SignUpdateHelper.updateSignForShop(shop, level);
        }

        player.sendSystemMessage(ShopTranslationHelper.header(player, "common.dynamic_pricing"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "admin.shop.dynamic.setup_success"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.info.min_price") + ": §e" + pending.minPrice));
        player.sendSystemMessage(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.info.max_price") + ": §e" + pending.maxPrice));
        player.sendSystemMessage(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.info.half_life") + ": §e" + halfLife));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic.current_unit_price") + String.format("%.2f", newPrice)));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "common.dynamic_pricing"));
    }

    private boolean handleTransactionInput(ServerPlayer player, String input) {
        UUID playerId = player.getUUID();
        PendingTransaction pending = pendingTransactions.get(playerId);

        if (pending == null) {
            return false;
        }

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
            return true;
        }

        String lowerInput = input.toLowerCase();
        if (lowerInput.equals("cancel") || lowerInput.equals("取消")) {
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.cancelled"));
            return true;
        }

        // 如果已经在等待确认 (动态定价流程)
        if (pending.quantity > 0) {
            boolean confirmed = lowerInput.equals("yes") || lowerInput.equals("是") || lowerInput.equals("confirm");
            boolean cancelled = lowerInput.equals("no") || lowerInput.equals("否");

            if (confirmed) {
                pendingTransactions.remove(playerId);
                Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
                if (shopOpt.isPresent()) {
                    executeTransaction(player, shopOpt.get(), pending.quantity);
                }
                return true;
            } else if (cancelled) {
                pendingTransactions.remove(playerId);
                player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.cancelled"));
                return true;
            }
            // 输入无效，取消并提示
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.invalid"));
            return true;
        }

        // 处理初始数量输入
        int quantity;
        try {
            quantity = Integer.parseInt(input);
            if (quantity <= 0) {
                pendingTransactions.remove(playerId);
                player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.invalid"));
                return true;
            }
            int maxQty = ShopModule.getConfig().getMaxTransactionQuantity();
            if (quantity > maxQty) {
                pendingTransactions.remove(playerId);
                player.sendSystemMessage(ShopTranslationHelper.colored("§c" + ShopTranslationHelper.getRawTranslation(player, "transaction.too_many_items") + ": " + maxQty));
                return true;
            }
        } catch (NumberFormatException e) {
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.invalid"));
            return true;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty() || !shopOpt.get().isValid()) {
            pendingTransactions.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.not_found"));
            return true;
        }

        ShopInstance shop = shopOpt.get();

        // 动态定价需要二次确认
        if (shop.isDynamicPricing()) {
            startDynamicConfirmation(player, shop, quantity);
            return true;
        }

        pendingTransactions.remove(playerId);
        executeTransaction(player, shop, quantity);
        return true;
    }

    public void startDynamicConfirmation(ServerPlayer player, ShopInstance shop, int quantity) {
        boolean isBuy = shop.getShopType() == ShopType.SELL;
        double totalPrice = me.tuanzi.shop.pricing.DynamicPricing.calculateBulkPrice(shop, quantity, isBuy);
        String currencyName = shopManager.getCurrencyDisplayName(shop.getWalletTypeId());

        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        PendingTransaction pending = new PendingTransaction(shop.getShopId(), expireTime);
        pending.quantity = quantity;
        pendingTransactions.put(player.getUUID(), pending);

        player.sendSystemMessage(ShopTranslationHelper.header(player, "common.dynamic_pricing"));
        player.sendSystemMessage(ShopTranslationHelper.colored("§b" + ShopTranslationHelper.getRawTranslation(player, "transaction.dynamic_confirm_header") + ":"));
        
        // 物品悬浮展示 (使用 26.1 API)
        ItemStack stack = shop.getTradeItem();
        net.minecraft.world.item.ItemStackTemplate template = net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(stack);
        Component itemComp = stack.getDisplayName().copy().withStyle(s -> 
            s.withHoverEvent(new net.minecraft.network.chat.HoverEvent.ShowItem(template)));
        
        player.sendSystemMessage(Component.empty()
                .append(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "common.item") + ": "))
                .append(itemComp));
        
        player.sendSystemMessage(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "transaction.quantity") + ": §e" + quantity));
        player.sendSystemMessage(ShopTranslationHelper.colored("§f" + ShopTranslationHelper.getRawTranslation(player, "transaction.total_price") + ": §e" + String.format("%.2f", totalPrice) + " " + currencyName));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7(" + ShopTranslationHelper.getRawTranslation(player, "admin.shop.dynamic_desc") + ")"));
        
        Component confirmBtn = Component.literal("[" + ShopTranslationHelper.getRawTranslation(player, "item.change.confirm_btn") + "]")
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/shopconfirm yes"))
                        .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "transaction.click_to_confirm"))));

        Component cancelBtn = Component.literal("[" + ShopTranslationHelper.getRawTranslation(player, "item.change.cancel_btn") + "]")
                .setStyle(Style.EMPTY
                        .withClickEvent(new ClickEvent.RunCommand("/shopconfirm no"))
                        .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "transaction.click_to_cancel"))));

        player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.literal(" ")).append(cancelBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "transaction.confirm_hint")));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "common.dynamic_pricing"));
    }

    private boolean handleItemChangeInput(ServerPlayer player, String input) {
        UUID playerId = player.getUUID();
        PendingItemChange pending = pendingItemChanges.get(playerId);

        if (pending == null) return false;

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingItemChanges.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.timeout"));
            return true;
        }

        boolean confirmed = input.equals("yes") || input.equals("是") || input.equals("confirm");
        boolean cancelled = input.equals("no") || input.equals("否") || input.equals("cancel");

        if (!confirmed && !cancelled) return false;

        pendingItemChanges.remove(playerId);

        if (cancelled) {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.cancelled"));
            return true;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty() || !shopOpt.get().isValid()) {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.not_found"));
            return true;
        }

        ShopInstance shop = shopOpt.get();
        shop.setTradeItem(pending.newItem.copy());
        shopManager.markDirty();

        MinecraftServer server = player.level().getServer();
        if (server != null) {
            ShopModule instance = ShopModule.getInstance(server);
            if (instance != null && instance.getDisplayManager() != null) {
                if (player.level() instanceof ServerLevel serverLevel) {
                    instance.getDisplayManager().updateDisplayItem(shop.getShopId(), pending.newItem, serverLevel);
                    SignUpdateHelper.updateSignForShop(shop, serverLevel);
                }
            }
        }

        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.success", pending.newItem.getDisplayName().getString()));
        return true;
    }

    public boolean handleConfirmCommand(ServerPlayer player, boolean confirmed) {
        UUID playerId = player.getUUID();
        PendingItemChange pending = pendingItemChanges.get(playerId);

        if (pending == null) return false;

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingItemChanges.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.timeout"));
            return true;
        }

        pendingItemChanges.remove(playerId);

        if (!confirmed) {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.cancelled"));
            return true;
        }

        Optional<ShopInstance> shopOpt = shopManager.getShopById(pending.shopId);
        if (shopOpt.isEmpty() || !shopOpt.get().isValid()) {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.not_found"));
            return true;
        }

        ShopInstance shop = shopOpt.get();
        shop.setTradeItem(pending.newItem.copy());
        shopManager.markDirty();

        MinecraftServer serverCmd = player.level().getServer();
        if (serverCmd != null) {
            ShopModule instanceCmd = ShopModule.getInstance(serverCmd);
            if (instanceCmd != null && instanceCmd.getDisplayManager() != null) {
                if (player.level() instanceof ServerLevel serverLevelCmd) {
                    instanceCmd.getDisplayManager().updateDisplayItem(shop.getShopId(), pending.newItem, serverLevelCmd);
                    SignUpdateHelper.updateSignForShop(shop, serverLevelCmd);
                }
            }
        }

        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.success", pending.newItem.getDisplayName().getString()));
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

        if (pendingTransactions.containsKey(playerId)) {
            return handleTransactionInput(player, action);
        }

        return false;
    }

    private boolean handleShopCreationConfirm(ServerPlayer player, String action) {
        UUID playerId = player.getUUID();
        PendingShopCreation pending = pendingShopCreations.get(playerId);

        if (pending == null) return false;

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingShopCreations.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
            return true;
        }

        switch (pending.state) {
            case WAITING_CONFIRM:
                boolean confirmed = action.equals("yes") || action.equals("是");
                boolean cancelled = action.equals("no") || action.equals("否");
                
                if (!confirmed && !cancelled) return false;
                
                if (cancelled) {
                    pendingShopCreations.remove(playerId);
                    player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.cancelled"));
                    return true;
                }
                
                return handleCreationConfirm(player, pending, "yes");
                
            case SELECTING_TYPE:
                return handleCreationType(player, pending, action);
                
            default:
                return false;
        }
    }

    public void cleanupExpired() {
        long currentTime = System.currentTimeMillis();
        MinecraftServer server = shopManager.getServer();
        if (server == null) return;

        pendingTransactions.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expireTime) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
                return true;
            }
            return false;
        });

        pendingItemChanges.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expireTime) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) player.sendSystemMessage(ShopTranslationHelper.translatable(player, "item.change.timeout"));
                return true;
            }
            return false;
        });

        pendingShopCreations.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expireTime) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
                return true;
            }
            return false;
        });

        pendingDynamicSetups.entrySet().removeIf(entry -> {
            if (currentTime > entry.getValue().expireTime) {
                ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
                if (player != null) player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
                return true;
            }
            return false;
        });
    }

    public void cleanupForShop(UUID shopId) {
        pendingTransactions.entrySet().removeIf(entry -> entry.getValue().shopId.equals(shopId));
        pendingItemChanges.entrySet().removeIf(entry -> entry.getValue().shopId.equals(shopId));
        pendingDynamicSetups.entrySet().removeIf(entry -> entry.getValue().shopId.equals(shopId));
    }

    private static class PendingTransaction {
        final UUID shopId;
        final long expireTime;
        int quantity = -1;

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

    private enum ShopDynamicSetupState {
        INPUTTING_MIN_PRICE,
        INPUTTING_MAX_PRICE,
        INPUTTING_HALF_LIFE
    }

    private static class PendingDynamicSetup {
        final UUID shopId;
        ShopDynamicSetupState state;
        double minPrice;
        double maxPrice;
        long expireTime;

        PendingDynamicSetup(UUID shopId, long expireTime) {
            this.shopId = shopId;
            this.state = ShopDynamicSetupState.INPUTTING_MIN_PRICE;
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
        ShopType shopType;
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

    public boolean hasPendingTransaction(UUID playerId) {
        PendingTransaction pending = pendingTransactions.get(playerId);
        return pending != null && System.currentTimeMillis() <= pending.expireTime;
    }

    public boolean cancelPendingTransaction(UUID playerId) {
        return pendingTransactions.remove(playerId) != null;
    }

    public boolean hasPendingShopCreation(UUID playerId) {
        PendingShopCreation pending = pendingShopCreations.get(playerId);
        return pending != null && System.currentTimeMillis() <= pending.expireTime;
    }

    public void startShopCreation(ServerPlayer player, BlockPos signPos, ItemStack item, BlockPos containerPos) {
        int timeout = ShopModule.getConfig().getInputTimeoutSeconds();
        long expireTime = System.currentTimeMillis() + (timeout * 1000L);

        PendingShopCreation pending = new PendingShopCreation(signPos, containerPos, item, expireTime);
        pendingShopCreations.put(player.getUUID(), pending);

        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.prompt", item.getDisplayName().getString()));
        
        Component confirmBtn = Component.literal("[" + ShopTranslationHelper.getRawTranslation(player, "item.change.confirm_btn") + "]")
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm yes"))
                .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "transaction.click_to_confirm"))));
        
        Component cancelBtn = Component.literal("[" + ShopTranslationHelper.getRawTranslation(player, "item.change.cancel_btn") + "]")
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm no"))
                .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "transaction.click_to_cancel"))));
        
        player.sendSystemMessage(Component.empty().append(confirmBtn).append(Component.literal(" ")).append(cancelBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "transaction.confirm_hint")));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
    }

    private boolean handleShopCreationInput(ServerPlayer player, String input) {
        UUID playerId = player.getUUID();
        PendingShopCreation pending = pendingShopCreations.get(playerId);

        if (pending == null) return false;

        if (System.currentTimeMillis() > pending.expireTime) {
            pendingShopCreations.remove(playerId);
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "transaction.input.timeout"));
            return true;
        }

        switch (pending.state) {
            case WAITING_CONFIRM: return handleCreationConfirm(player, pending, input);
            case SELECTING_TYPE: return handleCreationType(player, pending, input);
            case INPUTTING_PRICE: return handleCreationPrice(player, pending, input);
            case INPUTTING_CURRENCY: return handleCreationCurrency(player, pending, input);
            case INPUTTING_NOTE: return handleCreationNote(player, pending, input);
            default: return false;
        }
    }

    private boolean handleCreationConfirm(ServerPlayer player, PendingShopCreation pending, String input) {
        boolean confirmed = input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("是") || input.equalsIgnoreCase("confirm");
        boolean cancelled = input.equalsIgnoreCase("no") || input.equalsIgnoreCase("否") || input.equalsIgnoreCase("cancel");

        if (!confirmed && !cancelled) return false;

        if (cancelled) {
            pendingShopCreations.remove(player.getUUID());
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.cancelled"));
            return true;
        }

        pending.state = ShopCreationState.SELECTING_TYPE;
        pending.expireTime = System.currentTimeMillis() + (15 * 1000L);

        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.type_prompt"));
        
        Component sellBtn = Component.literal("§a[" + ShopTranslationHelper.getRawTranslation(player, "shop.type.sell_btn") + "]")
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm sell"))
                .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "shop.creation.sell_btn_hover"))));
        
        Component buyBtn = Component.literal("§a[" + ShopTranslationHelper.getRawTranslation(player, "shop.type.buy_btn") + "]")
                .setStyle(Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand("/shopconfirm buy"))
                .withHoverEvent(new HoverEvent.ShowText(ShopTranslationHelper.translatable(player, "shop.creation.buy_btn_hover"))));
        
        player.sendSystemMessage(Component.empty().append(sellBtn).append(Component.literal(" ")).append(buyBtn));
        player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "shop.creation.type_hint")));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        return true;
    }

    private boolean handleCreationType(ServerPlayer player, PendingShopCreation pending, String input) {
        String lowerInput = input.toLowerCase();
        boolean isSell = lowerInput.equals("sell") || lowerInput.equals("出售");
        boolean isBuy = lowerInput.equals("buy") || lowerInput.equals("收购");

        if (!isSell && !isBuy) {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.invalid_type"));
            return true;
        }

        pending.state = ShopCreationState.INPUTTING_PRICE;
        pending.expireTime = System.currentTimeMillis() + (15 * 1000L);
        pending.shopType = isSell ? ShopType.SELL : ShopType.BUY;

        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.price_prompt"));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        return true;
    }

    private boolean handleCreationPrice(ServerPlayer player, PendingShopCreation pending, String input) {
        try {
            double price = Double.parseDouble(input);
            if (price <= 0) {
                player.sendSystemMessage(ShopTranslationHelper.translatable(player, "admin.shop.dynamic.invalid_value"));
                return true;
            }

            pending.price = price;
            pending.state = ShopCreationState.INPUTTING_CURRENCY;
            pending.expireTime = System.currentTimeMillis() + (15 * 1000L);

            player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.currency_prompt"));
            
            java.util.Collection<me.tuanzi.economy.currency.WalletType> walletTypes = shopManager.getEconomyAPI().getAllWalletTypes();
            if (walletTypes.isEmpty()) {
                player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.no_currencies"));
                return true;
            }
            
            StringBuilder currencyList = new StringBuilder("§a" + ShopTranslationHelper.getRawTranslation(player, "shop.creation.currency_list_header"));
            int index = 0;
            for (me.tuanzi.economy.currency.WalletType wt : walletTypes) {
                if (index > 0) currencyList.append("§7、");
                currencyList.append(wt.displayName().getString()).append("(§e").append(wt.id()).append("§a)");
                index++;
            }
            
            player.sendSystemMessage(ShopTranslationHelper.colored(currencyList.toString()));
            player.sendSystemMessage(ShopTranslationHelper.colored("§7" + ShopTranslationHelper.getRawTranslation(player, "shop.creation.currency_resolve_hint")));
            player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        } catch (NumberFormatException e) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c" + ShopTranslationHelper.getRawTranslation(player, "common.invalid_number")));
        }
        return true;
    }

    private boolean handleCreationCurrency(ServerPlayer player, PendingShopCreation pending, String input) {
        String userInput = input.trim();
        if (userInput.isEmpty()) return true;
        
        Optional<me.tuanzi.economy.currency.WalletType> walletType = resolveWalletType(userInput);
        if (walletType.isEmpty()) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c" + ShopTranslationHelper.getRawTranslation(player, "economy.pay.wallet_not_found", input)));
            return true;
        }
        
        me.tuanzi.economy.currency.WalletType selectedType = walletType.get();
        pending.currencyId = selectedType.id();
        pending.state = ShopCreationState.INPUTTING_NOTE;
        pending.expireTime = System.currentTimeMillis() + (15 * 1000L);

        player.sendSystemMessage(ShopTranslationHelper.colored("§a" + ShopTranslationHelper.getRawTranslation(player, "shop.creation.currency_selected") + selectedType.displayName().getString() + " (" + selectedType.id() + ")"));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.note_prompt", 15));
        player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        return true;
    }

    private Optional<me.tuanzi.economy.currency.WalletType> resolveWalletType(String input) {
        String normalizedInput = input.toLowerCase().trim();
        java.util.Collection<me.tuanzi.economy.currency.WalletType> allWalletTypes = shopManager.getEconomyAPI().getAllWalletTypes();
        for (me.tuanzi.economy.currency.WalletType wt : allWalletTypes) {
            if (wt.id().equalsIgnoreCase(normalizedInput) || wt.displayName().getString().equalsIgnoreCase(input.trim())) return Optional.of(wt);
        }
        return Optional.empty();
    }

    private boolean handleCreationNote(ServerPlayer player, PendingShopCreation pending, String input) {
        if (input.equalsIgnoreCase("{None}")) pending.note = "";
        else if (input.isEmpty()) {
            pendingShopCreations.remove(player.getUUID());
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.cancelled"));
            return true;
        } else pending.note = input.trim();

        completeShopCreation(player, pending);
        return true;
    }

    private void completeShopCreation(ServerPlayer player, PendingShopCreation pending) {
        ShopType shopType = pending.shopType;
        // 如果备注是 "出售" 或 "收购"，说明是 handleCreationType 设置的默认值，实际存储时设为空
        String finalNote = (pending.note.equals("出售") || pending.note.equals("收购")) ? "" : pending.note;
        
        ShopInstance shop = shopManager.createShop(player.getUUID(), pending.containerPos, pending.signPos, shopType, pending.item.copy(), pending.price, pending.currencyId, finalNote);

        pendingShopCreations.remove(player.getUUID());
        if (shop != null) {
            MinecraftServer server = player.level().getServer();
            if (server != null) {
                ShopModule instance = ShopModule.getInstance(server);
                if (instance != null && instance.getDisplayManager() != null && player.level() instanceof ServerLevel level) {
                    instance.getDisplayManager().createDisplayForShop(shop, level);
                    SignUpdateHelper.updateSignForShop(shop, level);
                }
            }
            player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.creation.success"));
            player.sendSystemMessage(ShopTranslationHelper.header(player, "shop.creation.header"));
        } else {
            player.sendSystemMessage(ShopTranslationHelper.translatable(player, "shop.created.failed"));
        }
    }

    private void executeTransaction(ServerPlayer player, ShopInstance shop, int quantity) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return;
        BlockInteractionHandler handler = ShopModule.getInstance(server).getInteractionHandler();
        if (handler != null) handler.executeTransaction(player, shop.getShopId(), (shop.getShopType() == ShopType.SELL ? "buy" : "sell"), quantity);
    }
}
