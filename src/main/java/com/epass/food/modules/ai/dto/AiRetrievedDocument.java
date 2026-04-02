package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiRetrievedDocument {

    private String id;

    private String title;

    private Double score;

    private String snippet;
}
