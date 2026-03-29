package com.example.statistics.data;

public class ServerStatistics {
    private long totalUptimeSeconds;
    private long lastStartupTime;

    public ServerStatistics() {
        this.totalUptimeSeconds = 0;
        this.lastStartupTime = System.currentTimeMillis() / 1000;
    }

    public long getTotalUptimeSeconds() {
        return totalUptimeSeconds + getCurrentSessionUptimeSeconds();
    }

    public void setTotalUptimeSeconds(long totalUptimeSeconds) {
        this.totalUptimeSeconds = totalUptimeSeconds;
    }

    public long getCurrentSessionUptimeSeconds() {
        return (System.currentTimeMillis() / 1000) - lastStartupTime;
    }

    public void updateLastStartupTime() {
        this.lastStartupTime = System.currentTimeMillis() / 1000;
    }

    public void addUptime(long seconds) {
        this.totalUptimeSeconds += seconds;
    }

    public long getTotalUptimeMinutes() {
        return getTotalUptimeSeconds() / 60;
    }

    public long getTotalUptimeHours() {
        return getTotalUptimeSeconds() / 3600;
    }

    public long getTotalUptimeDays() {
        return getTotalUptimeSeconds() / 86400;
    }
}
