package me.tuanzi.shop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class DevFlowLogger {
    private static final Logger LOGGER = LoggerFactory.getLogger("ShopModule/DevFlow");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");
    private static final SimpleDateFormat FILE_NAME_FORMAT = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss");
    
    // 动态生成的日志文件路径
    private static final java.nio.file.Path LOG_FILE = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
            .resolve("logs/tuanzi_shop_debug_" + FILE_NAME_FORMAT.format(new Date()) + ".log");

    private static final Map<String, Long> FLOW_START_TIMES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FLOW_STEP_COUNTERS = new ConcurrentHashMap<>();

    private static final boolean IS_DEV = net.fabricmc.loader.api.FabricLoader.getInstance().isDevelopmentEnvironment();
    private static boolean enabled = true;

    private static void log(String msg) {
        if (IS_DEV) {
            LOGGER.info(msg);
        } else {
            // 生产环境下，直接写入独立文件，不通过日志等级过滤
            String time = DATE_FORMAT.format(new Date());
            writeToFile("[" + time + "] " + msg);
        }
    }

    private static void writeToFile(String msg) {
        try {
            // 确保 logs 目录存在
            java.nio.file.Files.createDirectories(LOG_FILE.getParent());
            java.nio.file.Files.writeString(LOG_FILE, msg + "\n", 
                    java.nio.file.StandardOpenOption.CREATE, 
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (java.io.IOException ignored) {
            // 写入失败时不报错，避免干扰主进程
        }
    }

    private static void logWarn(String msg) {
        if (IS_DEV) {
            LOGGER.warn(msg);
        } else {
            log("[WARN] " + msg);
        }
    }

    private static void logError(String msg) {
        if (IS_DEV) {
            LOGGER.error(msg);
        } else {
            log("[ERROR] " + msg);
        }
    }

    // --- 统一日志入口 ---

    /**
     * 关键信息：无论在什么环境下都会输出到控制台和日志文件
     * 用于：模组启动、关键错误、管理员操作审计
     */
    public static void critical(String prefix, String msg) {
        String formatted = "[" + prefix + "] " + msg;
        LOGGER.info(formatted);
        if (!IS_DEV) {
            writeToFile("[" + DATE_FORMAT.format(new Date()) + "] [CRITICAL] " + formatted);
        }
    }

    /**
     * 普通信息：开发环境输出到控制台，生产环境仅输出到调试日志
     * 用于：数据保存成功、配置加载、流程通知
     */
    public static void info(String prefix, String msg) {
        log("[" + prefix + "] " + msg);
    }

    /**
     * 警告信息：开发环境输出到控制台，生产环境仅输出到调试日志
     */
    public static void warn(String prefix, String msg) {
        logWarn("[" + prefix + "] " + msg);
    }

    /**
     * 错误信息：开发环境输出到控制台，生产环境仅输出到调试日志
     */
    public static void error(String prefix, String msg) {
        logError("[" + prefix + "] " + msg);
    }

    public static void error(String prefix, String msg, Throwable e) {
        String formatted = "[" + prefix + "] " + msg + (e != null ? " - " + e.toString() : "");
        if (IS_DEV) {
            LOGGER.error(formatted);
            if (e != null) e.printStackTrace();
        } else {
            log("[ERROR] " + formatted);
            if (e != null) {
                writeToFile("Stacktrace: " + java.util.Arrays.toString(e.getStackTrace()));
            }
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
    }

    // --- 业务流程追踪入口 ---

    public static void startFlow(String flowName) {
        if (!enabled) return;
        String timestamp = DATE_FORMAT.format(new Date());
        FLOW_START_TIMES.put(flowName, System.currentTimeMillis());
        FLOW_STEP_COUNTERS.put(flowName, 1);
        log("│ 🚀 [" + timestamp + "] 流程开始: " + flowName);
    }

    public static void step(String flowName, String stepName, Object... details) {
        if (!enabled) return;
        long startTime = FLOW_START_TIMES.getOrDefault(flowName, System.currentTimeMillis());
        int stepNum = FLOW_STEP_COUNTERS.getOrDefault(flowName, 1);
        double elapsedS = (System.currentTimeMillis() - startTime) / 1000.0;

        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("│ ⏭️  [%.3fs] [%s] 步骤 %d: %s",
                elapsedS, flowName, stepNum, stepName));

        if (details.length > 0) {
            logMsg.append("\n│    参数: ");
            for (int i = 0; i < details.length; i++) {
                logMsg.append(details[i]);
                if (i < details.length - 1) logMsg.append(", ");
            }
        }

        log(logMsg.toString());
        FLOW_STEP_COUNTERS.put(flowName, stepNum + 1);
    }

    public static void status(String flowName, String statusInfo) {
        if (!enabled) return;
        log("│ 📝 [" + flowName + "] 状态: " + statusInfo);
    }

    public static void flowWarning(String flowName, String warnInfo) {
        if (!enabled) return;
        logWarn("│ ⚠️  [" + flowName + "] 警告: " + warnInfo);
    }

    public static void flowError(String flowName, String errorInfo) {
        if (!enabled) return;
        logError("│ ❌ [" + flowName + "] 错误: " + errorInfo);
    }

    public static void param(String flowName, String paramName, Object value) {
        if (!enabled) return;
        log("│    🔹 [" + flowName + "] " + paramName + " = " + value);
    }

    public static void endFlow(String flowName, boolean success, String detail) {
        if (!enabled) return;
        Long startTimeObj = FLOW_START_TIMES.remove(flowName);
        if (startTimeObj == null) return;

        long startTime = startTimeObj;
        FLOW_STEP_COUNTERS.remove(flowName);
        long elapsedMs = System.currentTimeMillis() - startTime;
        String timestamp = DATE_FORMAT.format(new Date());

        String result = success ? "✅ 成功 - " + detail : "❌ 失败 - " + detail;

        log("│ 🏁 [" + timestamp + "] 流程结束: " + flowName);
        log("│ ⏱️  总耗时: " + elapsedMs + " ms (" + String.format("%.3f", elapsedMs / 1000.0) + "s)");
        log("│ 📊 结果: " + result);
        log("╰" + "─".repeat(78));
    }
}
