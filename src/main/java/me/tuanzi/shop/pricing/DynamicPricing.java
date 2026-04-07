package me.tuanzi.shop.pricing;

import me.tuanzi.shop.shop.ShopInstance;
import me.tuanzi.shop.shop.ShopType;

public class DynamicPricing {
    private DynamicPricing() {
    }

    /**
     * 计算单单价 (基于当前系统活动量 S)
     * 收购商店 (BUY): P = x + (y - x) * K / (K + S)  [越卖越便宜]
     * 出售商店 (SELL): P = x + (y - x) * S / (K + S) [越买越贵]
     */
    public static double calculatePrice(ShopInstance shop) {
        if (!shop.isDynamicPricing()) {
            return shop.getBasePrice();
        }
        return calculatePriceWithStock(shop, shop.getSystemStock());
    }

    /**
     * 给定特定活动量 S 计算单价
     */
    private static double calculatePriceWithStock(ShopInstance shop, double tempS) {
        double x = shop.getMinPrice();
        double y = shop.getMaxPrice();
        double K = shop.getHalfLifeConstant();
        double S = Math.max(0, tempS);

        if (K <= 0) K = 1.0;

        if (shop.getShopType() == ShopType.BUY) {
            // 收购商店：库存越多，收购价越低 (趋向 x)
            return x + (y - x) * (K / (K + S));
        } else {
            // 出售商店：卖出越多，售价越高 (趋向 y)
            // 当 S=0 时，价格为 x (最低价)
            return x + (y - x) * (S / (K + S));
        }
    }

    /**
     * 在交易后更新系统活动量 S
     * 无论是买还是卖，交易都会增加 S (增加市场压力)
     */
    public static void updatePriceAfterTransaction(ShopInstance shop, int quantity, boolean playerBuying) {
        if (!shop.isDynamicPricing()) {
            return;
        }

        // 增加活动量
        shop.incrementSystemStock(quantity);

        if (playerBuying) {
            shop.incrementTotalSold(quantity);
        } else {
            shop.incrementTotalBought(quantity);
        }

        shop.setCurrentPrice(calculatePrice(shop));
    }

    /**
     * 批量计算总价
     * 每一个物品都会导致 S 的实时增加，从而影响下一个物品的单价
     */
    public static double calculateBulkPrice(ShopInstance shop, int quantity, boolean playerBuying) {
        if (!shop.isDynamicPricing()) {
            return shop.getBasePrice() * quantity;
        }

        double total = 0;
        double currentS = shop.getSystemStock();
        
        for (int i = 0; i < quantity; i++) {
            total += calculatePriceWithStock(shop, currentS);
            // 每一个物品成交，都会增加下一件物品的价格压力
            currentS += 1;
        }
        
        return total;
    }
}
