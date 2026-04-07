package me.tuanzi.shop;

import me.tuanzi.shop.commands.ShopAdminCommand;
import me.tuanzi.shop.commands.ShopCommand;
import me.tuanzi.shop.commands.ShopConfirmCommand;
import me.tuanzi.shop.config.ShopConfig;
import me.tuanzi.shop.display.ShopDisplayManager;
import me.tuanzi.shop.events.BlockInteractionHandler;
import me.tuanzi.shop.events.BlockProtectionHandler;
import me.tuanzi.shop.events.ChatInputHandler;
import me.tuanzi.shop.logging.TransactionLogger;
import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopManager;
import me.tuanzi.shop.shop.ShopRegistry;
import me.tuanzi.shop.storage.ShopStateSaver;
import me.tuanzi.shop.utils.ShopTranslationHelper;
import me.tuanzi.shop.util.DevFlowLogger;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackBlockCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.SignedMessageBody;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class ShopModule implements ModInitializer {
    public static final String MOD_ID = "shop-module";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    private static final Map<MinecraftServer, ShopModule> instances = new HashMap<>();
    private static ShopConfig config;

    private MinecraftServer server;
    private ShopManager shopManager;
    private TransactionLogger transactionLogger;
    private ShopDisplayManager displayManager;
    private BlockInteractionHandler interactionHandler;
    private BlockProtectionHandler protectionHandler;
    private ChatInputHandler chatInputHandler;

    public ShopModule() {
    }

    private void initializeForServer(MinecraftServer server) {
        this.server = server;
        this.shopManager = ShopManager.getInstance(server);

        Path worldPath = server.getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        this.transactionLogger = new TransactionLogger(worldPath, true);

        this.displayManager = new ShopDisplayManager(shopManager);
        this.interactionHandler = new BlockInteractionHandler(shopManager, transactionLogger);
        this.protectionHandler = new BlockProtectionHandler(shopManager);
        this.chatInputHandler = new ChatInputHandler(shopManager, transactionLogger);
    }

    public static ShopModule getInstance(MinecraftServer server) {
        return instances.computeIfAbsent(server, s -> {
            ShopModule module = new ShopModule();
            module.initializeForServer(s);
            return module;
        });
    }

    public static ShopConfig getConfig() {
        return config;
    }

    @Override
    public void onInitialize() {
        LOGGER.info("正在初始化商店模块...");

        config = new ShopConfig();
        config.load();

        ServerLifecycleEvents.SERVER_STARTING.register(this::onServerStarting);
        ServerLifecycleEvents.SERVER_STOPPED.register(this::onServerStopped);

        ServerTickEvents.START_SERVER_TICK.register(server -> {
            ShopModule instance = getInstance(server);
            if (instance != null) {
                ServerLevel overworld = server.getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    if (instance.displayManager != null) {
                        instance.displayManager.maintainAllDisplays(overworld);
                    }
                    
                    // 每日系统库存衰减逻辑 (积攒到 24000 tick)
                    ShopStateSaver saver = ShopStateSaver.getServerState(server);
                    saver.addTick();
                    if (saver.getAccumulatedTicks() >= 24000) {
                        if (instance.shopManager != null) {
                            instance.shopManager.decaySystemStock(overworld);
                        }
                        saver.setAccumulatedTicks(0);
                    }
                }
            }
        });

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            ShopAdminCommand.register(dispatcher, registryAccess);
            ShopCommand.register(dispatcher, registryAccess);
            ShopConfirmCommand.register(dispatcher, registryAccess);
        });

        UseBlockCallback.EVENT.register(this::onUseBlock);
        AttackBlockCallback.EVENT.register(this::onAttackBlock);
        PlayerBlockBreakEvents.BEFORE.register(this::onBlockBreak);

        ServerMessageEvents.ALLOW_CHAT_MESSAGE.register((message, sender, params) -> {
            MinecraftServer srv = sender.level().getServer();
            if (srv == null) {
                return true;
            }

            ShopModule instance = getInstance(srv);
            if (instance == null || instance.chatInputHandler == null) {
                return true;
            }

            String content = message.signedBody().content();
            return !instance.chatInputHandler.handleChatInput(sender, content);
        });

        LOGGER.info("商店模块初始化完成！");
    }

    private void onServerStarting(MinecraftServer server) {
        ShopModule instance = getInstance(server);
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld != null && instance.displayManager != null) {
            instance.displayManager.initializeAllDisplays(overworld);
        }
        int shopCount = instance.shopManager != null ? instance.shopManager.getAllShops().size() : 0;
        LOGGER.info("商店模块已加载 {} 个商店", shopCount);
    }

    private void onServerStopped(MinecraftServer server) {
        ShopManager.resetInstance();
        ShopRegistry.resetInstance();
        instances.remove(server);
        LOGGER.info("商店模块已卸载");
    }

    private InteractionResult onUseBlock(Player player, Level world, InteractionHand hand, BlockHitResult hitResult) {
        if (world.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        MinecraftServer srv = serverPlayer.level().getServer();
        if (srv == null) {
            return InteractionResult.PASS;
        }

        ShopModule instance = getInstance(srv);
        if (instance == null || instance.protectionHandler == null || instance.interactionHandler == null) {
            return InteractionResult.PASS;
        }

        boolean isShiftDown = serverPlayer.isShiftKeyDown();
        BlockPos hitPos = hitResult.getBlockPos();

        if (!instance.protectionHandler.canOpenContainer(serverPlayer, hitPos)) {
            return InteractionResult.FAIL;
        }

        Optional<ShopInstance> existingShop = instance.shopManager.getShopByPos(hitPos);
        if (existingShop.isPresent()) {
            if (instance.interactionHandler.handleBlockInteraction(serverPlayer, hitPos, false, isShiftDown)) {
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        }

        if (handleQuickShopCreation(serverPlayer, world, hitPos, hand, instance)) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean handleQuickShopCreation(ServerPlayer player, Level world, BlockPos pos, InteractionHand hand, ShopModule instance) {
        if (world.getBlockEntity(pos) instanceof net.minecraft.world.level.block.entity.SignBlockEntity signEntity) {
            if (!isSignEmpty(signEntity)) {
                return false;
            }

            BlockPos containerPos = getContainerPosForSign(world, pos);
            if (containerPos == null) {
                return false;
            }
            
            if (!(world.getBlockEntity(containerPos) instanceof net.minecraft.world.Container)) {
                return false;
            }

            ItemStack heldItem = player.getItemInHand(hand);
            if (heldItem.isEmpty()) {
                return false;
            }

            if (instance.chatInputHandler == null) {
                return false;
            }

            if (instance.chatInputHandler.hasPendingShopCreation(player.getUUID())) {
                player.sendSystemMessage(ShopTranslationHelper.colored("§c您有一个正在进行的商店创建流程，请先完成或等待超时"));
                return true;
            }

            instance.chatInputHandler.startShopCreation(player, pos, heldItem, containerPos);
            return true;
        }
        return false;
    }
    
    private BlockPos getContainerPosForSign(Level world, BlockPos signPos) {
        net.minecraft.world.level.block.state.BlockState state = world.getBlockState(signPos);
        net.minecraft.world.level.block.Block block = state.getBlock();
        
        if (block instanceof net.minecraft.world.level.block.StandingSignBlock) {
            BlockPos belowPos = signPos.below();
            if (world.getBlockEntity(belowPos) instanceof net.minecraft.world.Container) {
                return belowPos;
            }
        } else if (block instanceof net.minecraft.world.level.block.WallSignBlock) {
            net.minecraft.core.Direction facing = state.getValue(net.minecraft.world.level.block.WallSignBlock.FACING);
            BlockPos attachedPos = signPos.relative(facing.getOpposite());
            if (world.getBlockEntity(attachedPos) instanceof net.minecraft.world.Container) {
                return attachedPos;
            }
        }
        
        for (net.minecraft.core.Direction dir : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            BlockPos checkPos = signPos.relative(dir);
            if (world.getBlockEntity(checkPos) instanceof net.minecraft.world.Container) {
                return checkPos;
            }
        }
        
        BlockPos belowPos = signPos.below();
        if (world.getBlockEntity(belowPos) instanceof net.minecraft.world.Container) {
            return belowPos;
        }
        
        return null;
    }

    private boolean isSignEmpty(net.minecraft.world.level.block.entity.SignBlockEntity sign) {
        var frontText = sign.getFrontText();
        for (int i = 0; i < 4; i++) {
            String line = frontText.getMessage(i, false).getString();
            if (!line.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private InteractionResult onAttackBlock(Player player, Level world, InteractionHand hand, net.minecraft.core.BlockPos pos, net.minecraft.core.Direction direction) {
        if (world.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        MinecraftServer srv = serverPlayer.level().getServer();
        if (srv == null) {
            return InteractionResult.PASS;
        }

        ShopModule instance = getInstance(srv);
        if (instance == null || instance.interactionHandler == null) {
            return InteractionResult.PASS;
        }

        if (instance.interactionHandler.handleBlockInteraction(serverPlayer, pos, true, serverPlayer.isShiftKeyDown())) {
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.PASS;
    }

    private boolean onBlockBreak(Level world, Player player, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state, net.minecraft.world.level.block.entity.BlockEntity blockEntity) {
        if (world.isClientSide() || !(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) {
            return true;
        }

        MinecraftServer srv = serverPlayer.level().getServer();
        if (srv == null) {
            return true;
        }

        ShopModule instance = getInstance(srv);
        if (instance == null || instance.protectionHandler == null) {
            return true;
        }

        boolean canBreak = instance.protectionHandler.canBreakBlock(serverPlayer, pos);
        if (!canBreak) {
            restoreSignText(world, pos, blockEntity, state);
            return false;
        }

        if (instance.shopManager != null && instance.displayManager != null && world instanceof ServerLevel serverLevel) {
            Optional<ShopInstance> shopAtPos = instance.shopManager.getShopByPos(pos);
            
            if (shopAtPos.isEmpty()) {
                shopAtPos = findShopBySignOrContainerPos(instance.shopManager, pos, serverLevel);
            }
            
            if (shopAtPos.isPresent()) {
                ShopInstance shop = shopAtPos.get();
                
                if (!shop.isValid()) {
                    LOGGER.warn("检测到已标记删除的商店，强制清理 - 商店ID: {}, 位置: {}", shop.getShopId(), pos);
                    forceDeleteShop(instance, shop, serverLevel, serverPlayer);
                    return true;
                }
                
                LOGGER.info("检测到商店破坏事件 - 商店ID: {}, 破坏位置: {}, 商店箱子: {}, 告示牌: {}",
                        shop.getShopId(), pos, shop.getShopPos(), shop.getSignPos());
                forceDeleteShop(instance, shop, serverLevel, serverPlayer);
            }
        }

        return true;
    }

    private Optional<ShopInstance> findShopBySignOrContainerPos(ShopManager shopManager, BlockPos pos, ServerLevel level) {
        Optional<ShopInstance> directShop = shopManager.getShopByPos(pos);
        if (directShop.isPresent()) {
            return directShop;
        }

        for (ShopInstance shop : shopManager.getAllShops()) {
            if (shop.getSignPos().equals(pos)) {
                LOGGER.debug("通过告示牌位置找到商店 - 商店ID: {}, 告示牌位置: {}", shop.getShopId(), pos);
                return Optional.of(shop);
            }
        }

        return Optional.empty();
    }

    private void forceDeleteShop(ShopModule instance, ShopInstance shop, ServerLevel level, ServerPlayer player) {
        DevFlowLogger.startFlow("商店删除流程");
        DevFlowLogger.param("商店删除流程", "shopId", shop.getShopId());
        DevFlowLogger.param("商店删除流程", "ownerId", shop.getOwnerId());
        DevFlowLogger.param("商店删除流程", "shopPos", shop.getShopPos());
        DevFlowLogger.param("商店删除流程", "signPos", shop.getSignPos());
        DevFlowLogger.param("商店删除流程", "操作玩家", player.getName().getString());

        UUID shopId = shop.getShopId();
        
        LOGGER.info("正在删除商店 - 商店ID: {}, 所有者: {}", shopId, shop.getOwnerId());
        DevFlowLogger.step("商店删除流程", "标记商店为已删除");
        
        shop.markAsDeleted();
        DevFlowLogger.status("商店删除流程", "商店已标记为删除状态");

        DevFlowLogger.step("商店删除流程", "移除悬浮物品显示");
        instance.displayManager.removeDisplayForShop(shopId, level);
        DevFlowLogger.status("商店删除流程", "悬浮物品显示已移除");

        DevFlowLogger.step("商店删除流程", "从系统中注销商店");
        instance.shopManager.deleteShop(shopId);
        DevFlowLogger.status("商店删除流程", "商店已从系统注销");
        
        if (instance.chatInputHandler != null) {
            DevFlowLogger.step("商店删除流程", "清理待处理状态");
            instance.chatInputHandler.cleanupForShop(shopId);
            DevFlowLogger.status("商店删除流程", "待处理状态已清理");
        }
        
        player.sendSystemMessage(ShopTranslationHelper.colored("§a商店已成功删除"));
        
        LOGGER.info("商店删除完成 - 商店ID: {}", shopId);

        DevFlowLogger.endFlow("商店删除流程", true, 
                "商店删除成功 - ID: " + shopId.toString().substring(0, 8));
    }

    
    private void restoreSignText(Level world, net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.entity.BlockEntity blockEntity, net.minecraft.world.level.block.state.BlockState state) {
        if (!(world instanceof ServerLevel serverLevel)) {
            return;
        }
        
        if (blockEntity instanceof SignBlockEntity signEntity) {
            world.getServer().execute(() -> {
                if (world.getBlockEntity(pos) instanceof SignBlockEntity currentSign) {
                    currentSign.setChanged();
                    world.sendBlockUpdated(pos, state, state, 3);
                }
            });
        }
    }

    public ShopManager getShopManager() {
        return shopManager;
    }

    public TransactionLogger getTransactionLogger() {
        return transactionLogger;
    }

    public ShopDisplayManager getDisplayManager() {
        return displayManager;
    }

    public BlockInteractionHandler getInteractionHandler() {
        return interactionHandler;
    }

    public BlockProtectionHandler getProtectionHandler() {
        return protectionHandler;
    }

    public ChatInputHandler getChatInputHandler() {
        return chatInputHandler;
    }
}
