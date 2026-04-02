package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.SystemKnowledgeIndexStatusResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SystemKnowledgeIndexService {

    private static final String KNOWLEDGE_BASE = "system";

    private final VectorStore systemKnowledgeVectorStore;
    private final SystemKnowledgeDocumentFactory documentFactory;
    private final SystemKnowledgeRagProperties ragProperties;
    private final AiMetricsService aiMetricsService;

    private volatile int documentCount;
    private volatile long lastRebuildAt;

    public SystemKnowledgeIndexService(VectorStore systemKnowledgeVectorStore,
                                       SystemKnowledgeDocumentFactory documentFactory,
                                       SystemKnowledgeRagProperties ragProperties,
                                       AiMetricsService aiMetricsService) {
        this.systemKnowledgeVectorStore = systemKnowledgeVectorStore;
        this.documentFactory = documentFactory;
        this.ragProperties = ragProperties;
        this.aiMetricsService = aiMetricsService;
    }

    @PostConstruct
    public void initialize() {
        rebuildIndex();
    }

    public synchronized SystemKnowledgeIndexStatusResponse rebuildIndex() {
        List<String> documentIds = documentFactory.getDocumentIds();
        if (!documentIds.isEmpty()) {
            systemKnowledgeVectorStore.delete(documentIds);
        }

        var documents = documentFactory.createDocuments();
        systemKnowledgeVectorStore.add(documents);

        this.documentCount = documents.size();
        this.lastRebuildAt = System.currentTimeMillis();
        aiMetricsService.recordKnowledgeRebuild(KNOWLEDGE_BASE, this.documentCount);
        return getStatus();
    }

    public SystemKnowledgeIndexStatusResponse getStatus() {
        return new SystemKnowledgeIndexStatusResponse(
                KNOWLEDGE_BASE,
                documentCount,
                lastRebuildAt == 0 ? null : lastRebuildAt,
                ragProperties.getTopK(),
                ragProperties.getSimilarityThreshold()
        );
    }
}
