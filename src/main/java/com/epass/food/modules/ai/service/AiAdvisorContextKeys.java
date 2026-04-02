package com.epass.food.modules.ai.service;

public final class AiAdvisorContextKeys {

    public static final String STRUCTURED_FIELDS = "structuredFields";
    public static final String TOOL_STATUS_OPTIONS = "toolStatusOptions";
    public static final String MEMORY_SUMMARY = "memorySummary";
    public static final String MEMORY_RECENT_TURNS = "memoryRecentTurns";
    public static final String RAG_FILTER_EXPRESSION = "ragFilterExpression";
    public static final String RAG_TOP_K = "ragTopK";
    public static final String RAG_SIMILARITY_THRESHOLD = "ragSimilarityThreshold";
    public static final String RAG_KNOWLEDGE_BASE = "ragKnowledgeBase";

    private AiAdvisorContextKeys() {
    }
}
