package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiModelUsage {

    private String responseId;

    private String model;

    private Integer promptTokens;

    private Integer completionTokens;

    private Integer totalTokens;
}
