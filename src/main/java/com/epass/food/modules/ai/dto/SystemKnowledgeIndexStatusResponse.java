package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SystemKnowledgeIndexStatusResponse {

    private String knowledgeBase;

    private Integer documentCount;

    private Long lastRebuildAt;

    private Integer topK;

    private Double similarityThreshold;
}
