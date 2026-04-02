package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AiEntityReference {

    private String entityType;

    private Long entityId;

    private AiQueryIntent intent;
}