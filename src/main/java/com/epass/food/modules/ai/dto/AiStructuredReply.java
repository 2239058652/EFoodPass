package com.epass.food.modules.ai.dto;

import lombok.Data;

@Data
public class AiStructuredReply {

    private String content;

    private String scene;

    private Boolean grounded;
}