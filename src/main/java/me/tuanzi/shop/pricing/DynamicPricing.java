package me.tuanzi.shop.pricing;

import me.tuanzi.shop.shop.ShopInstance;

public class DynamicPricing {
    private DynamicPricing() {
    }

    public static double calculatePrice(ShopInstance shop) {
        if (!shop.isDynamicPricing()) {
            return shop.getBasePrice();
        }

        double basePrice = shop.getBasePrice();
        long sold = shop.getTotalSold();
        long bought = shop.getTotalBought();

        double ratio;
        if (bought == 0) {
            ratio = sold > 0 ? 2.0 : 1.0;
        } else {
            ratio = (double) sold / bought;
        }

        double price = basePrice * ratio;

        return Math.max(shop.getMinPrice(), Math.min(shop.getMaxPrice(), price));
    }

    public static void updatePriceAfterTransaction(ShopInstance shop, boolean isSold) {
        if (!shop.isDynamicPricing()) {
            return;
        }

        if (isSold) {
            shop.incrementTotalSold(1);
        } else {
            shop.incrementTotalBought(1);
        }

        double newPrice = calculatePrice(shop);
        shop.setCurrentPrice(newPrice);
    }

    public static double calculateBulkPrice(ShopInstance shop, int quantity) {
        return calculatePrice(shop) * quantity;
    }
}
