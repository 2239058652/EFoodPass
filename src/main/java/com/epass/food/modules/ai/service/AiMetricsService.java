package com.epass.food.modules.ai.service;

import com.epass.food.modules.ai.dto.AiRetrievalMeta;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class AiMetricsService {

    private final MeterRegistry meterRegistry;

    public AiMetricsService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void recordChat(String scene,
                           String answerType,
                           String toolStatus,
                           AiRetrievalMeta retrievalMeta,
                           long durationMillis) {
        String retrievalApplied = String.valueOf(retrievalMeta != null && retrievalMeta.isRetrievalApplied());

        Counter.builder("ai.chat.requests")
                .tag("scene", normalize(scene))
                .tag("answer_type", normalize(answerType))
                .tag("tool_status", normalize(toolStatus))
                .tag("retrieval", retrievalApplied)
                .register(meterRegistry)
                .increment();

        Timer.builder("ai.chat.latency")
                .tag("scene", normalize(scene))
                .tag("answer_type", normalize(answerType))
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMillis));

        if (retrievalMeta != null && retrievalMeta.isRetrievalApplied()) {
            DistributionSummary.builder("ai.rag.retrieved.documents")
                    .tag("scene", normalize(scene))
                    .tag("knowledge_base", normalize(retrievalMeta.getKnowledgeBase()))
                    .register(meterRegistry)
                    .record(retrievalMeta.getRetrievedCount());
        }
    }

    public void recordStream(String scene, long durationMillis) {
        Counter.builder("ai.chat.stream.requests")
                .tag("scene", normalize(scene))
                .register(meterRegistry)
                .increment();

        Timer.builder("ai.chat.stream.latency")
                .tag("scene", normalize(scene))
                .register(meterRegistry)
                .record(Duration.ofMillis(durationMillis));
    }

    public void recordKnowledgeRebuild(String knowledgeBase, int documentCount) {
        Counter.builder("ai.rag.index.rebuild.count")
                .tag("knowledge_base", normalize(knowledgeBase))
                .register(meterRegistry)
                .increment();

        DistributionSummary.builder("ai.rag.index.document.count")
                .tag("knowledge_base", normalize(knowledgeBase))
                .register(meterRegistry)
                .record(documentCount);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? "none" : value;
    }
}
