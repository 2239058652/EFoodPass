package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiRetrievalMeta {

    private boolean retrievalApplied;

    private String knowledgeBase;

    private String filterExpression;

    private Integer topK;

    private Double similarityThreshold;

    private int retrievedCount;

    private List<AiRetrievedDocument> documents;
}
