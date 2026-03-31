package com.epass.food.modules.ai.service.impl;

import com.epass.food.common.exception.BusinessException;
import com.epass.food.modules.ai.dto.AiChatResponse;
import com.epass.food.modules.ai.dto.AiStructuredReply;
import com.epass.food.modules.ai.service.AiChatService;
import com.epass.food.modules.ai.service.BusinessContextProvider;
import com.epass.food.modules.ai.service.OrderFactProvider;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final BusinessContextProvider businessContextProvider;
    private final OrderFactProvider orderFactProvider;
    private final ObjectMapper objectMapper;

    public AiChatServiceImpl(ChatClient.Builder chatClientBuilder,
                             BusinessContextProvider businessContextProvider,
                             OrderFactProvider orderFactProvider,
                             ObjectMapper objectMapper) {
        this.chatClient = chatClientBuilder.build();
        this.businessContextProvider = businessContextProvider;
        this.orderFactProvider = orderFactProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public AiChatResponse chat(String message) {
        boolean orderQuestion = isOrderQuestion(message);
        String systemPrompt = buildPromptByMessage(message, orderQuestion);

        String rawContent = chatClient.prompt()
                .system(systemPrompt)
                .user(message)
                .call()
                .content();

        AiStructuredReply reply = parseStructuredReply(rawContent);

        return new AiChatResponse(
                reply.getContent(),
                reply.getScene(),
                reply.getGrounded()
        );
    }

    private AiStructuredReply parseStructuredReply(String rawContent) {
        try {
            return objectMapper.readValue(rawContent, AiStructuredReply.class);
        } catch (JsonProcessingException e) {
            throw new BusinessException(500, "AI 返回结果不是合法 JSON: " + rawContent);
        }
    }

    private String buildPromptByMessage(String message, boolean orderQuestion) {
        if (orderQuestion) {
            return """
                    %s
                    
                    下面是订单领域的真实业务事实：
                    %s
                    
                    你现在是 EFoodPass 的订单助手。
                    请严格基于这些真实事实回答订单问题。
                    如果事实里没有，不要编造。
                    
                    你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                    JSON 格式如下：
                    {
                      "content": "给用户的中文回答",
                      "scene": "order",
                      "grounded": true
                    }
                    """.formatted(
                    businessContextProvider.buildCommonFacts(),
                    orderFactProvider.buildOrderFacts()
            );
        }

        return """
                %s
                
                你必须只返回一个 JSON 对象，不要返回 Markdown，不要返回代码块，不要添加额外说明。
                JSON 格式如下：
                {
                  "content": "给用户的中文回答",
                  "scene": "general",
                  "grounded": true
                }
                """.formatted(businessContextProvider.buildGeneralAssistantPrompt());
    }

    private boolean isOrderQuestion(String message) {
        if (message == null || message.isBlank()) {
            return false;
        }

        return message.contains("订单")
                || message.contains("下单")
                || message.contains("取消")
                || message.contains("状态");
    }
}