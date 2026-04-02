package com.epass.food.modules.ai.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai.memory")
public class AiConversationMemoryProperties {

    private int maxTurns = 6;

    private int archiveMaxTurns = 100;

    private int recentTurnsForPrompt = 3;

    private int summaryMaxChars = 240;

    private long ttlHours = 12;

    public int getMaxTurns() {
        return maxTurns;
    }

    public void setMaxTurns(int maxTurns) {
        this.maxTurns = maxTurns;
    }

    public int getRecentTurnsForPrompt() {
        return recentTurnsForPrompt;
    }

    public void setRecentTurnsForPrompt(int recentTurnsForPrompt) {
        this.recentTurnsForPrompt = recentTurnsForPrompt;
    }

    public int getArchiveMaxTurns() {
        return archiveMaxTurns;
    }

    public void setArchiveMaxTurns(int archiveMaxTurns) {
        this.archiveMaxTurns = archiveMaxTurns;
    }

    public int getSummaryMaxChars() {
        return summaryMaxChars;
    }

    public void setSummaryMaxChars(int summaryMaxChars) {
        this.summaryMaxChars = summaryMaxChars;
    }

    public long getTtlHours() {
        return ttlHours;
    }

    public void setTtlHours(long ttlHours) {
        this.ttlHours = ttlHours;
    }
}
