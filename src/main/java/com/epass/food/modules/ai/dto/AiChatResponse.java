package com.epass.food.modules.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AiChatResponse {

    private String content; // AI 回复内容

    private String scene; // 本次问题属于哪个场景，比如 order 或 general

    private Boolean grounded; // 这次回答是否明确基于项目事实
}