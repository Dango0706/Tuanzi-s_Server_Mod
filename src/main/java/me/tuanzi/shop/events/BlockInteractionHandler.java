package me.tuanzi.shop.events;

import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.shop.ShopModule;
import me.tuanzi.shop.config.ShopConfig;
import me.tuanzi.shop.logging.TransactionLogger;
import me.tuanzi.shop.pricing.DynamicPricing;
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
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

public class BlockInteractionHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShopModule.MOD_ID);
    private final ShopManager shopManager;
    private final TransactionLogger transactionLogger;

    public BlockInteractionHandler(ShopManager shopManager, TransactionLogger transactionLogger) {
        this.shopManager = shopManager;
        this.transactionLogger = transactionLogger;
    }

    public boolean handleBlockInteraction(ServerPlayer player, BlockPos pos, boolean isLeftClick, boolean isShiftDown) {
        Optional<ShopInstance> shopOpt = shopManager.getShopByPos(pos);
        if (shopOpt.isEmpty()) {
            return false;
        }

        ShopInstance shop = shopOpt.get();
        
        if (!shop.isValid()) {
            LOGGER.warn("尝试与已删除的商店交互 - ShopId: {}, 玩家: {}", shop.getShopId(), player.getName().getString());
            player.sendSystemMessage(ShopTranslationHelper.colored("§c此商店已不存在"));
            return true;
        }
        
        ItemStack heldItem = player.getMainHandItem();

        if (shop.isOwner(player.getUUID()) && !heldItem.isEmpty()) {
            return handleItemChangeRequest(player, shop, heldItem);
        }

        if (shop.isOwner(player.getUUID())) {
            return false;
        }

        ShopConfig config = ShopModule.getConfig();
        if (config != null && config.isEnableSimplifiedTransaction()) {
            return handleSimplifiedTransaction(player, shop, isShiftDown);
        }

        sendShopInteractionMessage(player, shop, isShiftDown);
        return true;
    }

    private boolean handleItemChangeRequest(ServerPlayer player, ShopInstance shop, ItemStack newItem) {
        if (ItemUtils.itemsMatch(shop.getTradeItem(), newItem)) {
            player.sendSystemMessage(ShopTranslationHelper.colored("§c商店已经在出售此物品！"));
            return true;
        }

        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler == null) {
            return false;
        }

        chatHandler.startItemChangeConfirmation(player, shop, newItem);
        return true;
    }

    private boolean handleSimplifiedTransaction(ServerPlayer player, ShopInstance shop, boolean isShiftDown) {
        ChatInputHandler chatHandler = ShopModule.getInstance(player.level().getServer()).getChatInputHandler();
        if (chatHandler == null) {
            return false;
        }

        if (chatHandler.hasPendingTransaction(player.getUUID())) {
            chatHandler.cancelPendingTransaction(player.getUUID());
            player.sendSystemMessage(ShopTranslationHelper.colored("§7已取消旧交易，切换到新商店"));
        }

        if (isShiftDown) {
            int quantity = 64;
            String type = shop.getShopType() == ShopType.SELL ? "buy" : "sell";
            return executeTransaction(player, shop.getShopId(), type, quantity);
        }

        sendShopInfo(player, shop);
        chatHandler.startTransactionInput(player, shop);
        return true;
    }

    private void sendShopInfo(ServerPlayer player, ShopInstance shop) {
        WalletType walletType = shopManager.getWalletType(shop.getWalletTypeId()).orElse(null);
        String currencyName = walletType != null ? walletType.displayName().getString() : "Unknown";
        double price = DynamicPricing.calculatePrice(shop);
        int stock = getShopStock(shop);
        int capacity = getChestAvailableSpace(shop);

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.title"));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.item", 
                shop.getTradeItem().getDisplayName().getString()));
        player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.price", 
                price, currencyName));

        if (!shop.isAdminShop() && !shop.isInfinite()) {
            if (shop.getShopType() == ShopType.SELL) {
                player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.stock", stock));
            } else {
                player.sendSystemMessage(ShopTranslationHelper.translatable("admin.shop.info.capacity", capacity));
            }
        }

        player.sendSystemMessage(ShopTranslationHelper.colored("§e========================================"));
    }

    private void sendShopInteractionMessage(ServerPlayer player, ShopInstance shop, boolean isShiftDown) {
        WalletType walletType = shopManager.getWalletType(shop.getWalletTypeId()).orElse(null);
        String currencyName = walletType != null ? walletType.displayName().getString() : "Unknown";
        double price = DynamicPricing.calculatePrice(shop);
        int quantity = isShiftDown ? 64 : 1;
        double totalPrice = price * quantity;

        Component shopTypeComponent = ShopTranslationHelper.shopTypeDisplayName(shop.getShopType() == ShopType.SELL);
        Component itemName = shop.getTradeItem().getDisplayName();
        int stock = getShopStock(shop);
        int capacity = getChestAvailableSpace(shop);

        player.sendSystemMessage(Component.empty()
                .append(ShopTranslationHelper.colored("§e=== "))
                .append(shopTypeComponent)
                .append(ShopTranslationHelper.colored(" §e===")));

        player.sendSystemMessage(Component.empty()
                .append(ShopTranslationHelper.colored("§b物品: §f"))
                .append(itemName));

        player.sendSystemMessage(Component.empty()
                .append(ShopTranslationHelper.colored("§b单价: §e"))
                .append(ShopTranslationHelper.literal(String.format("%.2f %s", price, currencyName))));

        if (!shop.isAdminShop() && !shop.isInfinite()) {
            if (shop.getShopType() == ShopType.SELL) {
                player.sendSystemMessage(Component.empty()
                        .append(ShopTranslationHelper.colored("§b库存: §f"))
                        .append(ShopTranslationHelper.literal(String.valueOf(stock))));
            } else {
                player.sendSystemMessage(Component.empty()
                        .append(ShopTranslationHelper.colored("§b收购物品容量: §f"))
                        .append(ShopTranslationHelper.literal(String.valueOf(capacity))));
            }
        }

        Component actionButton = createActionButton(shop, player, quantity, totalPrice, currencyName);
        player.sendSystemMessage(actionButton);

        if (!isShiftDown) {
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.shift_hint"));
        }
    }

    private Component createActionButton(ShopInstance shop, ServerPlayer player, int quantity, 
                                          double totalPrice, String currencyName) {
        boolean isSellShop = shop.getShopType() == ShopType.SELL;
        String buttonText = isSellShop ? 
                ShopTranslationHelper.getRawTranslation("transaction.click_to_buy") :
                ShopTranslationHelper.getRawTranslation("transaction.click_to_sell");

        String command = String.format("/shop %s %d", 
                isSellShop ? "buy" : "sell", quantity);

        Style style = Style.EMPTY
                .withClickEvent(new ClickEvent.RunCommand(command))
                .withHoverEvent(new HoverEvent.ShowText(
                        Component.literal(String.format("§e总价: %.2f %s", totalPrice, currencyName))));

        return Component.literal(buttonText).setStyle(style);
    }

    public boolean executeTransaction(ServerPlayer player, UUID shopId, String transactionType, int quantity) {
        DevFlowLogger.startFlow("交易执行流程");
        DevFlowLogger.param("交易执行流程", "player", player.getName().getString());
        DevFlowLogger.param("交易执行流程", "shopId", shopId);
        DevFlowLogger.param("交易执行流程", "transactionType", transactionType);
        DevFlowLogger.param("交易执行流程", "quantity", quantity);

        DevFlowLogger.step("交易执行流程", "查找商店");
        Optional<ShopInstance> shopOpt = shopManager.getShopById(shopId);
        if (shopOpt.isEmpty()) {
            DevFlowLogger.error("交易执行流程", "商店不存在: " + shopId);
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.not_found"));
            DevFlowLogger.endFlow("交易执行流程", false, "商店不存在");
            return false;
        }

        ShopInstance shop = shopOpt.get();
        
        if (!shop.isValid()) {
            DevFlowLogger.error("交易执行流程", "商店已被删除: " + shopId);
            LOGGER.warn("尝试与已删除的商店交易 - ShopId: {}, 玩家: {}", shopId, player.getName().getString());
            player.sendSystemMessage(ShopTranslationHelper.colored("§c此商店已不存在"));
            DevFlowLogger.endFlow("交易执行流程", false, "商店已删除");
            return false;
        }
        
        DevFlowLogger.status("交易执行流程", "商店验证通过");
        DevFlowLogger.param("交易执行流程", "shopType", shop.getShopType());
        DevFlowLogger.param("交易执行流程", "tradeItem", shop.getTradeItem().getDisplayName().getString());

        DevFlowLogger.step("交易执行流程", "获取钱包类型和计算价格");
        WalletType walletType = shopManager.getWalletType(shop.getWalletTypeId()).orElse(null);
        if (walletType == null) {
            DevFlowLogger.error("交易执行流程", "钱包类型无效: " + shop.getWalletTypeId());
            player.sendSystemMessage(ShopTranslationHelper.translatable("shop.not_found"));
            DevFlowLogger.endFlow("交易执行流程", false, "钱包类型无效");
            return false;
        }

        String currencyName = walletType.displayName().getString();
        double price = DynamicPricing.calculatePrice(shop);
        double totalPrice = price * quantity;

        DevFlowLogger.param("交易执行流程", "currencyName", currencyName);
        DevFlowLogger.param("交易执行流程", "unitPrice", price);
        DevFlowLogger.param("交易执行流程", "totalPrice", totalPrice);

        if (transactionType.equals("buy")) {
            boolean result = executeBuyTransaction(player, shop, quantity, totalPrice, currencyName, walletType);
            DevFlowLogger.endFlow("交易执行流程", result ? true : false, 
                    result ? "购买成功" : "购买失败");
            return result;
        } else {
            boolean result = executeSellTransaction(player, shop, quantity, totalPrice, currencyName, walletType);
            DevFlowLogger.endFlow("交易执行流程", result ? true : false, 
                    result ? "出售成功" : "出售失败");
            return result;
        }
    }

    private boolean executeBuyTransaction(ServerPlayer player, ShopInstance shop, int quantity,
                                          double totalPrice, String currencyName, WalletType walletType) {
        DevFlowLogger.startFlow("购买交易子流程");
        DevFlowLogger.param("购买交易子流程", "player", player.getName().getString());
        DevFlowLogger.param("购买交易子流程", "quantity", quantity);
        DevFlowLogger.param("购买交易子流程", "totalPrice", totalPrice);

        DevFlowLogger.step("购买交易子流程", "检查库存");
        if (!shop.isAdminShop() && !shop.isInfinite()) {
            int stock = getShopStock(shop);
            DevFlowLogger.param("购买交易子流程", "当前库存", stock);
            
            if (stock < quantity) {
                DevFlowLogger.warning("购买交易子流程", "库存不足 - 需要: " + quantity + ", 实际: " + stock);
                player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.insufficient_stock"));
                DevFlowLogger.endFlow("购买交易子流程", false, "库存不足");
                return false;
            }
            DevFlowLogger.status("购买交易子流程", "库存检查通过");
        } else {
            DevFlowLogger.status("购买交易子流程", "跳过库存检查（管理员商店或无限制模式）");
        }

        DevFlowLogger.step("购买交易子流程", "检查玩家余额");
        double playerBalance = shopManager.getPlayerBalance(player.getUUID(), shop.getWalletTypeId());
        DevFlowLogger.param("购买交易子流程", "玩家余额", playerBalance);

        if (!shopManager.hasEnoughBalance(player.getUUID(), shop.getWalletTypeId(), totalPrice)) {
            DevFlowLogger.warning("购买交易子流程", "余额不足 - 需要: " + totalPrice + ", 实际: " + playerBalance);
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.insufficient_balance", 
                    totalPrice, currencyName));
            DevFlowLogger.endFlow("购买交易子流程", false, "余额不足");
            return false;
        }
        DevFlowLogger.status("购买交易子流程", "余额检查通过");

        DevFlowLogger.step("购买交易子流程", "执行资金转账（从玩家到商店所有者）");
        if (!shop.isAdminShop()) {
            shopManager.transferMoney(player.getUUID(), shop.getOwnerId(), 
                    shop.getWalletTypeId(), totalPrice);
            DevFlowLogger.status("购买交易子流程", "转账完成 - 从玩家到商店所有者: " + totalPrice);
        } else {
            DevFlowLogger.status("购买交易子流程", "跳过转账（管理员商店）");
        }

        DevFlowLogger.step("购买交易子流程", "从商店箱子移除物品");
        if (!shop.isAdminShop() && !shop.isInfinite()) {
            removeItemsFromChest(shop, quantity);
            DevFlowLogger.status("购买交易子流程", "已从箱子移除 " + quantity + " 个物品");
        } else {
            DevFlowLogger.status("购买交易子流程", "跳过移除物品（管理员商店或无限制模式）");
        }

        DevFlowLogger.step("购买交易子流程", "向玩家发放物品");
        giveItemsToPlayer(player, shop.getTradeItem(), quantity);
        DevFlowLogger.status("购买交易子流程", "已向玩家发放 " + quantity + " 个物品");

        DevFlowLogger.step("购买交易子流程", "更新动态价格和保存数据");
        DynamicPricing.updatePriceAfterTransaction(shop, true);
        shopManager.markDirty();
        DevFlowLogger.param("购买交易子流程", "新价格", shop.getCurrentPrice());

        transactionLogger.logBuy(player.getUUID(), player.getName().getString(), shop,
                ItemUtils.getItemDisplayName(shop.getTradeItem()), quantity, totalPrice, currencyName);

        LOGGER.info("[商店交易] 玩家 {} 从商店 {} 购买了 {}x{} - 花费: {} {}", 
                player.getName().getString(),
                shop.getShopId(),
                shop.getTradeItem().getDisplayName().getString(),
                quantity,
                totalPrice,
                currencyName);

        player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.buy.success", 
                totalPrice, currencyName));

        DevFlowLogger.step("购买交易子流程", "同步更新告示牌显示");
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }
        DevFlowLogger.status("购买交易子流程", "告示牌已同步更新");

        DevFlowLogger.endFlow("购买交易子流程", true, 
                "购买成功 - 物品: " + shop.getTradeItem().getDisplayName().getString() + 
                " x" + quantity + 
                ", 花费: " + totalPrice + " " + currencyName);
        return true;
    }

    private boolean executeSellTransaction(ServerPlayer player, ShopInstance shop, int quantity,
                                           double totalPrice, String currencyName, WalletType walletType) {
        DevFlowLogger.startFlow("出售交易子流程");
        DevFlowLogger.param("出售交易子流程", "player", player.getName().getString());
        DevFlowLogger.param("出售交易子流程", "quantity", quantity);
        DevFlowLogger.param("出售交易子流程", "totalPrice", totalPrice);

        DevFlowLogger.step("出售交易子流程", "检查玩家物品数量");
        if (!hasEnoughItems(player, shop.getTradeItem(), quantity)) {
            DevFlowLogger.warning("出售交易子流程", "玩家物品不足 - 需要: " + quantity);
            player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.insufficient_items"));
            DevFlowLogger.endFlow("出售交易子流程", false, "玩家物品不足");
            return false;
        }
        DevFlowLogger.status("出售交易子流程", "玩家物品检查通过");

        MinecraftServer server = player.level().getServer();
        
        DevFlowLogger.step("出售交易子流程", "检查商店所有者余额和箱子容量");
        if (!shop.isAdminShop() && !shop.isInfinite()) {
            double ownerBalance = shopManager.getPlayerBalance(shop.getOwnerId(), shop.getWalletTypeId());
            DevFlowLogger.param("出售交易子流程", "商店所有者余额", ownerBalance);
            
            if (!shopManager.hasEnoughBalance(shop.getOwnerId(), shop.getWalletTypeId(), totalPrice)) {
                DevFlowLogger.warning("出售交易子流程", "商店所有者余额不足 - 需要: " + totalPrice + ", 实际: " + ownerBalance);
                player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.owner_insufficient_balance"));
                
                if (server != null) {
                    ServerPlayer ownerPlayer = server.getPlayerList().getPlayer(shop.getOwnerId());
                    if (ownerPlayer != null) {
                        ownerPlayer.sendSystemMessage(ShopTranslationHelper.translatable(
                                "transaction.owner_notification", currencyName));
                        DevFlowLogger.status("出售交易子流程", "已通知商店所有者余额不足");
                    }
                }
                DevFlowLogger.endFlow("出售交易子流程", false, "商店所有者余额不足");
                return false;
            }

            int availableSpace = getChestAvailableSpace(shop);
            DevFlowLogger.param("出售交易子流程", "箱子可用空间", availableSpace);
            
            if (availableSpace < quantity) {
                DevFlowLogger.warning("出售交易子流程", "箱子空间不足 - 需要: " + quantity + ", 可用: " + availableSpace);
                player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.insufficient_space"));
                DevFlowLogger.endFlow("出售交易子流程", false, "箱子空间不足");
                return false;
            }
            DevFlowLogger.status("出售交易子流程", "商店所有者余额和箱子容量检查通过");
        } else {
            DevFlowLogger.status("出售交易子流程", "跳过检查（管理员商店或无限制模式）");
        }

        DevFlowLogger.step("出售交易子流程", "执行资金转账（从商店所有者到玩家）");
        if (!shop.isAdminShop()) {
            shopManager.transferMoney(shop.getOwnerId(), player.getUUID(), 
                    shop.getWalletTypeId(), totalPrice);
            DevFlowLogger.status("出售交易子流程", "转账完成 - 从商店所有者到玩家: " + totalPrice);
        } else {
            shopManager.depositToPlayer(player.getUUID(), shop.getWalletTypeId(), totalPrice);
            DevFlowLogger.status("出售交易子流程", "系统向玩家发放: " + totalPrice + "（管理员商店）");
        }

        DevFlowLogger.step("出售交易子流程", "从玩家收取物品");
        takeItemsFromPlayer(player, shop.getTradeItem(), quantity);
        DevFlowLogger.status("出售交易子流程", "已从玩家收取 " + quantity + " 个物品");

        DevFlowLogger.step("出售交易子流程", "将物品添加到商店箱子");
        if (!shop.isAdminShop() && !shop.isInfinite()) {
            addItemsToChest(shop, quantity);
            DevFlowLogger.status("出售交易子流程", "已向箱子添加 " + quantity + " 个物品");
        } else {
            DevFlowLogger.status("出售交易子流程", "跳过添加物品（管理员商店或无限制模式）");
        }

        DevFlowLogger.step("出售交易子流程", "更新动态价格和保存数据");
        DynamicPricing.updatePriceAfterTransaction(shop, false);
        shopManager.markDirty();
        DevFlowLogger.param("出售交易子流程", "新价格", shop.getCurrentPrice());

        transactionLogger.logSell(player.getUUID(), player.getName().getString(), shop,
                ItemUtils.getItemDisplayName(shop.getTradeItem()), quantity, totalPrice, currencyName);

        LOGGER.info("[商店交易] 玩家 {} 向商店 {} 出售了 {}x{} - 获得: {} {}", 
                player.getName().getString(),
                shop.getShopId(),
                shop.getTradeItem().getDisplayName().getString(),
                quantity,
                totalPrice,
                currencyName);

        player.sendSystemMessage(ShopTranslationHelper.translatable("transaction.sell.success", 
                totalPrice, currencyName));

        DevFlowLogger.step("出售交易子流程", "同步更新告示牌显示");
        if (player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
            SignUpdateHelper.updateSignForShop(shop, serverLevel);
        }
        DevFlowLogger.status("出售交易子流程", "告示牌已同步更新");

        DevFlowLogger.endFlow("出售交易子流程", true, 
                "出售成功 - 物品: " + shop.getTradeItem().getDisplayName().getString() + 
                " x" + quantity + 
                ", 获得: " + totalPrice + " " + currencyName);
        return true;
    }

    private int getShopStock(ShopInstance shop) {
        Level level = shopManager.getServer().getLevel(Level.OVERWORLD);
        if (level == null) return 0;

        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(shop.getShopPos());
        if (!(blockEntity instanceof Container container)) {
            return 0;
        }

        int count = 0;
        ItemStack tradeItem = shop.getTradeItem();
        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotItem = container.getItem(i);
            if (ItemUtils.itemsMatch(slotItem, tradeItem)) {
                count += slotItem.getCount();
            }
        }
        return count;
    }

    private int getChestAvailableSpace(ShopInstance shop) {
        Level level = shopManager.getServer().getLevel(Level.OVERWORLD);
        if (level == null) return 0;

        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(shop.getShopPos());
        if (!(blockEntity instanceof Container container)) {
            return 0;
        }

        int space = 0;
        ItemStack tradeItem = shop.getTradeItem();
        int maxStackSize = tradeItem.getMaxStackSize();

        for (int i = 0; i < container.getContainerSize(); i++) {
            ItemStack slotItem = container.getItem(i);
            if (slotItem.isEmpty()) {
                space += maxStackSize;
            } else if (ItemUtils.itemsMatch(slotItem, tradeItem)) {
                space += maxStackSize - slotItem.getCount();
            }
        }
        return space;
    }

    private void removeItemsFromChest(ShopInstance shop, int quantity) {
        Level level = shopManager.getServer().getLevel(Level.OVERWORLD);
        if (level == null) return;

        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(shop.getShopPos());
        if (!(blockEntity instanceof Container container)) {
            return;
        }

        ItemStack tradeItem = shop.getTradeItem();
        int remaining = quantity;

        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slotItem = container.getItem(i);
            if (ItemUtils.itemsMatch(slotItem, tradeItem)) {
                int toRemove = Math.min(remaining, slotItem.getCount());
                slotItem.shrink(toRemove);
                remaining -= toRemove;
            }
        }
    }

    private void addItemsToChest(ShopInstance shop, int quantity) {
        Level level = shopManager.getServer().getLevel(Level.OVERWORLD);
        if (level == null) return;

        net.minecraft.world.level.block.entity.BlockEntity blockEntity = level.getBlockEntity(shop.getShopPos());
        if (!(blockEntity instanceof Container container)) {
            return;
        }

        ItemStack tradeItem = shop.getTradeItem();
        int remaining = quantity;
        int maxStackSize = tradeItem.getMaxStackSize();

        for (int i = 0; i < container.getContainerSize() && remaining > 0; i++) {
            ItemStack slotItem = container.getItem(i);
            if (slotItem.isEmpty()) {
                int toAdd = Math.min(remaining, maxStackSize);
                container.setItem(i, ItemUtils.copyItemStackWithCount(tradeItem, toAdd));
                remaining -= toAdd;
            } else if (ItemUtils.itemsMatch(slotItem, tradeItem) && slotItem.getCount() < maxStackSize) {
                int toAdd = Math.min(remaining, maxStackSize - slotItem.getCount());
                slotItem.grow(toAdd);
                remaining -= toAdd;
            }
        }
    }

    private boolean hasEnoughItems(ServerPlayer player, ItemStack tradeItem, int quantity) {
        int count = 0;
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack item = inventory.getItem(i);
            if (ItemUtils.itemsMatch(item, tradeItem)) {
                count += item.getCount();
            }
        }
        return count >= quantity;
    }

    private void giveItemsToPlayer(ServerPlayer player, ItemStack tradeItem, int quantity) {
        int remaining = quantity;
        int maxStackSize = tradeItem.getMaxStackSize();

        while (remaining > 0) {
            int toGive = Math.min(remaining, maxStackSize);
            ItemStack stack = ItemUtils.copyItemStackWithCount(tradeItem, toGive);
            player.getInventory().add(stack);
            remaining -= toGive;
        }
    }

    private void takeItemsFromPlayer(ServerPlayer player, ItemStack tradeItem, int quantity) {
        int remaining = quantity;
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();

        for (int i = 0; i < inventory.getContainerSize() && remaining > 0; i++) {
            ItemStack slotItem = inventory.getItem(i);
            if (ItemUtils.itemsMatch(slotItem, tradeItem)) {
                int toTake = Math.min(remaining, slotItem.getCount());
                slotItem.shrink(toTake);
                remaining -= toTake;
            }
        }
    }
}
