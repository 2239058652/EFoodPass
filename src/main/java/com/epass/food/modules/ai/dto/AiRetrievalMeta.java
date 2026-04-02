package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class AiRetrievalMeta {

    private boolean retrievalApplied;

    private int retrievedCount;

    private List<AiRetrievedDocument> documents;
}
