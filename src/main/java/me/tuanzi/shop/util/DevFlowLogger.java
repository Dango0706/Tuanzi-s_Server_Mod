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
    
    private static final Map<String, Long> FLOW_START_TIMES = new ConcurrentHashMap<>();
    private static final Map<String, Integer> FLOW_STEP_COUNTERS = new ConcurrentHashMap<>();
    
    private static boolean enabled = false;

    public static void setEnabled(boolean value) {
        enabled = value;
        if (enabled) {
            LOGGER.info("╭" + "─".repeat(78));
            LOGGER.info("║          🛠️  商店模块开发环境流程日志已启用                ║");
            LOGGER.info("║   所有核心业务流程将输出详细的中文调试信息                  ║");
            LOGGER.info("╰" + "─".repeat(78));
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void startFlow(String flowName) {
        if (!enabled) return;
        String timestamp = DATE_FORMAT.format(new Date());
        FLOW_START_TIMES.put(flowName, System.currentTimeMillis());
        FLOW_STEP_COUNTERS.put(flowName, 1);
        LOGGER.info("│ 🚀 [{}] 流程开始: {}", timestamp, flowName);
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

        LOGGER.info(logMsg.toString());
        FLOW_STEP_COUNTERS.put(flowName, stepNum + 1);
    }

    public static void status(String flowName, String statusInfo) {
        if (!enabled) return;
        LOGGER.info("│ 📝 [{}] 状态: {}", flowName, statusInfo);
    }

    public static void warning(String flowName, String warnInfo) {
        if (!enabled) return;
        LOGGER.warn("│ ⚠️  [{}] 警告: {}", flowName, warnInfo);
    }

    public static void error(String flowName, String errorInfo) {
        if (!enabled) return;
        LOGGER.error("│ ❌ [{}] 错误: {}", flowName, errorInfo);
    }

    public static void param(String flowName, String paramName, Object value) {
        if (!enabled) return;
        LOGGER.info("│    🔹 [{}] {} = {}", flowName, paramName, value);
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

        LOGGER.info("│ 🏁 [{}] 流程结束: {}", timestamp, flowName);
        LOGGER.info("│ ⏱️  总耗时: {} ms ({:.3f}s)", elapsedMs, elapsedMs / 1000.0);
        LOGGER.info("│ 📊 结果: {}", result);
        LOGGER.info("╰" + "─".repeat(78));
    }
}
