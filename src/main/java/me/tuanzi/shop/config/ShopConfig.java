package me.tuanzi.shop.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class ShopConfig {
    private static final Logger LOGGER = LoggerFactory.getLogger("shop-module");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private int inputTimeoutSeconds = 8;
    private boolean enableSimplifiedTransaction = true;
    private boolean enableItemChangeByOwner = true;
    private int maxTransactionQuantity = 2304;

    private final Path configPath;

    public ShopConfig() {
        this.configPath = Path.of("config/shop-module.json");
    }

    public void load() {
        if (!Files.exists(configPath)) {
            save();
            return;
        }

        try {
            String json = Files.readString(configPath);
            JsonObject obj = GSON.fromJson(json, JsonObject.class);

            if (obj.has("inputTimeoutSeconds")) {
                inputTimeoutSeconds = obj.get("inputTimeoutSeconds").getAsInt();
            }
            if (obj.has("enableSimplifiedTransaction")) {
                enableSimplifiedTransaction = obj.get("enableSimplifiedTransaction").getAsBoolean();
            }
            if (obj.has("enableItemChangeByOwner")) {
                enableItemChangeByOwner = obj.get("enableItemChangeByOwner").getAsBoolean();
            }
            if (obj.has("maxTransactionQuantity")) {
                maxTransactionQuantity = obj.get("maxTransactionQuantity").getAsInt();
            }

            LOGGER.info("Shop config loaded");
        } catch (Exception e) {
            LOGGER.warn("Failed to load shop config, using defaults: {}", e.getMessage());
            save();
        }
    }

    public void save() {
        try {
            Files.createDirectories(configPath.getParent());

            JsonObject obj = new JsonObject();
            obj.addProperty("inputTimeoutSeconds", inputTimeoutSeconds);
            obj.addProperty("enableSimplifiedTransaction", enableSimplifiedTransaction);
            obj.addProperty("enableItemChangeByOwner", enableItemChangeByOwner);
            obj.addProperty("maxTransactionQuantity", maxTransactionQuantity);

            Files.writeString(configPath, GSON.toJson(obj));
            LOGGER.info("Shop config saved");
        } catch (IOException e) {
            LOGGER.error("Failed to save shop config: {}", e.getMessage());
        }
    }

    public int getInputTimeoutSeconds() {
        return inputTimeoutSeconds;
    }

    public void setInputTimeoutSeconds(int seconds) {
        this.inputTimeoutSeconds = Math.max(1, Math.min(60, seconds));
        save();
    }

    public boolean isEnableSimplifiedTransaction() {
        return enableSimplifiedTransaction;
    }

    public boolean isEnableItemChangeByOwner() {
        return enableItemChangeByOwner;
    }

    public int getMaxTransactionQuantity() {
        return maxTransactionQuantity;
    }
}
