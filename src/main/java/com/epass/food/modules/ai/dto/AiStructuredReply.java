package com.epass.food.modules.ai.dto;

import lombok.Data;

@Data
public class AiStructuredReply {

    private String content;

    /**
     * 仅在工具调用场景下返回，可选值：
     * success / not_found / restricted
     */
    private String toolStatus;
}
