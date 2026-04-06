package me.tuanzi.shop.shop;

import me.tuanzi.economy.api.EconomyAPI;
import me.tuanzi.economy.api.EconomyAPIImpl;
import me.tuanzi.economy.currency.WalletType;
import me.tuanzi.shop.storage.ShopStateSaver;
import me.tuanzi.shop.util.DevFlowLogger;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

public class ShopManager {
    private static ShopManager instance;
    private final MinecraftServer server;
    private final ShopRegistry registry;
    private final EconomyAPI economyAPI;

    private ShopManager(MinecraftServer server) {
        this.server = server;
        this.registry = ShopRegistry.getInstance(server);
        this.economyAPI = EconomyAPIImpl.getInstance(server);
    }

    public static synchronized ShopManager getInstance(MinecraftServer server) {
        if (instance == null) {
            instance = new ShopManager(server);
        }
        return instance;
    }

    public static synchronized void resetInstance() {
        instance = null;
        ShopRegistry.resetInstance();
    }

    public ShopInstance createShop(UUID ownerId, BlockPos shopPos, BlockPos signPos,
                                   ShopType shopType, ItemStack tradeItem, double price,
                                   String walletTypeId, String description) {
        DevFlowLogger.startFlow("创建商店");

        DevFlowLogger.step("创建商店", "验证钱包类型", "walletTypeId=", walletTypeId);
        if (!validateWalletType(walletTypeId)) {
            DevFlowLogger.error("创建商店", "钱包类型验证失败: " + walletTypeId);
            DevFlowLogger.endFlow("创建商店", false, "无效的钱包类型");
            return null;
        }
        DevFlowLogger.status("创建商店", "钱包类型验证通过");

        DevFlowLogger.step("创建商店", "生成商店ID和基础信息");
        UUID shopId = UUID.randomUUID();
        long createdTime = System.currentTimeMillis();
        DevFlowLogger.param("创建商店", "shopId", shopId);
        DevFlowLogger.param("创建商店", "ownerId", ownerId);
        DevFlowLogger.param("创建商店", "shopPos", shopPos);
        DevFlowLogger.param("创建商店", "signPos", signPos);
        DevFlowLogger.param("创建商店", "shopType", shopType);
        DevFlowLogger.param("创建商店", "tradeItem", tradeItem.getDisplayName().getString());
        DevFlowLogger.param("创建商店", "price", price);
        DevFlowLogger.param("创建商店", "walletTypeId", walletTypeId);

        DevFlowLogger.step("创建商店", "实例化ShopInstance对象");
        ShopInstance shop = new ShopInstance(shopId, ownerId, shopPos, signPos, shopType,
                tradeItem, price, walletTypeId, createdTime);
        shop.setDescription(description);
        DevFlowLogger.status("创建商店", "ShopInstance对象创建成功");

        DevFlowLogger.step("创建商店", "注册商店到Registry");
        registry.registerShop(shop);
        DevFlowLogger.status("创建商店", "商店已注册到系统");
        DevFlowLogger.param("创建商店", "当前商店总数", registry.getShopCount());

        DevFlowLogger.endFlow("创建商店", true, "商店创建成功 - ID: " + shopId.toString().substring(0, 8));
        return shop;
    }

    public ShopInstance createAdminShop(BlockPos shopPos, BlockPos signPos,
                                        ShopType shopType, ItemStack tradeItem, double price,
                                        String walletTypeId) {
        DevFlowLogger.startFlow("创建管理员商店");

        DevFlowLogger.step("创建管理员商店", "验证钱包类型", "walletTypeId=", walletTypeId);
        if (!validateWalletType(walletTypeId)) {
            DevFlowLogger.error("创建管理员商店", "钱包类型验证失败: " + walletTypeId);
            DevFlowLogger.endFlow("创建管理员商店", false, "无效的钱包类型");
            return null;
        }
        DevFlowLogger.status("创建管理员商店", "钱包类型验证通过");

        DevFlowLogger.step("创建管理员商店", "生成商店ID和基础信息");
        UUID shopId = UUID.randomUUID();
        long createdTime = System.currentTimeMillis();
        UUID adminUuid = new UUID(0, 0);

        DevFlowLogger.param("创建管理员商店", "shopId", shopId);
        DevFlowLogger.param("创建管理员商店", "shopPos", shopPos);
        DevFlowLogger.param("创建管理员商店", "signPos", signPos);
        DevFlowLogger.param("创建管理员商店", "shopType", shopType);
        DevFlowLogger.param("创建管理员商店", "tradeItem", tradeItem.getDisplayName().getString());
        DevFlowLogger.param("创建管理员商店", "price", price);
        DevFlowLogger.param("创建管理员商店", "walletTypeId", walletTypeId);

        DevFlowLogger.step("创建管理员商店", "实例化管理员ShopInstance对象");
        ShopInstance shop = new ShopInstance(shopId, adminUuid, shopPos, signPos, shopType,
                tradeItem, price, walletTypeId, createdTime);
        shop.setAdminShop(true);
        shop.setInfinite(true);
        DevFlowLogger.status("创建管理员商店", "已设置管理员标识和无限制模式");

        DevFlowLogger.step("创建管理员商店", "注册商店到Registry");
        registry.registerShop(shop);
        DevFlowLogger.status("创建管理员商店", "管理员商店已注册到系统");
        DevFlowLogger.param("创建管理员商店", "当前商店总数", registry.getShopCount());

        DevFlowLogger.endFlow("创建管理员商店", true, "管理员商店创建成功 - ID: " + shopId.toString().substring(0, 8));
        return shop;
    }

    public void deleteShop(UUID shopId) {
        DevFlowLogger.startFlow("删除商店");
        DevFlowLogger.param("删除商店", "shopId", shopId);

        DevFlowLogger.step("删除商店", "检查商店是否存在");
        Optional<ShopInstance> existingShop = getShopById(shopId);
        if (existingShop.isEmpty()) {
            DevFlowLogger.warning("删除商店", "商店不存在，可能已被删除: " + shopId);
            DevFlowLogger.endFlow("删除商店", false, "商店不存在");
            return;
        }
        DevFlowLogger.status("删除商店", "商店存在，准备删除");

        ShopInstance shop = existingShop.get();
        DevFlowLogger.param("删除商店", "ownerId", shop.getOwnerId());
        DevFlowLogger.param("删除商店", "shopPos", shop.getShopPos());
        DevFlowLogger.param("删除商店", "tradeItem", shop.getTradeItem().getDisplayName().getString());

        DevFlowLogger.step("删除商店", "从Registry中注销商店");
        registry.unregisterShop(shopId);
        DevFlowLogger.status("删除商店", "商店已从系统注销");
        DevFlowLogger.param("删除商店", "剩余商店总数", registry.getShopCount());

        DevFlowLogger.endFlow("删除商店", true, "商店删除成功 - ID: " + shopId.toString().substring(0, 8));
    }

    public Optional<ShopInstance> getShopById(UUID shopId) {
        return registry.getShopById(shopId);
    }

    public Optional<ShopInstance> getShopByPos(BlockPos pos) {
        return registry.getShopByPos(pos);
    }

    public Collection<ShopInstance> getShopsByOwner(UUID ownerId) {
        return registry.getShopsByOwner(ownerId);
    }

    public Collection<ShopInstance> getAllShops() {
        return registry.getAllShops();
    }

    public Optional<ShopInstance> getShopPlayerLookingAt(ServerPlayer player) {
        double reachDistance = 5.0;
        Vec3 eyePosition = player.getEyePosition();
        Vec3 lookVector = player.getLookAngle();
        Vec3 endPosition = eyePosition.add(lookVector.x * reachDistance,
                lookVector.y * reachDistance, lookVector.z * reachDistance);

        Level level = player.level();
        ClipContext context = new ClipContext(eyePosition, endPosition,
                ClipContext.Block.OUTLINE, ClipContext.Fluid.NONE, player);
        BlockHitResult hitResult = level.clip(context);

        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos hitPos = hitResult.getBlockPos();
            Optional<ShopInstance> shopOpt = getShopByPos(hitPos);
            if (shopOpt.isPresent()) {
                return shopOpt;
            }
            Direction direction = hitResult.getDirection();
            BlockPos adjacentPos = hitPos.relative(direction);
            return getShopByPos(adjacentPos);
        }
        return Optional.empty();
    }

    public boolean validateWalletType(String walletTypeId) {
        if (walletTypeId == null || walletTypeId.isEmpty()) {
            return false;
        }
        return economyAPI.getWalletType(walletTypeId).isPresent();
    }

    public Optional<WalletType> findWalletTypeByDisplayName(String displayName) {
        return economyAPI.getAllWalletTypes().stream()
                .filter(wt -> wt.displayName().getString().equals(displayName))
                .findFirst();
    }

    public Optional<WalletType> findWalletTypeById(String walletTypeId) {
        return economyAPI.getWalletType(walletTypeId);
    }

    public String getCurrencyDisplayName(String walletTypeId) {
        return economyAPI.getWalletType(walletTypeId)
                .map(wt -> wt.displayName().getString())
                .orElse("Unknown");
    }

    public Optional<WalletType> getWalletType(String walletTypeId) {
        return economyAPI.getWalletType(walletTypeId);
    }

    public double getPlayerBalance(UUID playerId, String walletTypeId) {
        return economyAPI.getBalance(playerId, walletTypeId);
    }

    public boolean hasEnoughBalance(UUID playerId, String walletTypeId, double amount) {
        return economyAPI.hasEnough(playerId, walletTypeId, amount);
    }

    public void transferMoney(UUID from, UUID to, String walletTypeId, double amount) {
        economyAPI.transfer(from, to, walletTypeId, amount);
    }

    public void depositToPlayer(UUID playerId, String walletTypeId, double amount) {
        economyAPI.deposit(playerId, walletTypeId, amount);
    }

    public void withdrawFromPlayer(UUID playerId, String walletTypeId, double amount) {
        economyAPI.withdraw(playerId, walletTypeId, amount);
    }

    public void markDirty() {
        ShopStateSaver.getServerState(server).setDirty();
    }

    public MinecraftServer getServer() {
        return server;
    }

    public EconomyAPI getEconomyAPI() {
        return economyAPI;
    }
}
