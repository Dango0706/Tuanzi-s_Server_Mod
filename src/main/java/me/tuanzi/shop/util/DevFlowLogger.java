package me.tuanzi.shop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

public class DevFlowLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopModule/DevFlow");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");
    private static final boolean DEV_MODE;

    private static final Map<String, Long> flowStartTimes = new ConcurrentHashMap<>();
    private static final Map<String, AtomicLong> stepCounters = new ConcurrentHashMap<>();

    static {
        DEV_MODE = Boolean.parseBoolean(System.getProperty("shop.dev.mode", "false"));
        if (DEV_MODE) {
            LOGGER.info("╔════════════════════════════════════════════════════════════╗");
            LOGGER.info("║          🛠️  商店模块开发环境日志已启用                    ║");
            LOGGER.info("║   所有核心业务流程将输出详细的调试信息                      ║");
            LOGGER.info("╚════════════════════════════════════════════════════════════╝");
        }
    }

    public static boolean isDevMode() {
        return DEV_MODE;
    }

    public static void startFlow(String flowName) {
        if (!DEV_MODE) return;

        String flowKey = generateFlowKey(flowName);
        long startTime = System.currentTimeMillis();
        flowStartTimes.put(flowKey, startTime);
        stepCounters.put(flowKey, new AtomicLong(0));

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        LOGGER.info("");
        LOGGER.info("┌──────────────────────────────────────────────────────────────┐");
        LOGGER.info("│ 🚀 [{}] 流程开始: {}", timestamp, flowName);
        LOGGER.info("├──────────────────────────────────────────────────────────────┘");
    }

    public static void step(String flowName, String stepName, Object... params) {
        if (!DEV_MODE) return;

        String flowKey = generateFlowKey(flowName);
        AtomicLong counter = stepCounters.get(flowKey);
        long stepNum = counter != null ? counter.incrementAndGet() : 1;

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("│ ⏭️  [%.3fs] [%s] 步骤 %d: %s",
                getElapsedMs(flowKey) / 1000.0,
                timestamp,
                stepNum,
                stepName));

        if (params.length > 0) {
            logMsg.append("\n│    参数: ");
            for (int i = 0; i < params.length; i++) {
                if (i > 0) logMsg.append(", ");
                logMsg.append(params[i]);
            }
        }

        LOGGER.info(logMsg.toString());
    }

    public static void param(String flowName, String paramName, Object value) {
        if (!DEV_MODE) return;

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        LOGGER.info("│ 📋 [{}] {} = {}", timestamp, paramName, value);
    }

    public static void status(String flowName, String statusMsg) {
        if (!DEV_MODE) return;

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        LOGGER.info("│ ✅ [{}] {}", timestamp, statusMsg);
    }

    public static void warning(String flowName, String warningMsg) {
        if (!DEV_MODE) return;

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        LOGGER.warn("│ ⚠️  [{}] {}", timestamp, warningMsg);
    }

    public static void error(String flowName, String errorMsg) {
        if (!DEV_MODE) return;

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        LOGGER.error("│ ❌ [{}] {}", timestamp, errorMsg);
    }

    public static void endFlow(String flowName, String result) {
        if (!DEV_MODE) return;

        String flowKey = generateFlowKey(flowName);
        long elapsedMs = getElapsedMs(flowKey);

        String timestamp = LocalDateTime.now().format(TIME_FORMATTER);
        LOGGER.info("├──────────────────────────────────────────────────────────────┤");
        LOGGER.info("│ 🏁 [{}] 流程结束: {}", timestamp, flowName);
        LOGGER.info("│ ⏱️  总耗时: {} ms ({:.3f}s)", elapsedMs, elapsedMs / 1000.0);
        LOGGER.info("│ 📊 结果: {}", result);
        LOGGER.info("└──────────────────────────────────────────────────────────────┘");
        LOGGER.info("");

        cleanupFlow(flowKey);
    }

    public static void endFlow(String flowName, boolean success, String detail) {
        String result = success ? "✅ 成功 - " + detail : "❌ 失败 - " + detail;
        endFlow(flowName, result);
    }

    private static long getElapsedMs(String flowKey) {
        Long startTime = flowStartTimes.get(flowKey);
        return startTime != null ? System.currentTimeMillis() - startTime : 0;
    }

    private static String generateFlowKey(String flowName) {
        return Thread.currentThread().getId() + ":" + flowName;
    }

    private static void cleanupFlow(String flowKey) {
        flowStartTimes.remove(flowKey);
        stepCounters.remove(flowKey);
    }
}
