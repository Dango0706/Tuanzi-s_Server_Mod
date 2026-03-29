package me.tuanzi.statistics.util;

import net.minecraft.network.chat.Component;

public class TranslationHelper {
    
    public static String translateItemKey(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return "未知";
        }
        
        try {
            Component translated = Component.translatable(translationKey);
            String result = translated.getString();
            
            if (result.equals(translationKey)) {
                return formatRawKey(translationKey);
            }
            return result;
        } catch (Exception e) {
            return formatRawKey(translationKey);
        }
    }
    
    public static String translateBlockKey(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return "未知";
        }
        
        try {
            Component translated = Component.translatable(translationKey);
            String result = translated.getString();
            
            if (result.equals(translationKey)) {
                return formatRawKey(translationKey);
            }
            return result;
        } catch (Exception e) {
            return formatRawKey(translationKey);
        }
    }
    
    public static String translateEntityKey(String translationKey) {
        if (translationKey == null || translationKey.isEmpty()) {
            return "未知";
        }
        
        try {
            Component translated = Component.translatable(translationKey);
            String result = translated.getString();
            
            if (result.equals(translationKey)) {
                return formatRawKey(translationKey);
            }
            return result;
        } catch (Exception e) {
            return formatRawKey(translationKey);
        }
    }
    
    private static String formatRawKey(String key) {
        if (key == null || key.isEmpty()) {
            return "未知";
        }
        
        if (key.startsWith("item.minecraft.")) {
            String itemName = key.substring("item.minecraft.".length());
            return formatSnakeCase(itemName);
        }
        
        if (key.startsWith("block.minecraft.")) {
            String blockName = key.substring("block.minecraft.".length());
            return formatSnakeCase(blockName);
        }
        
        if (key.startsWith("entity.minecraft.")) {
            String entityName = key.substring("entity.minecraft.".length());
            return formatSnakeCase(entityName);
        }
        
        return key;
    }
    
    private static String formatSnakeCase(String str) {
        String[] parts = str.split("_");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.append(Character.toUpperCase(part.charAt(0)))
                      .append(part.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }
}
