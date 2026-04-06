package me.tuanzi.translation;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public final class MinecraftLanguageHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger("MinecraftLanguageHelper");
    private static final Gson GSON = new Gson();
    private static final Pattern UNSUPPORTED_FORMAT_PATTERN = Pattern.compile("%(\\d+\\$)?[\\d.]*[df]");
    private static final Map<String, Map<String, String>> LANGUAGE_CACHE = new ConcurrentHashMap<>();
    private static final Set<String> LOGGED_ASSET_ROOTS = ConcurrentHashMap.newKeySet();
    private static final Set<String> LOGGED_MOD_ROOTS = ConcurrentHashMap.newKeySet();

    private MinecraftLanguageHelper() {
    }

    public static String normalizeLanguage(String language) {
        if (language == null || language.isBlank()) {
            return "zh_cn";
        }
        String normalized = language.toLowerCase(Locale.ROOT).replace('-', '_');
        return normalized.startsWith("zh") ? "zh_cn" : "en_us";
    }

    public static String translateVanilla(String key, String language) {
        if (key == null || key.isBlank()) {
            return key;
        }
        String normalizedLanguage = normalizeLanguage(language);
        Map<String, String> languageMap = LANGUAGE_CACHE.computeIfAbsent(normalizedLanguage, MinecraftLanguageHelper::loadLanguageMap);
        return languageMap.getOrDefault(key, key);
    }

    public static boolean hasVanillaTranslation(String key, String language) {
        return !Objects.equals(translateVanilla(key, language), key);
    }

    private static Map<String, String> loadLanguageMap(String language) {
        LinkedHashMap<String, String> loaded = new LinkedHashMap<>();

        loadFromFabricMods(language, loaded);
        loadFromClasspath(language, loaded);
        loadFromAssetDirectories(language, loaded);

        if (!"en_us".equals(language)) {
            Map<String, String> enUs = LANGUAGE_CACHE.computeIfAbsent("en_us", MinecraftLanguageHelper::loadLanguageMap);
            enUs.forEach(loaded::putIfAbsent);
        }

        LOGGER.info("[语言加载] 语言 {} 加载完成，键值数量: {}", language, loaded.size());
        return Map.copyOf(loaded);
    }

    private static void loadFromFabricMods(String language, Map<String, String> target) {
        List<ModContainer> mods = new ArrayList<>(FabricLoader.getInstance().getAllMods());
        mods.sort(Comparator.comparing(mod -> mod.getMetadata().getId()));

        int loadedFileCount = 0;
        for (ModContainer mod : mods) {
            loadedFileCount += loadLanguageFromModRoots(mod, language, target);
        }

        LOGGER.info("[语言加载] 已扫描 Fabric 模组资源，语言: {}，文件数: {}，当前键值总数: {}",
                language, loadedFileCount, target.size());
    }

    private static int loadLanguageFromModRoots(ModContainer mod, String language, Map<String, String> target) {
        int loadedFileCount = 0;
        String modId = mod.getMetadata().getId();

        for (Path rootPath : mod.getRootPaths()) {
            logModRootOnce(modId, rootPath);
            Path assetsPath = rootPath.resolve("assets");
            if (!Files.isDirectory(assetsPath)) {
                continue;
            }

            try (Stream<Path> namespaces = Files.list(assetsPath)) {
                for (Path namespaceDir : namespaces.filter(Files::isDirectory).toList()) {
                    Path langFile = namespaceDir.resolve("lang").resolve(language + ".json");
                    if (!Files.isRegularFile(langFile)) {
                        continue;
                    }

                    if (loadFromJsonFile(langFile, target, false)) {
                        loadedFileCount++;
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("[语言加载] 扫描模组 {} 语言目录失败: {}，原因: {}", modId, assetsPath, e.toString());
            }
        }

        return loadedFileCount;
    }

    private static void logModRootOnce(String modId, Path rootPath) {
        String normalizedPath;
        try {
            normalizedPath = rootPath.toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            normalizedPath = rootPath.toString();
        }

        String key = modId + "|" + normalizedPath;
        if (LOGGED_MOD_ROOTS.add(key)) {
            LOGGER.debug("[语言加载] 模组资源根目录: {} -> {}", modId, normalizedPath);
        }
    }

    private static void loadFromClasspath(String language, Map<String, String> target) {
        String path = "/assets/minecraft/lang/" + language + ".json";
        try (InputStream input = MinecraftLanguageHelper.class.getResourceAsStream(path)) {
            if (input == null) {
                return;
            }
            int loadedCount = loadFromStream(input, target);
            LOGGER.info("[语言加载] 语言源 {} 已加载 {} 条翻译", path, loadedCount);
        } catch (Exception e) {
            LOGGER.warn("[语言加载] 从类路径加载语言失败: {}，原因: {}", path, e.toString());
        }
    }

    private static void loadFromAssetDirectories(String language, Map<String, String> target) {
        for (Path assetsRoot : resolveAssetRoots()) {
            logAssetRootOnce(assetsRoot);

            boolean loadedFromLegacy = loadFromLegacyPath(assetsRoot, language, target);
            boolean loadedFromIndex = loadFromIndexPath(assetsRoot, language, target);
            if (loadedFromLegacy || loadedFromIndex) {
                return;
            }
        }
    }

    private static void logAssetRootOnce(Path assetsRoot) {
        String rootText = assetsRoot.toAbsolutePath().normalize().toString();
        if (LOGGED_ASSET_ROOTS.add(rootText)) {
            LOGGER.info("[语言加载] 发现资源目录: {}", rootText);
        }
    }

    private static boolean loadFromLegacyPath(Path assetsRoot, String language, Map<String, String> target) {
        Path legacyFile = assetsRoot.resolve("virtual").resolve("legacy").resolve("minecraft").resolve("lang").resolve(language + ".json");
        if (!Files.isRegularFile(legacyFile)) {
            return false;
        }
        return loadFromJsonFile(legacyFile, target, true);
    }

    private static boolean loadFromIndexPath(Path assetsRoot, String language, Map<String, String> target) {
        Path indexesDir = assetsRoot.resolve("indexes");
        if (!Files.isDirectory(indexesDir)) {
            return false;
        }

        List<Path> indexFiles = listIndexFiles(indexesDir);
        for (Path indexFile : indexFiles) {
            String hash = findLanguageHash(indexFile, language);
            if (hash == null || hash.length() < 2) {
                continue;
            }

            Path objectFile = assetsRoot.resolve("objects")
                    .resolve(hash.substring(0, 2))
                    .resolve(hash);
            if (Files.isRegularFile(objectFile) && loadFromJsonFile(objectFile, target, true)) {
                LOGGER.info("[语言加载] 已从资源索引加载语言 {}，索引文件: {}", language, indexFile.getFileName());
                return true;
            }
        }

        return false;
    }

    private static List<Path> listIndexFiles(Path indexesDir) {
        try (Stream<Path> stream = Files.list(indexesDir)) {
            return stream.filter(path -> Files.isRegularFile(path) && path.getFileName().toString().endsWith(".json"))
                    .sorted(Comparator.comparing(MinecraftLanguageHelper::getLastModifiedTime).reversed())
                    .limit(50)
                    .toList();
        } catch (Exception e) {
            LOGGER.warn("[语言加载] 读取资源索引目录失败: {}，原因: {}", indexesDir, e.toString());
            return List.of();
        }
    }

    private static FileTime getLastModifiedTime(Path path) {
        try {
            return Files.getLastModifiedTime(path);
        } catch (IOException e) {
            return FileTime.fromMillis(0);
        }
    }

    private static String findLanguageHash(Path indexFile, String language) {
        String objectKey = "minecraft/lang/" + language + ".json";
        try (InputStream input = Files.newInputStream(indexFile)) {
            JsonObject root = GSON.fromJson(new InputStreamReader(input, StandardCharsets.UTF_8), JsonObject.class);
            if (root == null) {
                return null;
            }
            JsonObject objects = root.getAsJsonObject("objects");
            if (objects == null) {
                return null;
            }
            JsonObject languageObject = objects.getAsJsonObject(objectKey);
            if (languageObject == null) {
                return null;
            }
            JsonElement hashElement = languageObject.get("hash");
            if (hashElement == null || !hashElement.isJsonPrimitive()) {
                return null;
            }
            return hashElement.getAsString();
        } catch (Exception e) {
            LOGGER.warn("[语言加载] 读取资源索引文件失败: {}，原因: {}", indexFile.getFileName(), e.toString());
            return null;
        }
    }

    private static boolean loadFromJsonFile(Path filePath, Map<String, String> target, boolean verboseLog) {
        try (InputStream input = Files.newInputStream(filePath)) {
            int loadedCount = loadFromStream(input, target);
            if (verboseLog) {
                LOGGER.info("[语言加载] 语言源 {} 已加载 {} 条翻译", filePath, loadedCount);
            }
            return true;
        } catch (Exception e) {
            LOGGER.warn("[语言加载] 读取语言文件失败: {}，原因: {}", filePath, e.toString());
            return false;
        }
    }

    private static int loadFromStream(InputStream stream, Map<String, String> target) {
        JsonObject entries = GSON.fromJson(new InputStreamReader(stream, StandardCharsets.UTF_8), JsonObject.class);
        if (entries == null) {
            return 0;
        }

        int loadedCount = 0;
        for (Map.Entry<String, JsonElement> entry : entries.entrySet()) {
            JsonElement valueElement = entry.getValue();
            if (valueElement == null || !valueElement.isJsonPrimitive()) {
                continue;
            }
            String value = valueElement.getAsString();
            value = UNSUPPORTED_FORMAT_PATTERN.matcher(value).replaceAll("%$1s");
            target.put(entry.getKey(), value);
            loadedCount++;
        }

        return loadedCount;
    }

    private static List<Path> resolveAssetRoots() {
        LinkedHashSet<Path> roots = new LinkedHashSet<>();

        addAssetRoot(roots, System.getProperty("tuanzi.minecraft.assets.dir"));
        addAssetRoot(roots, System.getProperty("minecraft.assets"));

        String fabricGameDir = System.getProperty("fabric.gameDir");
        if (fabricGameDir != null && !fabricGameDir.isBlank()) {
            addAssetRoot(roots, Paths.get(fabricGameDir, "assets").toString());
        }

        String userDir = System.getProperty("user.dir");
        if (userDir != null && !userDir.isBlank()) {
            addAssetRoot(roots, Paths.get(userDir, "run", "assets").toString());
            addAssetRoot(roots, Paths.get(userDir, "assets").toString());
        }

        String appData = System.getenv("APPDATA");
        if (appData != null && !appData.isBlank()) {
            addAssetRoot(roots, Paths.get(appData, ".minecraft", "assets").toString());
        }

        String userHome = System.getProperty("user.home");
        if (userHome != null && !userHome.isBlank()) {
            addAssetRoot(roots, Paths.get(userHome, ".minecraft", "assets").toString());
            addAssetRoot(roots, Paths.get(userHome, ".gradle", "caches", "fabric-loom", "assets").toString());
            addFabricLoomVersionedAssetRoots(roots, Paths.get(userHome, ".gradle", "caches", "fabric-loom"));
        }

        String gradleUserHome = System.getProperty("gradle.user.home");
        if (gradleUserHome == null || gradleUserHome.isBlank()) {
            gradleUserHome = System.getenv("GRADLE_USER_HOME");
        }
        if (gradleUserHome != null && !gradleUserHome.isBlank()) {
            Path gradlePath = Paths.get(gradleUserHome);
            addAssetRoot(roots, gradlePath.resolve("caches").resolve("fabric-loom").resolve("assets").toString());
            addFabricLoomVersionedAssetRoots(roots, gradlePath.resolve("caches").resolve("fabric-loom"));
        }

        return new ArrayList<>(roots);
    }

    private static void addFabricLoomVersionedAssetRoots(Set<Path> roots, Path fabricLoomRoot) {
        if (!Files.isDirectory(fabricLoomRoot)) {
            return;
        }

        try (Stream<Path> stream = Files.list(fabricLoomRoot)) {
            stream.filter(Files::isDirectory)
                    .forEach(path -> addAssetRoot(roots, path.resolve("assets").toString()));
        } catch (Exception e) {
            LOGGER.warn("[语言加载] 扫描 Fabric-Loom 资源目录失败: {}，原因: {}", fabricLoomRoot, e.toString());
        }
    }

    private static void addAssetRoot(Set<Path> roots, String rawPath) {
        if (rawPath == null || rawPath.isBlank()) {
            return;
        }

        try {
            Path path = Paths.get(rawPath).toAbsolutePath().normalize();
            if (Files.isDirectory(path)) {
                roots.add(path);
            }
        } catch (Exception ignored) {
        }
    }
}
